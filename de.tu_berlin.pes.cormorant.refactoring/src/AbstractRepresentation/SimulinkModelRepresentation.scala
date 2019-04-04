package AbstractRepresentation

/**
 * @author gauss
 */

import de.tu_berlin.pes.memo.model.impl._

object ModelType extends Enumeration {
  type ModelType=Value
  val Input, Output, Unsampled, Discrete, Continuous, Hybrid = Value
  
  override def toString: String = ModelType match {
    case Input => "Input"
    case Output => "Output"
    case Unsampled => "Unsampled"
    case Discrete => "Discrete"
    case Continuous => "Continuous"
    case Hybrid => "Hybrid"
    case _ => "Undefined Type"
  }
} 

import ModelType._
import java.util.Arrays.ArrayList

class ARBlock(val block: Block, val id: Int) {
  def name: String = block.getName
  def blocktype: Option[ModelType] = block.getType match {
    case "Inport" => Some(Input)
    case "Constant" => Some(Input)
    case "Sin" => Some(Input)
    case "Reference" => block.getParameter("SourceType") match {
      case "Ramp" => Some(Input)
    }
    case "Bias" => Some(Unsampled)
    case "Sum" => Some(Unsampled)
    case "Math" => Some(Unsampled)
    case "Product" => Some(Unsampled)
    case "Sqrt" => Some(Unsampled)
    case "Gain" => Some(Unsampled)
    case "Trigonometry" => Some(Unsampled)
    case "Abs" => Some(Unsampled)    
    case "UnitDelay" => Some(Discrete)
    case "Delay" => Some(Discrete)
    case "Integrator" => Some(Continuous)
    case "TransferFcn" => Some(Continuous)
    case "Scope" => Some(Output)
    case "Outport" => Some(Output)
    case _ => None
  }
  
  
  //functionType für unsampled Blöcke noch unvollständig: Math und Trigonometry etc. muss noch per Parameter weiter runter geprüft werden (damit man EXP erkennt etc.)
  // ggf. auch für InputBlöcke erweitern
  import FunctionType._
  def functionType: Option[FunctionType] = blocktype match {
    
    case Some(Unsampled) => block.getType match {
      case "Sum" => Some(ADD)
      case "Product" => Some(MULT)
      case "Sqrt" => Some(SQRT)
      case "Abs" => Some(ABS)
      case "Math" => block.getParameter("Operator") match {
        case "exp" => Some(EXP)
        case "log" => Some(LN)
        case "square" => Some(SQUARE)
      }
      case "Trigonometry" => block.getParameter("Operator") match {
        case "sin" => Some(SIN)
      }
      case _ => None
    }
    
    case _ => None
    
  }
  
 
  // Achtung! isLinear noch nicht vollständig korrekt implementiert - Linear ist es auch, wenn der Product Block nur ein Signal mit einer Konstante multiplikativ verknüpft-> muss noch gecheckt werden
  def isLinear: Boolean = blocktype match {
    case Some(Unsampled) => block.getType match {
      case "Sum" => true
      case "Gain" => true
      case _ => false
    }
    case _ => true
   
  }
  
  override def equals(other: Any): Boolean = other match {
    case a: ARBlock if (a.block equals block) && (a.id==id) => true
    case _ => false
  }
  
  def equalsInBlocksOnly(other: Block): Boolean = other equals block
  
  override def hashCode: Int = block.hashCode() + id
 
  override def toString: String = blocktype match {
    case Some(x) => "BLOCK :" + block.toString + ", LABELID: " + id + ", TYPE: "+ x toString
    case None => "BLOCK: " + block.toString + ", LABELID: " + id + ", TYPE: Undefined"
  }
}

object ARBlock {
  def apply(b: Block, id: Int) = new ARBlock(b,id)
  def unapply(b: Block, id: Int): Option[(Block,Int)]=Some((b,id))
}

object ARSimulinkModelRepresentation{
  def modelTypeComparison(t1: Option[ModelType], t2: Option[ModelType]): Option[ModelType] = t1 match {
    case None => None
    case _ if t2==None => None
    case Some(Input) if t2==Some(Input) || t2==Some(Output) => Some(Unsampled)
    case Some(Input) => t2
    case Some(Output) if t2==Some(Output) || t2==Some(Input) => Some(Unsampled)
    case Some(Output) => t2
    case Some(Unsampled) if t2==Some(Unsampled) => Some(Unsampled)
    case Some(Unsampled) if t2==Some(Discrete) => Some(Discrete)
    case Some(Unsampled) if t2==Some(Continuous) => Some(Continuous)
    case Some(Discrete) if t2==Some(Unsampled) || t2==Some(Discrete) => Some(Discrete)
    case Some(Discrete) if t2==Some(Continuous) => Some(Hybrid)
    case Some(Continuous) if t2==Some(Continuous) || t2==Some(Unsampled) => Some(Continuous)
    case Some(Continuous) if t2==Some(Discrete) => Some(Hybrid)
    case _ => modelTypeComparison(t2,t1)
  }
  
}

class ARSimulinkModelRepresentation(m: Model) {

  import scalax.collection.Graph
  import scalax.collection.GraphPredef._
  import scalax.collection.GraphEdge._
  import scalax.collection.edge.LDiEdge
  import scalax.collection.edge.Implicits._ // shortcuts
  import ARSimulinkModelRepresentation._
  import FunctionType._
  /**
   * vertices is a set of pairs (ARBlock, id) assigning a unique ID (for the labels of the outgoing edges of each block)
   */
  val vertices = for((b,id)<-scala.collection.JavaConversions.asScalaSet(m.getBlocks) zip Stream.from(0)) yield ARBlock(b,id)
  
  def vertex(b: Block): ARBlock = vertices.find(x => x.equalsInBlocksOnly(b)) match {
    case Some(b) => b
    case _ => ARBlock(new Block(), -1)
  }
  
  //returns source vertex of the signal with the specified label
   def srcVertex(id: Int): ARBlock = vertices.find { b=> b.id==id} match{
     case Some(b) => b
     case _ => ARBlock(new Block(), -1)
   }
   
    def edge2signalLine(b1:ARBlock, b2: ARBlock): SignalLine =scala.collection.JavaConversions.asScalaSet(m.getSignalLines).find { x => x.getSrcBlock==b1.block && x.getDstBlock==b2.block } match {
    case Some(l) => l
    case _ => new SignalLine
  }
   
  val blocks = vertices.map {x => x.block}
  
  case class SimulinkEdge(label: Int, arity: Int) 
  
  //val edges = scala.collection.JavaConversions.asScalaSet(m.getSignalLines).map { x => (vertex(x.getSrcBlock) ~+> vertex(x.getDstBlock)) (vertex(x.getSrcBlock).id) }
 val edges = scala.collection.JavaConversions.asScalaSet(m.getSignalLines).map { x => (vertex(x.getSrcBlock) ~+> vertex(x.getDstBlock)) (SimulinkEdge(vertex(x.getSrcBlock).id, edge2signalLine(vertex(x.getSrcBlock), vertex(x.getDstBlock)).getDstPort.getId)) }
  
  
  def tgtVertex(id:Int): ARBlock = edges.find { x=>x.label==id } match {
     case Some(e) => e._2
     case _ => ARBlock(new Block(), -1)
  }
  
 
  
  def edge2signalLine(i:Int): SignalLine = edge2signalLine(srcVertex(i), tgtVertex(i))
  
  val graph = Graph.from(List(), edges)
  
  
  val blocktypes = vertices map { x => x.blocktype }
  
  def modelType: Option[ModelType] = blocktypes.reduceLeft ((a, b) => modelTypeComparison(a, b))
 
  def isLinear: Boolean = vertices.foldLeft(true)((a, b) => a && b.isLinear)
  
  
  //unvollständig
  def test(block: ARBlock)={
    graph.find(block) match {
      case Some(b) => b.outgoing.head.label match {
        case SimulinkEdge(l,i) => l.toString.toInt
        case _ => "Klappt nich"
      }
      case _ => "Klappt nich"
    }
  }
  
  def getAR(block: ARBlock): Option[Equation] = {
    val labelid_out: Int = graph.find(block) match {
      case Some(b) => b.outgoing.head.label match {
        case SimulinkEdge(l,i) => l.toString.toInt 
      
      }
      case _ => -1
    }
    val labelid_args: Set[Pair[Int,Int]] = graph.find(block) match {
      case Some(b) => b.incoming map { x => x.label match {
        case SimulinkEdge(l,i) => (l,i)
      }} 
      case _ => Set()
    }
    
    block.blocktype match {
      case Some(Input) => block.block.getType match {
        case "Constant" => Some(Equation(Variable(labelid_out, TimeVariable(0)), Constant(block.block.getParameter("Value").toDouble), false))
        case "Reference" => block.block.getParameter("SourceType") match {
          case "Ramp" => Some(Equation(Variable(labelid_out,TimeVariable(0)),TimeVariable(0) , false))
        }
        case "Sin" => Some(Equation(Variable(labelid_out, TimeVariable(0)), Function(SIN, List(TimeVariable(0))), false))
        case _ => None
      }
      
      case Some(Unsampled) => block.functionType match {
        case Some(f) => block.block.getType match {
          case "Sum" => Some(Equation(Variable(labelid_out,TimeVariable(0)), Function(f, setUpArgumentTerms4AR(labelid_args, block.block.getParameter("Inputs"))), false))
          case "Product" => Some(Equation(Variable(labelid_out,TimeVariable(0)), Function(f, setUpArgumentTerms4AR(labelid_args, block.block.getParameter("Inputs"))), false))
          case _ => Some(Equation(Variable(labelid_out,TimeVariable(0)), Function(f, setUpArgumentTerms4AR(labelid_args, "")), false))
        }
        case _ => None
      }
      case Some(Continuous) => block.block.getType match {
        case "Integrator" => Some(Equation(Variable(labelid_out,TimeVariable(0)), Variable(labelid_args.head._1, TimeVariable(0)), true))
      }
      case Some(Discrete) => block.block.getType match {
        case "UnitDelay" => Some(Equation(Variable(labelid_out, TimeVariable(1)), Variable(labelid_args.head._1, TimeVariable(0)), false))
        case "Delay" => Some(Equation(Variable(labelid_out, TimeVariable(block.block.getParameter("DelayLength").toInt)), Variable(labelid_args.head._1, TimeVariable(0)), false))
      }
      
      case _ => None
    }
  }
  
  
  
  
  def getPortByEdgeLabel(id:Int): Int = tgtVertex(id).block.getInPortsMap.get(edge2signalLine(id)).getId 
    
  def setUpArgumentTerms4AR(args: Set[Pair[Int, Int]], param: String): List[Term] = setUpSortedArgumentTerms4AR(args.toList.sortBy(x=>x._2).map(x=>x._1), param)
    
  
  private def setUpSortedArgumentTerms4AR(args: List[Int], param: String): List[Term] =  param match { 
    case p if p.length > 0 => (args zip param).head match {
      case (x,sign) if sign=='-' => List(Function(MULT, List(Constant(-1), Variable(x,TimeVariable(0))))) ++ setUpSortedArgumentTerms4AR(args.tail, (new StringBuilder(param)).tail.toString) 
      case (x, sign) if sign=='/' => List(Function(INVERT, List(Variable(x,TimeVariable(0))))) ++ setUpSortedArgumentTerms4AR(args.tail, (new StringBuilder(param)).tail.toString)
      case (x,_) => List(Variable(x,TimeVariable(0))) ++ setUpSortedArgumentTerms4AR(args.tail, (new StringBuilder(param)).tail.toString) 
        
    }
    case _ if args.size > 0 => List(Variable(args.head,TimeVariable(0))) ++ setUpSortedArgumentTerms4AR(args.tail, param)
    case _ => List()
       
  } 
  override def toString = AR toString
  import collection.JavaConverters
  def toARList : java.util.ArrayList[Equation] = {
    var t = new java.util.ArrayList[Equation]()
    for (el <- AR) el match { 
      case s: Option[Equation]  => 
        if (s.isDefined) {
          t.add(s.get)
        }
      case _ => 
    }
    return t
  }
    
  def AR = vertices.filter { x => x.blocktype!=Some(Output) } map {x => getAR(x)}
  
  /**def unPack(s: Set[Option[Any]]) = s match {
    case (Some(x)::xs) => (x::unPack xs)
    case (None::xs) => unPack xs
    case _=> Set()
  }
  */
  
}

