

package de.tu_berlin.sese.cormorant.frameworkInstantiation.EquivalenceVerifier

object EpsilonCalculator {

  
  

  
  /*
   * The function extracts the matrix A from a partition described by a state equation of the form
   * d/dt y=Ay + i or y+{n+1}=Ay_n + i_n
   */
  def setUpMatrix(p: Partition): Matrix = {
    
    def collectStateCoefficients(p: Partition, i: Int, j: Int): Term = {
    //val terms = statesTransformed(p)
      val term1: Term = ExpressionExtractor.convert2AdditiveNormalForm(ExpressionExtractor.ID2Term(i,ExpressionExtractor.states(p)))
      val v2: SimulinkVertex = ExpressionExtractor.iD2Vertex(j,ExpressionExtractor.statefulVertices(p))
      val term2: AtomicTerm = v2.vertex2Expression(j, Nil).asInstanceOf[AtomicTerm]
      ExpressionExtractor.collectCoefficients(term1,term2)
    
    }
  
    val dim = ExpressionExtractor.states(p).size
    val matrix = Array.ofDim[Term](dim, dim)
    for(i<-0 to dim-1) {
      for(j<-0 to dim-1) {
        matrix(i)(j)=collectStateCoefficients(p,i+1,j+1)
       }
    }
    Matrix(matrix)
    
   }
  
  def setUpInputVector(p: Partition): Vector = {
    def collectCoefficients(p: Partition, i: Int): Term = {
    val term: Term = ExpressionExtractor.convert2AdditiveNormalForm(ExpressionExtractor.ID2Term(i,ExpressionExtractor.states(p)))
    ExpressionExtractor.collectStateLessTerms(term)
  }
    val dim = ExpressionExtractor.states(p).size
    val v = Array.ofDim[Term](dim)
    for(i<-0 to dim-1) {
      v(i)=collectCoefficients(p,i+1)
    }
    Vector(v)
  }
  
  def setUpDisturbanceVector(p: Partition, eps: Double): Vector = Vector(setUpInputVector(p).v.map(x=> x match {
    case a: InputVariable => Constant(eps)
    case a => a
  }))
  
  
  def setUpStateVector(p:Partition): Vector = {
    val dim = ExpressionExtractor.states(p).size
    val v = Array.ofDim[Term](dim)
    for(i<-0 to dim-1) {
      v(i)=ExpressionExtractor.iD2Vertex(i+1, ExpressionExtractor.statefulVertices(p)).vertex2Expression(i+1, Nil)
    }
    Vector(v)
  }
  
  def setUpInitialValues(p: Partition):Array[Double] = {
    val dim = ExpressionExtractor.states(p).size
    val v = Array.ofDim[Double](dim)
    for(i<-0 to dim-1) {
      v(i)=ExpressionExtractor.iD2Vertex(i+1, ExpressionExtractor.statefulVertices(p)) match {
        case x: IntegratorVertex => x.initialValue
        case x: DelayVertex => x.initialValue
        case _ => 0
      }
      
    }
    v
  }
  
//   def calculateLipschitz(p: Partition) = p.solved match {
//    case true => setUpMatrix(p).norm
//    case _ => 0
//  }
   
   def calculateLipschitz(p: Partition) = setUpMatrix(p).norm
  
   
   def obtainAnalyticalSolution(p: Partition): Term = p.solved match {
     case true => p.isStateLess match {
       case true => ExpressionExtractor.observations(p).head._2
       case false => UndefinedTerm()
     }
     case false => UndefinedTerm()
   }
   
  
   def maxSecondDerivative(p: Partition, simulationEnd: Int) = p.solved match {
     case true  => p.isStateLess match {
       case true => MathematicaProcessor.runCommand("MaxValue[{Abs[D[D[" + obtainAnalyticalSolution(p).mathematicaString + ",t],t]],0<=t<=" + simulationEnd + "},t]").toDouble
       case _ => 0
     }
     case _ => 0
   }
   def initialValues(v: Array[Term], init: Array[Double]): String = {
       var s: String = "{"
       for(i<-0 to v.size-1) {
         s=s+v(i) + "[0]==" + init(i) + ","
  
       }
       s.dropRight(1)+"}"
    }
   
  def maxSecondDerivativeBetweenPartitions(p: Partition, disturbance: Double, simulationEnd: Int) = {
    def multidimensSecondDeriv(s: String, i: Int, maxi: Int, res: List[Double]): List[Double] =  i match {
      case a if a<=maxi => res.+:(MathematicaProcessor.runCommand("MaxValue[{Abs[D[D[" + s + "[[" + i + "]],t],t]],0<=t<=" + simulationEnd + "},t]").toDouble)
      case _ => res
    }
    def max(l: List[Double])=l.foldLeft(0.0)((m,x)=> m match {
      case a if a>=x => a
      case a => x
    })
    def multidimensSecondDerivList(s: String): List[Double] = multidimensSecondDeriv(s,1,ExpressionExtractor.states(p).size, Nil)
    
      val s = "("+setUpStateVector(p).enhancedMathematicaString + "/.DSolve[{" + setUpStateVector(p).derivedMathematicaString + "==" + setUpMatrix(p).mathematicaString + "." + setUpStateVector(p).enhancedMathematicaString + "+" + setUpDisturbanceVector(p,disturbance).mathematicaString + "," + initialValues(setUpStateVector(p).v, setUpInitialValues(p)) + "}," + setUpStateVector(p).enhancedMathematicaString + ",t])[[1]]"
    max(multidimensSecondDerivList(s))
  }
  
   def localEpsilon(p1: Partition, p2: Partition, simulationEnd: Int, sampleStepSize: Double) = p1.solved match {
     case true => p2.solved match {
       case true => p1.isStateLess match {
         
         case true => p2.isPurelyTimeContinuous match {
           case true => 0.5*simulationEnd*sampleStepSize*Math.exp(simulationEnd*calculateLipschitz(p1))*maxSecondDerivative(p1,simulationEnd)
           case false => 0
         }
         case false => p1.isPurelyTimeContinuous match {
           case true => 0.5*simulationEnd*sampleStepSize*Math.exp(simulationEnd*calculateLipschitz(p2))*maxSecondDerivative(p2,simulationEnd)
           case false => 0
         }
         
       }
      
       case false => 0
     }
     case false =>0
   }
   
   def localEpsilon(par1:Partitioning, par2:Partitioning, simulationEnd: Int, sampleStepSize: Double): Partition => Double = p=> par1.partitions.find(x=>x==p) match {
     case None => 0 
     case Some(a) if a.solved => localEpsilon(a,par2.partitions.filter(_.solved).head, simulationEnd, sampleStepSize)
     case Some(a) =>0
   }
   
   def epsilonCalculated(par: Partitioning, p: Partition): Partition => Boolean = p=> p.solved 
   
   def /(f: Partition => Boolean, p:Partition, replace: Boolean): Partition =>Boolean = x => x match {
     case x if x==p => replace
     case _ => f(p)
   }
   
    //false case (diskret) noch anpassen: RSolve oder so
   def conditionalError(p: Partition, disturbance: Double, simulationEnd: Int): Double = p.isPurelyTimeContinuous match{
     case true =>MathematicaProcessor.runCommand("N[MaxValue[{Norm[" + setUpStateVector(p).enhancedMathematicaString + "/.DSolve[{" + setUpStateVector(p).derivedMathematicaString + "==" + setUpMatrix(p).mathematicaString + "." + setUpStateVector(p).enhancedMathematicaString + "+" + setUpDisturbanceVector(p,disturbance).mathematicaString + "," + initialValues(setUpStateVector(p).v, setUpInitialValues(p)) + "}," + setUpStateVector(p).enhancedMathematicaString + ",t], Infinity],0<=t<=" + simulationEnd + "},t]]").toDouble
     case false => 0
   }
   
   
   def convergenceError(p: Partition, disturbance: Double, simulationEnd: Int, sampleStepSize: Double) = p.isPurelyTimeContinuous match {
     case true => 0.5*simulationEnd*sampleStepSize*Math.exp(simulationEnd*calculateLipschitz(p))*maxSecondDerivativeBetweenPartitions(p,disturbance,simulationEnd)
     case _ => 0
   }
   
   def propagatedEpsilon(p: Partition, disturbance: Double, simulationEnd: Int, sampleStepSize: Double) = disturbance match {
     case 0.0 => 0.0
     case _ => conditionalError(p, disturbance, simulationEnd) + convergenceError(p, disturbance, simulationEnd, sampleStepSize)
   }
   
   def max(l: List[Double]) = l.foldLeft(0.0)((s,x)=> x match {
     case a if a<s => s
     case a => a
   })
   
   def propagateEpsilon(par1: Partitioning, par2:Partitioning, p: Partition, simulationEnd: Int, sampleStepSize:Double): Double = par1.predecessors(p) match {
     case Nil => p.solved match {
       case true => p.isPurelyTimeContinuous match { 
         case true =>localEpsilon(par1,par2,simulationEnd,sampleStepSize)(p)
         case false => 0.0
       }
       case false => 0.0
     }
     case ls => propagatedEpsilon(p,max(ls.map(x=>propagateEpsilon(par1,par2,x,simulationEnd, sampleStepSize))), simulationEnd, sampleStepSize)
   }
   
   def globalEpsilon(par1: Partitioning, par2:Partitioning, simulationEnd: Int, sampleStepSize: Double): Double = max(par1.partitions.filter(p=>p.elements.find(x=>x.isInstanceOf[RegularOutputVertex]) match {
     case None => false
     case Some(a) => true}
   ).map(p=>propagateEpsilon(par1,par2,p,simulationEnd,sampleStepSize)).toList)
   
   def globalEpsilon(l1: List[Partitioning], l2: List[Partitioning], simulationEnd: Int, sampleStepSize: Double): Double = max(l1.map(x=>max(l2.map(y=>globalEpsilon(x,y,simulationEnd,sampleStepSize)))))
}