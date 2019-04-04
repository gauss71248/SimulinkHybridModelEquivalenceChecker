package de.tu_berlin.pes.cormorant.ExpressionExtraction.denotation
import de.tu_berlin.pes.memo.model.impl._


object StateType extends Enumeration {
  type StateType = Value
  val Continuous, Discrete, UDef = Value
}

import StateType._

object GeneralType extends Enumeration {
  type GeneralType = Value
  val InstantiatedInput, AnonymousInput, Output, StateFul, NonStateFul, UDef = Value
  
  def generalBlockType(block: Block): GeneralType = block.getType match {
    case "Inport" => AnonymousInput
    case "Constant" => InstantiatedInput
    case "Sin" => InstantiatedInput
    case "Reference" => block.getParameter("SourceType") match {
      case "Ramp" => InstantiatedInput
      case _ => GeneralType.UDef
    }
    case "Bias" => NonStateFul
    case "Sum" => NonStateFul
    case "Math" => NonStateFul
    case "Product" => NonStateFul
    case "Sqrt" => NonStateFul
    case "Gain" => NonStateFul
    case "Trigonometry" => NonStateFul
    case "Abs" => NonStateFul 
    case "UnitDelay" => StateFul
    case "Memory" => StateFul
    case "Delay" => StateFul
    case "Integrator" => StateFul
    case "TransferFcn" => StateFul//StateFul 
    case "Scope" => Output
    case "Outport" => Output
    case _ => GeneralType.UDef
  }
}

import GeneralType._
import de.tu_berlin.pes.memo.MeMoPlugin

/**
 * This class associates a block from the Simulink Java IR with a unique identifier
 * @param block The block from the Simulink Java IR
 * @param id The unique identifier
 */
case class SimulinkBlock(val block: Block, val id: Int) {
  //arity prüfen -> reicht size für die Stelligkeit?
  def arity: Int = block.getInPorts.size
  def stateType: StateType = block.getType match {
    case "Integrator" => Continuous
    case "UnitDelay" => Discrete
    case "Delay" => Discrete
    case "TransferFcn" => Continuous
    case _ => StateType.UDef
  }
  
  // gets pred or same block
  def predecessor: SimulinkBlock = {
      val signals = scala.collection.JavaConversions.asScalaSet(block.getParentModel.getSignalLines)
      val b = block.getType match {
        case "InPort" => signals.filter { x => x.getDstBlock == block }.map { x => x.getDstBlock }.headOption
        case _ => None
      }
      b match {
        case Some(x) => new SimulinkBlock(x,id)
        case None => this
      }
  }

  def successor: SimulinkBlock = {
    val signals = scala.collection.JavaConversions.asScalaSet(block.getParentModel.getSignalLines)
    val b = block.getType match {
      case "OutPort" => signals.filter { x => x.getSrcBlock == block }.map { x => x.getSrcBlock }.headOption
      case _ => None
    }
    b match {
      case Some(x) => new SimulinkBlock(x,id)
      case None => this
    }
  }
  
  def generalType = generalBlockType(block)
  
  def parseDouble: () => Double =  {
    try {
      val v = block.getParameter("Gain").toDouble
      val fn = () => block.getParameter("Gain").toDouble
      return fn
    } catch {
      case _: Exception => return () => Double.NaN
    }
  }
  
  private def simulinkArray2ScalaArray(arr: String): Array[String] = arr.replaceAll("\\[", "").replaceAll("\\]", "").split(" ")
  
  def block2Function(arguments: List[Term]): Function =  block.getType match {
    case "Constant" => Constant(() => block.getParameter("Value").toDouble)
    case "Sum" => Add(arguments)
    case "Product" => Mult(arguments)
    case "Sqrt" => Sqrt(arguments)
    case "Abs" => Abs(arguments)
    case "Math" => block.getParameter("Operator") match {
      case "exp" => Exp(arguments)
      case "log" => Ln(arguments)
      case "square" => Square(arguments)
    }
    case "Gain" => Mult(List(Constant(parseDouble))++arguments)
    case "Sin" => Sin(arguments)
    case "Trigonometry" => block.getParameter("Operator") match {
        case "sin" => Sin(arguments)
    }
    case "TransferFcn" => {
      val num = simulinkArray2ScalaArray(block.getParameter("Numerator")).map { x => x.toDouble }
      val den = simulinkArray2ScalaArray(block.getParameter("Denominator")).map { x => x.toDouble }
      //println("foo", num, den)
      return TransferFcn(num,den,arguments)
    }
    case _ => UndefinedFunction()
    
  }
  
  def block2expression(arguments: List[Term]): Expression = generalType match {
    case Output => OutputEquation(OutputVariable(id, TimeVariable(0)), arguments.head)
    case AnonymousInput => InputVariable(id, TimeVariable(0))
    case InstantiatedInput => block2Function(arguments)
    case StateFul => StateVariable(id, TimeVariable(0))
    case NonStateFul => block2Function(arguments)
    case _ => new UndefinedFunction
  }
  
  
  override def toString: String = generalType match {
    case NonStateFul => "Block #" + id + "/type: " + generalType + "/function: " + block.getType
    case InstantiatedInput => "Block #" + id + "/type: " + generalType + "/function: "+block.getType
    case StateFul => "Block #" + id + "/type: " + generalType + "/state type: " +  stateType
    case _ => "Block #" + id + "/type: " + generalType
  }
}


case class SimulinkEdge(val src: SimulinkBlock, val tgt: SimulinkBlock, val arity: Int) {
  def apply(src: SimulinkBlock, tgt: SimulinkBlock, arity: Int) = new SimulinkEdge(src,tgt,arity)
  override def toString: String = "Edge:=(" + src + " ==[" + arity + "]==>" + tgt + ")" 
}

class SimulinkGraph(val vertices: Set[SimulinkBlock], val edges: Set[SimulinkEdge]) {
  
  val predecessor: SimulinkBlock => Int => Option[SimulinkBlock] = b => vertices(b) match {
    case false => (_)=>None
    case true => i => edges.find { case SimulinkEdge(x,y,n) => (y==b) && (i==n) } match {
      case Some(SimulinkEdge(a,_,_)) => Some(a)
      case _=> None
    }
  }
  /*
   * obtainTerm ist erstmal nur für kontinuierliche Systeme implementiert -> bei diskreten muss man noch checken, ob man die ineinander einsetzen darf. Muss ich noch machen.
   */
  
  val obtainTerm: SimulinkBlock => Term = b => vertices(b) match {
    case false => UndefinedFunction()
    case true => b.generalType match {
      case InstantiatedInput => b.block2Function(List(TimeVariable(0)))
      case AnonymousInput => InputVariable(b.id, TimeVariable(0))
      case StateFul => StateVariable(b.id, TimeVariable(0))
      case NonStateFul => b.block2Function((for(i<-1 to b.arity) yield predecessor(b)(i)).toList.map { x => x match {
        case Some(a) => obtainTerm(a)
        case None => UndefinedFunction()
        }
      })
      case _ => UndefinedFunction()
    }
    
  }
  
  val obtainObservation: SimulinkBlock => Equation = b => b.generalType match {
    case Output => predecessor(b)(1) match {
      case Some(p) => OutputEquation(OutputVariable(b.id, TimeVariable(0)),obtainTerm(p))
      case None => UndefinedEquation()
    }
    case _ => UndefinedEquation()
  }
  
  import EquationType._
  
  val obtainStateEquation: SimulinkBlock => Equation = b => b.generalType match {
    case StateFul => b.stateType match {
      case Continuous => predecessor(b)(1) match {
        case Some(p) => StateEquation(StateVariable(b.id,TimeVariable(0)),obtainTerm(p), ODE)
        case None => UndefinedEquation()
      }
      case Discrete => predecessor(b)(1) match {
        case Some(p) => b.block.getType match {
          case "UnitDelay" => StateEquation(StateVariable(b.id, TimeVariable(1)), obtainTerm(p),Difference)
          case "Delay" => StateEquation(StateVariable(b.id,TimeVariable(b.block.getParameter("DelayLength").toInt)), obtainTerm(p), Difference)
          case _ => UndefinedEquation()
        }
        case _ => UndefinedEquation()
      }
      case _ => UndefinedEquation()
    }
  }
  
  val denotation: SimulinkBlock => Set[Equation] = b => vertices.filter { x => x.generalType == StateFul }.map { x => obtainStateEquation(x) } ++ Set(obtainObservation(b))
  
  override def toString: String = "Vertices:={" + vertices + "}, Edges:={" + edges + "}"
 
}

object SimulinkGraph {
  
  def flatten(signals: scala.collection.mutable.Set[SimulinkEdge]): scala.collection.mutable.Set[SimulinkEdge] = signals.map { x => new SimulinkEdge(x.src.predecessor,x.tgt.successor,x.arity) }
  
  def apply(m: Model): SimulinkGraph = {
    //val tfcn = TransferFcn(Array(3,2,1),Array(7,5,3,1),Nil)
    //MeMoPlugin.out.println(tfcn)
    val blocks = scala.collection.JavaConversions.asScalaSet(m.getBlocks).toSet

    val inputBlocks = (for ((b,id)<-blocks.filter { x => generalBlockType(x)==AnonymousInput } zip Stream.from(0)) yield new SimulinkBlock(b,id)) ++ (for ((b,id)<-blocks.filter { x => generalBlockType(x)==InstantiatedInput } zip Stream.from(0)) yield new SimulinkBlock(b,id))
    val stateBlocks = for ((b,id)<-blocks.filter { x => generalBlockType(x)==StateFul } zip Stream.from(0)) yield new SimulinkBlock(b,id)
    val nonstateBlocks = for ((b,id)<-blocks.filter { x => generalBlockType(x)==NonStateFul } zip Stream.from(0)) yield new SimulinkBlock(b,id)
    val outputBlocks = for ((b,id)<-blocks.filter { x => generalBlockType(x)==Output } zip Stream.from(0)) yield new SimulinkBlock(b,id)
    val vertices = inputBlocks ++ stateBlocks ++ nonstateBlocks ++ outputBlocks
    val signals = scala.collection.JavaConversions.asScalaSet(m.getSignalLines)
    var edges = (for(s<-signals) yield {
      // check if the filter works
      val start = vertices.filter { x => x.block == s.getSrcBlock }
      val end = vertices.filter { x => x.block == s.getDstBlock }
      if (start.isEmpty || end.isEmpty) {
        None
      } else {
        Some(new SimulinkEdge(start.head, end.head, s.getDstPort.getNumber)) 
      }
    }).flatten[SimulinkEdge]
    edges = flatten(edges)
    new SimulinkGraph(vertices.toSet, edges.toSet)
  }
}
