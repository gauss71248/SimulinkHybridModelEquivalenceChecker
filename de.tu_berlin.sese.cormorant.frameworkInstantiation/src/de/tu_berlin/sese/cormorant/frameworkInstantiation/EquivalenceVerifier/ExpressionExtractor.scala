

package de.tu_berlin.sese.cormorant.frameworkInstantiation.EquivalenceVerifier

import java.lang.reflect.Constructor


object ExpressionExtractor {
  
  
  
  def reAssignIDs(mode: SimulinkVertex => Boolean, p: Partition) =  p.elements.filter(mode).toList.sortBy(x=>x.id) zip Stream.from(1)
  
  def statefulVertices(p: Partition): List[(SimulinkVertex, Int)] = reAssignIDs(v => v.isInstanceOf[StatefulVertex],p)
  def integratorVertices(p: Partition): List[(SimulinkVertex, Int)] = statefulVertices(p).filter(x=>x._1.isInstanceOf[IntegratorVertex])
  def transferFunctionVertices(p: Partition): List[(SimulinkVertex, Int)] = statefulVertices(p).filter(x=>x._1.isInstanceOf[TransferFunctionVertex])
  def delayVertices(p: Partition): List[(SimulinkVertex, Int)] = statefulVertices(p).filter(x=>x._1.isInstanceOf[DelayVertex])
  def outputVertices(p: Partition): List[(SimulinkVertex, Int)] = reAssignIDs(v => v.isInstanceOf[OutputVertex],p)
//  def inputVertices(p: Partition): List[(SimulinkVertex, Int)] = p.elements.filter(v => v.isInstanceOf[InputVertex]).toList zip Stream.from(1)
   
  def vertex2ID(v: SimulinkVertex, l: List[(SimulinkVertex, Int)]): Int = l match {
    case Nil => 0
    case ((x, i) :: xs) if x == v => i
    case (x :: xs) => vertex2ID(v, xs)
  }
  
  def vertex2ID(mode: SimulinkVertex => Boolean, p: Partition, v: SimulinkVertex): Int = p.elements.filter(mode).find(x => x==v) match {
    case None => 0
    case Some(x) => vertex2ID(v,reAssignIDs(mode, p))
  }
  
  def iD2Vertex(a: Int, l:List[(SimulinkVertex, Int)]): SimulinkVertex = l match {
    case Nil => UndefinedSimulinkVertex(0,None)
    case ((x, i) :: xs) if i == a => x
    case (x :: xs) => iD2Vertex(a, xs)
  }
 
  def obtainTerm(p: Partition, b: SimulinkVertex): Term = {
    import de.tu_berlin.pes.memo.MeMoPlugin 
    
    b match {

      case x: ArithmeticVertex => {

        
        x.vertex2Expression(x.id, x.fixedInputConstants.map(c => new Constant(c)) ++ predecessorsSorted(p, x).map(y => obtainTerm(p,y)))
      }

      case x: InputVertex => {
       
        val id = vertex2ID(v => v.isInstanceOf[InputVertex],p,x)
       
        x match {
          case y: InstantiatedInputVertex => y.vertex2Expression(id, y.fixedInputConstants.map(c => new Constant(c)))
          case y => y.vertex2Expression(id, Nil)
        }

      }
      case x: StatefulVertex => {
        val id = vertex2ID(v => v.isInstanceOf[StatefulVertex], p,x)
        x.vertex2Expression(id, Nil)

      }
      case x => {
        
        new UndefinedTerm()
      }
    }
  }  
  


 
 def predecessor(p: Partition, b: SimulinkVertex, arity: Int): SimulinkVertex = p.partitionGraphRestriction.directpredecessor(b)
 def predecessorsSorted(p: Partition, v: SimulinkVertex): List[SimulinkVertex] = p.partitionGraphRestriction.directPredecessorsSorted(v)
 
  def observations(p: Partition): List[(Int, Term)]=outputVertices(p).map(x => (x._2,obtainTerm(p,predecessor(p,x._1,1))))
  def observations(p: Partitioning): List[(Partition, List[(Int, Term)])] = p.partitions.toList.map(x => (x,observations(x)))
  def states(p: Partition): List[(Int, Term)]=statefulVertices(p).map(x => (x._2,obtainTerm(p,predecessor(p,x._1,1))))
  def states(p: Partitioning): List[(Partition, List[(Int, Term)])] = p.partitions.toList.map(x => (x,states(x)))
 
  def integratorsTransformed(p: Partition):List[(Int, Term)]=integratorVertices(p).map(x => (x._2, Add(List(Mult(List(RationalPolynomial(Polynomial(List(1),LaplaceVariable()),Polynomial(List(1,0), LaplaceVariable())),obtainTerm(p,predecessor(p,x._1,1)))), Mult(List(RationalPolynomial(Polynomial(List(1),LaplaceVariable()),Polynomial(List(1,0), LaplaceVariable())),Constant(x._1.asInstanceOf[IntegratorVertex].initialValue)))))))
  def transferFunctionsTransformed(p: Partition):List[(Int, Term)]=transferFunctionVertices(p).map(x => (x._2, Mult(List(RationalPolynomial(Polynomial(x._1.asInstanceOf[TransferFunctionVertex].numerator, LaplaceVariable()), Polynomial(x._1.asInstanceOf[TransferFunctionVertex].denominator, LaplaceVariable())), obtainTerm(p,predecessor(p,x._1,1))))))
  def delaysTransformed(p: Partition):List[(Int, Term)]={
    def properArityList(del: Int, l: List[Double]): List[Double] = del match {
      case 0 => l 
      case n => properArityList(n-1,(l:+0.0))
    }
    def delayTerm(delay: Int) = RationalPolynomial(Polynomial(List(1),zVariable()), Polynomial(List(1.0) ++ properArityList(delay-1,Nil), zVariable()))
    delayVertices(p).map(x => (x._2, Add(List(Mult(List(delayTerm(x._1.asInstanceOf[DelayVertex].delay),obtainTerm(p,predecessor(p,x._1,1)))), Mult(List(delayTerm(x._1.asInstanceOf[DelayVertex].delay),Constant(x._1.asInstanceOf[DelayVertex].initialValue)))))))
  }
  
  def statesTransformed(p: Partition): List[(Int, Term)]= integratorsTransformed(p) ++ transferFunctionsTransformed(p) ++ delaysTransformed(p)
  def statesTransformed(p: Partitioning): List[(Partition, List[(Int, Term)])] = p.partitions.toList.map(x => (x,statesTransformed(x)))
  
  def isInAdditiveCanonicalForm(t: Term): Boolean =  {
    def noOuterAdd(t2: Term) = t2 match {
      case Add(_) => false
      case _ => true
    }
    
    def allSameForm(pred: Term => Boolean, t2:Term): Boolean = t2 match {
      case x: AtomicTerm => true
      case x: Constant => true
      case x: ArithmeticFunction if pred(x) => x.arguments.foldLeft(true)((b,y) => b && (allSameForm(pred,y)))
      case _ => false
    }
    t match {
      case x: AtomicTerm => true
      case x: Mult => x.arguments.foldLeft(true)((b,y) => b && (allSameForm(noOuterAdd,y)))
      case x: ArithmeticFunction => x.arguments.foldLeft(true)((b,y) => b && (isInAdditiveCanonicalForm(y)))
     // case _ => true
    }
    
  }
  
 
  def convert2AdditiveNormalForm(t: Term): Term = {
    def orderAddsInFront(l: List[Term]): List[Term] = l.sortWith((t1,t2) => t1.isInstanceOf[Add])
    def orderAddsInFrontInTerm(t: Term): Term = t match {
      case x: Mult => Mult(orderAddsInFront(x.arguments))
      case x: Add => Add(orderAddsInFront(x.arguments))
      case x => x
    }
    
    def exploitDistributivity(t: Term) = t match {
      case Mult(l) if l.head.isInstanceOf[Add] => {
        val lAdd=l.head.asInstanceOf[Add].arguments
        val lMult = l.tail
        Add(lAdd.map(x=>Mult(x::lMult)))
      }
      case _ => t
    }
    
    isInAdditiveCanonicalForm(t) match {
      case false => t match {
        //case x: AtomicTerm => x
        case x: Add => Add(x.arguments.map(y=> convert2AdditiveNormalForm(exploitDistributivity(orderAddsInFrontInTerm(y)))))
        case x: Mult => convert2AdditiveNormalForm(exploitDistributivity(orderAddsInFrontInTerm(Mult(x.arguments.map(y=> convert2AdditiveNormalForm(exploitDistributivity(orderAddsInFrontInTerm(y))))))))
        case _ => t
      }
      case true => t
    }
  }
  
  def collectCoefficients(t: Term, tMatch: AtomicTerm): Term ={

   def deepContain(t: Term, tMatch: AtomicTerm): Boolean = t match {
     case x if x==tMatch => true
     case x: ArithmeticFunction if x.arguments.contains(tMatch) => true
     case x: ArithmeticFunction => x.arguments.foldLeft(false)((b,y)=>b || deepContain(y,tMatch))
     case _ => false
   }
   def eraseMatch(t: Term, tMatch: AtomicTerm): Term = t match {
     case x: Add => Add(x.arguments.diff(List(tMatch)).map(y=>eraseMatch(y,tMatch)))
     case x: Mult => Mult(x.arguments.diff(List(tMatch)).map(y=>eraseMatch(y,tMatch)))
     case x => x
   }
   t match {
     case x: Add if deepContain(x,tMatch) => Add(x.arguments.map(y=> collectCoefficients(y,tMatch)))
     case x: Mult if deepContain(x,tMatch) => eraseMatch(x,tMatch)
    // case x: Mult => Add(x.arguments.map(y=> collectCoefficients(y,tMatch)))
     case x if x==tMatch => Constant(1)
     case _ => Constant(0)
    }
  }
  
  def collectStateLessTerms(t: Term): Term = {
    def isStateLessTerm(t: Term): Boolean = t match {
      case x: AtomicTerm => !x.isInstanceOf[StateVariable]
      case x: ArithmeticFunction => x.arguments.foldLeft(true)((b,y)=>b && isStateLessTerm(y))
      case _ => false
    }
   t match {
     case x if isStateLessTerm(x) => x
     case x: Add => x.arguments.filter(isStateLessTerm).size match {
       case i if i>0 => Add(x.arguments.map(collectStateLessTerms))
       case _ => Constant(0)
     }
     
     case _ => Constant(0)
    }  
  }
  
  
  def ID2Term(id: Int, l: List[(Int, Term)]): Term = l match {
      case Nil => UndefinedTerm()
      case (x::xs) if x._1==id => x._2
      case (x::xs) => ID2Term(id,xs)
  }
  
  def collectTransformedCoefficients(p: Partition, i: Int, j: Int): Term = {
    //val terms = statesTransformed(p)
    val term1: Term = convert2AdditiveNormalForm(ID2Term(i,statesTransformed(p)))
    val v2: SimulinkVertex = iD2Vertex(j,statefulVertices(p))
    val term2: AtomicTerm = v2.vertex2Expression(j, Nil).asInstanceOf[AtomicTerm]
    collectCoefficients(term1,term2)
    
  }
  
  def collectTransformedCoefficients(p: Partition, i: Int): Term = {
    val term: Term = convert2AdditiveNormalForm(ID2Term(i,statesTransformed(p)))
    collectStateLessTerms(term)
  }
  
  
  def convertEquations(f: Term=> Term, l: List[(Int, Term)]): List[(Int, Term)] = l.map(x=>(x._1,f(x._2)))
  
  def setUpMatrix(p:Partition): Matrix =  {
    //val transEq = convertEquations(convert2AdditiveNormalForm,statesTransformed(p))
    val dim = statesTransformed(p).size
    val matrix = Array.ofDim[Term](dim, dim)
    for(i<-0 to dim-1) {
      for(j<-0 to dim-1) {
        matrix(i)(j)=collectTransformedCoefficients(p,i+1,j+1)
      }
    }
    Matrix(matrix)
  }
  
  
  def setUpInputVector(p: Partition): Vector = {
    val dim = statesTransformed(p).size
    val v = Array.ofDim[Term](dim)
    for(i<-0 to dim-1) {
      v(i)=collectTransformedCoefficients(p,i+1)
    }
    Vector(v)
  }
  
  def termReplacement(t: Term, searchPattern: Term, substitute: Term): Term = t match { 
    case x if x==searchPattern => substitute
    case x: Constant => x
    case x: ArithmeticFunction => {
      val applymethod = x.getClass.getMethod("apply", classOf[List[Term]])
      applymethod.invoke(null, x.arguments.map(y=> termReplacement(y, searchPattern, substitute))).asInstanceOf[Term]
    }
    case x => x
  }
  
  
  
//  def termReplacement(t: Term, searchPattern: Term, substitute: Term): Term = t match {
//    case x if x==searchPattern => substitute
//    
//    case x: Add => Add(x.arguments.map(y=> termReplacement(y, searchPattern, substitute)))
//    case x: Mult => Mult(x.arguments.map(y=> termReplacement(y, searchPattern, substitute)))
//    case x: Exp => Exp (x.arguments.map(y=> termReplacement(y, searchPattern, substitute)))
//    case x: Ln => Ln(x.arguments.map(y=> termReplacement(y, searchPattern, substitute)))
//    case x: MultInvert => MultInvert(x.arguments.map(y=> termReplacement(y, searchPattern, substitute)))
//    case x: Square => Square(x.arguments.map(y=> termReplacement(y, searchPattern, substitute)))
//    case x: Sqrt => Sqrt(x.arguments.map(y=> termReplacement(y, searchPattern, substitute)))
//    case x: Abs => Abs(x.arguments.map(y=> termReplacement(y, searchPattern, substitute)))
//    case x: Sin => Sin(x.arguments.map(y=> termReplacement(y, searchPattern, substitute)))
//    case x: Cos => Cos(x.arguments.map(y=> termReplacement(y, searchPattern, substitute)))
//    case x => x
//  }
  
//  def resolvedObservationTerm(p: Partition): Term = {
//    val matrix = setUpMatrix(p)
//    val vector = setUpInputVector(p)
//    
//  }
}