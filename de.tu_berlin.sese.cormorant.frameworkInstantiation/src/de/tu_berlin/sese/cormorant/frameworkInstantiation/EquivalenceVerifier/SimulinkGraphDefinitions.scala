package de.tu_berlin.sese.cormorant.frameworkInstantiation.EquivalenceVerifier

import de.tu_berlin.pes.memo.model.impl._

/**
 * Function Symbols are used to identify arithmetic functions. Functions are identified in The Java Simulink IR (JSIR) in the Block class via getParameter, which
 * yields a String. In some cases, further parameter checks are necessary in order to obtain the function of the blocks. Since this is very cumbersome, we introduce the
 * standardised Function Symbols here.
 */
object FunctionSymbol extends Enumeration {
  type FunctionSymbol = Value
  val CONST, ADD, SUB, MULT, SQRT, ABS, EXP, LN, SQUARE, SIN, COS, RAMP = Value
}

//trait PrettyPrint {
//  def pretty(graph: SimulinkGraph, visited: List[SimulinkVertex]): String
//}

import FunctionSymbol._
/**
 * Time Types are associated with Simulink models. A model containing only time-discrete blocks as stateful blocks is considered to be time-discrete. Analogously, a model
 * whose stateful blocks are all time-continuous is considered to be time-continuous. Time-hybridness characterises models in which both types of stateful blocks occur.
 */
object TimeType extends Enumeration {
  type TimeType = Value
  val TimeDiscrete, TimeContinuous, TimeHybrid = Value
}

import TimeType._
/**
 * The Model Type is another attribute to characterise a model. A pure model does not contain control flow elements such as Switch blocks. If control flow elements
 * are present, the model is considered to by hybrid. Hybridness in this sense is different of time-hybridness. A hybrid model can additionally be either time-hybrid or not.
 */
object ModelType extends Enumeration {
  type ModelType = Value
  val Pure, Hybrid = Value
}
import ModelType._

/**
 * The JSIR provides a Java class hierarchy for Simulink models. It enables to iterate through the model. Unfortunately, solely read-only access is possible. However,
 * we need to modify the graph: Our approach depends on a syntax-based modification prior to generating the proof obligations for the Computer Algebra System (CAS).
 * Hence, we need a graph structure for our own purposes.
 * A Simulink Model is a pair (B,E) consisting of the set of Simulink vertices B and E\subseteq B\times B, the edges
 * The type of the vertex is determined by a certain function. This function generates the proper subclasses of the Simulink Vertex class.
 * The variables are an id as unique identifier and the Simulink Block that determines the vertex. Block b is of type Option[Block] due to the following reason:
 * Vertices generated from blocks of the model are of type Some(b). Artificially created blocks, i.e., blocks in the course of the syntactical modification in our approach
 * have type None.
 */
abstract class SimulinkVertex(val id: Int, val b: Option[Block]) {
  /**
   * The function is true if the vertex is artificially created, false if it originates fron a Simulink block of the model.
   */
  def artificiallyCreated: Boolean = b == None
  def vertex2Expression(id: Int, arguments: List[Term]): Term
  def arity: Int = b match {
    case Some(block) => block.getInPorts.size()
    case None => 1
  }
  // def pretty(g: SimulinkGraph, visited: List[SimulinkVertex]): String
  def graphviz: String
  override def toString: String = "Vertex #" + id
}
/**
 * The default error case. If the function of the block cannot be properly identified, the catch all case yields an UndefinedSimulinkVertex.
 */
case class UndefinedSimulinkVertex(override val id: Int, override val b: Option[Block]) extends SimulinkVertex(id, b) {
  override def vertex2Expression(id: Int, arguments: List[Term]): Term = new UndefinedTerm
  def vertex2Expression(id: Int): Term = vertex2Expression(id, Nil)
  override def toString: String = "Undefined Vertex #" + id
  //def pretty(graph: SimulinkGraph, visited: List[SimulinkVertex]): String = "undefined"
  override def graphviz: String = "undefined"
}
/**
 * The class represents a Simulink subsystem. Subsystems are identified but later deleted because we need a flattened graph and subsystems have no semantical impact.
 */
case class SubSystemVertex(override val id: Int, override val b: Option[Block]) extends SimulinkVertex(id, b) {
  override def vertex2Expression(id: Int, arguments: List[Term]): Term = new UndefinedTerm
  def vertex2Expression(id: Int): Term = vertex2Expression(id, Nil)
  override def toString: String = "SubSystem Vertex #" + id
  //def pretty(graph: SimulinkGraph, visited: List[SimulinkVertex]): String = "SubSystem"
  override def graphviz: String = "SubSystem"
}
/**
 * The class represents a directed edge in the Simulink Graph. An edge is an element of B\times B, i.e., a pair of two vertices.
 */
case class SimulinkEdge(val source: SimulinkVertex, val target: SimulinkVertex, val arity: Int) {
  override def toString: String = "Edge: " + source + "-->" + target + " on arity " + arity
  def graphviz: String = "\"" + source.graphviz + "\" -> \"" + target.graphviz + "\";\n"
}

/**
 * The class represents an Input vertex. This can be either an input block, i.e., an uninstantiated block (which is instantiated at runtime) or an instantiated block, i.e.,
 * a source block with an associated, fixed function, e.g. a Sine Wave Generator or a Constant block.
 */
abstract class InputVertex(override val id: Int, override val b: Option[Block]) extends SimulinkVertex(id, b) {
  override def vertex2Expression(id: Int, arguments: List[Term]): Term = new InputVariable(id)
 // def vertex2Expression(id: Int): Term
  override def toString: String = "InputVertex #" + id

}
case class InterfaceInputVertex(override val id: Int, val predblock: SimulinkVertex) extends InputVertex(id, None) {
  
  override def vertex2Expression(id: Int, arguments: List[Term]): Term = new InputVariable(id)
 // override def vertex2Expression(id: Int): Term = vertex2Expression(id, Nil)
  //override def artificiallyCreated = true
  override def toString: String = "InterfaceInputVertex #" + id
  override def graphviz: String = id + " InterfaceInput(pre: " + predblock.graphviz + ")" 
}
/**
 * An InstantiatedInputVertex represents a source block with an associated, fixed function, e.g. a Sine Wave Generator or a Constant block.
 */
case class InstantiatedInputVertex(override val id: Int, val fun: FunctionSymbol, val fixedInputConstants: List[Double], override val b: Option[Block]) extends InputVertex(id, b) {
  override def toString: String = super.toString + " with function " + fun + " and fixed params: " + fixedInputConstants
  //  def pretty(graph: SimulinkGraph, visited: List[SimulinkVertex]): String = {
  //    fun match {
  //      case ABS => fixedInputConstants.mkString(id+"abs(",",",")")
  //        case ADD => fixedInputConstants.mkString(id+"add(", ",", ")")
  //        case CONST => fixedInputConstants.mkString(id+"const(", ",", ")")
  //        case COS => fixedInputConstants.mkString(id+"cos(",",",")")
  //        case EXP => fixedInputConstants.mkString(id+"exp(",",",")")
  //        case LN => fixedInputConstants.mkString(id+"ln(",",",")")
  //        case MULT => fixedInputConstants.mkString(id+"mult(", ",", ")")
  //        case RAMP => fixedInputConstants.mkString(id+"ramp(",",",")")
  //        case SIN => fixedInputConstants.mkString(id+"sin(",",",")")
  //        case SQRT => fixedInputConstants.mkString(id+"sqrt(",",",")")
  //        case SQUARE => fixedInputConstants.mkString(id+"square(",",",")")
  //        case SUB => fixedInputConstants.mkString(id+"sub(",",",")")
  //    }
  //  }
  override def vertex2Expression(id: Int, arguments: List[Term]): Term = fun match {

    case RAMP => TimeVariable()
    case SIN => Sin(List(TimeVariable()))
    case CONST => arguments.head
    case _ => new UndefinedFunction

  }
 // override def vertex2Expression(id: Int): Term = vertex2Expression(id, Nil)
  
  
  override def graphviz: String = fun match {
    case ABS => fixedInputConstants.mkString(id + " abs(", ",", ")")
    case ADD => fixedInputConstants.mkString(id + " add(", ",", ")")
    case CONST => fixedInputConstants.mkString(id + " const(", ",", ")")
    case COS => fixedInputConstants.mkString(id + " cos(", ",", ")")
    case EXP => fixedInputConstants.mkString(id + " exp(", ",", ")")
    case LN => fixedInputConstants.mkString(id + " ln(", ",", ")")
    case MULT => fixedInputConstants.mkString(id + " mult(", ",", ")")
    case RAMP => fixedInputConstants.mkString(id + " ramp(", ",", ")")
    case SIN => fixedInputConstants.mkString(id + " sin(", ",", ")")
    case SQRT => fixedInputConstants.mkString(id + " sqrt(", ",", ")")
    case SQUARE => fixedInputConstants.mkString(id + " square(", ",", ")")

  }
}
/**
 * This class represents an Input block, i.e., an uninstantiated source block, which gets instantiated at runtime (like a parameter of a function)
 */
case class VariableInputVertex(override val id: Int, override val b: Option[Block]) extends InputVertex(id, b) {
  override def vertex2Expression(id: Int, arguments: List[Term]): Term = InputVariable(id)
  //override def vertex2Expression(id: Int): Term = vertex2Expression(id, Nil)
  
  //def pretty(graph: SimulinkGraph, visited: List[SimulinkVertex]) = "Var("+id+")"
  override def graphviz: String = id + " Input"
}
/**
 * This class represents a sink block or a data input port. Sink blocks are existent in Simulink models while Data Input Vertices do not exist in Simulink models.
 * Data Input Vertices are artificially created vertices later in the process.
 */
abstract class OutputVertex(override val id: Int, override val b: Option[Block]) extends SimulinkVertex(id, b) {
  override def vertex2Expression(id: Int, arguments: List[Term]): Term = OutputVariable(id)
  def vertex2Expression(id: Int): Term = vertex2Expression(id, Nil)
}
/**
 * This class is a standard output block. It serves as a source for signals for subsequent models.
 */
case class RegularOutputVertex(override val id: Int, override val b: Option[Block]) extends OutputVertex(id, b) {

  override def toString: String = "OutputVertex #" + id
  // def pretty(graph: SimulinkGraph, visited: List[SimulinkVertex]) = "Out("+id+")"
  override def graphviz: String = id + " out"
}
/**
 * A DataInputVertex is an artificially created, i.e., not a priori existent Simulink block. In our approach, we generate Data Flow Models. These are Simulink Models
 * without Control Flow Elements. We consider only Switch blocks in this prototype, but the approach is extendable. Our approach creates two additional models from the original model
 * for each Switch block: the Switch block is being replaced by 1) an edge from data port 1 to the output of the Switch block, and 2) an edge from data port 2 to the output of the Switch block.
 * Hence, 2^n models are created for n Switch blocks in the original model.
 * The other data port would be dangling after deleting the Switch block. Hence, we add a DataInputVertex, which is also of the type OutputVertex, to this dangling edge at the other data input port of the Switch block.
 */
case class DataInputVertex(override val id: Int, override val b: Option[Block], val dataPath: Int) extends OutputVertex(id, b) {
  require(dataPath == 1 || dataPath == 2)
  override def toString: String = "DataInputVertex #" + id
  //def pretty(graph: SimulinkGraph, visited: List[SimulinkVertex]) = "DataPortOutport("+id+")"
  override def graphviz: String = id + " DataInputVertex"
}

/**
 * This class stands for a vertex that is added to the end of the dangling edge at the control flow port of a Switch block that is being replaced in our approach.
 * It is an output vertex as well.
 */
case class ControlInputVertex(override val id: Int, override val b: Option[Block]) extends OutputVertex(id, b) {
  override def toString: String = "ControlInput Vertex #" + id
  //def pretty(graph: SimulinkGraph, visited: List[SimulinkVertex]) = "ControlPort("+id+")"
  override def graphviz: String = id + " ControlInputVertex"
}
/**
 * This class represents an output of a Partition and not an output of the SimulinkGraph. Hence, it is an Virtual Vertex which is injected into the Partition for verification purposes
 */
case class InterfaceOutputVertex(override val id: Int, val succblock: SimulinkVertex, val succArity: Int) extends OutputVertex(id, None) {
  
  override def toString: String = "InterfaceOutputVertex #" + id
  override def graphviz: String = id + " InterfaceOutputVertex(succblock: " + succblock.graphviz + ")"
}
/**
 * This class represents a vertex with impact on the control flow.
 */
abstract class ControlFlowVertex(override val id: Int, override val b: Option[Block]) extends SimulinkVertex(id, b) {
  override def vertex2Expression(id: Int, arguments: List[Term]): Term = UndefinedTerm()
  override def toString: String = "Control Flow Vertex #" + id
}
/**
 *
 */
object ValueComparisonCriteria extends Enumeration {
  type ValueComparisonCriteria = Value
  val GT, GEQ, LEQ, LT, EQ, NEQ = Value
}

import ValueComparisonCriteria._

/**
 *  This class represents a Switch Vertex. The variable threshold is the number at which the signal switches.
 */
case class SwitchVertex(override val id: Int, val threshold: Double, override val b: Option[Block]) extends ControlFlowVertex(id, b) {
  //def pretty(graph: SimulinkGraph, visited: List[SimulinkVertex]) = "Switch("+threshold+")"
  override def graphviz: String = id + " Switch >=" + threshold
}
/**
 * NonStateful vertices are also called direct-feedthrough vertices. Examples are arithmetic vertices and logical vertices.
 */
abstract class NonStateFulVertex(override val id: Int, override val b: Option[Block]) extends SimulinkVertex(id, b)
/**
 * An arithmetic vertex is a direct-feedthrough Simulink block that calculates an arithmetic function, e.g. a sum, a product, or an exponential functional value
 */
case class ArithmeticVertex(override val id: Int, val fun: FunctionSymbol, val fixedInputConstants: List[Double], val signs: String, override val b: Option[Block]) extends NonStateFulVertex(id, b) {
  override def vertex2Expression(id: Int, arguments: List[Term]): Term = fun match {
    case ABS => Abs(arguments)
    case ADD => Add((arguments zip signs.toList).map(x => x._2 match {
      case '+' => x._1
      case _ => Mult(List(new Constant(-1), x._1))
    }))
    case CONST => Constant(fixedInputConstants.head)
    case COS => Cos(arguments)
    case EXP => Exp(arguments)
    case LN => Ln(arguments)
    case MULT => Mult((arguments zip signs.toList).map(x => x._2 match {
      case '/' => MultInvert(List(x._1))
      case _ => x._1
    }))
    case RAMP => TimeVariable()
    case SIN => Sin(arguments)
    case SQRT => Sqrt(arguments)
    case SQUARE => Square(arguments)
    case _ => UndefinedFunction()
  }

  override def toString: String = "Arithmetic Vertex #" + id + " with function " + fun + " and fixed params: " + fixedInputConstants

  //def pretty(graph: SimulinkGraph): String = pretty(graph, List())

  //  def pretty(graph: SimulinkGraph, visited: List[SimulinkVertex]): String = {
  //    fun match {
  //        case ABS => fixedInputConstants.mkString(id+"abs(",",",")")
  //        case ADD => fixedInputConstants.mkString(id+"add(", ",", ")")
  //        case CONST => fixedInputConstants.mkString(id+"const(", ",", ")")
  //        case COS => fixedInputConstants.mkString(id+"cos(",",",")")
  //        case EXP => fixedInputConstants.mkString(id+"exp(",",",")")
  //        case LN => fixedInputConstants.mkString(id+"ln(",",",")")
  //        case MULT => fixedInputConstants.mkString(id+"mult(", ",", ")")
  //        case RAMP => fixedInputConstants.mkString(id+"ramp(",",",")")
  //        case SIN => fixedInputConstants.mkString(id+"sin(",",",")")
  //        case SQRT => fixedInputConstants.mkString(id+"sqrt(",",",")")
  //        case SQUARE => fixedInputConstants.mkString(id+"square(",",",")")
  //        case SUB => fixedInputConstants.mkString(id+"sub(",",",")")
  //      }
  //  }
  override def graphviz: String = fun match {
    case ABS => fixedInputConstants.mkString(id + " abs(", ",", ")")
    case ADD => fixedInputConstants.mkString(id + " add(", ",", ")")
    case CONST => fixedInputConstants.mkString(id + " const(", ",", ")")
    case COS => fixedInputConstants.mkString(id + " cos(", ",", ")")
    case EXP => fixedInputConstants.mkString(id + " exp(", ",", ")")
    case LN => fixedInputConstants.mkString(id + " ln(", ",", ")")
    case MULT => fixedInputConstants.mkString(id + " mult(", ",", ")")
    case RAMP => fixedInputConstants.mkString(id + " ramp(", ",", ")")
    case SIN => fixedInputConstants.mkString(id + " sin(", ",", ")")
    case SQRT => fixedInputConstants.mkString(id + " sqrt(", ",", ")")
    case SQUARE => fixedInputConstants.mkString(id + " square(", ",", ")")
    case SUB => fixedInputConstants.mkString(id + " sub(", ",", ")")
  }
}
/**
 * A stateful vertex is a Simulink block that has an internal value. There are time-discrete and time-continuous vertices. The type is represented by the attribute typ.
 * Examples for stateful vertices are Unit Delaz (time-discrete) and Integrator (time-continuous).
 */
abstract class StatefulVertex(override val id: Int, val typ: TimeType, override val b: Option[Block]) extends SimulinkVertex(id, b) {
  //def laplaceTerm(id: Int): Term
  override def toString: String = "StatefulVertex #" + id + " with time type " + typ
}
/**
 * A Delay Vertex is a time-discrete stateful vertex. The attribute delay represents the delay length. For instance, a Unit Delay has delay length 1. Furthermore, a delay vertex has an initial value.
 */
case class DelayVertex(override val id: Int, val initialValue: Double, val delay: Int, val sampleStepSize: Double, override val b: Option[Block]) extends StatefulVertex(id, TimeDiscrete, b) {
  override def vertex2Expression(id: Int, arguments: List[Term]): Term = delay match {
    case 1 => UnitDelayVariable(id, initialValue)
    case _ => DelayVariable(id, initialValue, delay)
  }
  def vertex2Expression(id: Int): Term = vertex2Expression(id, Nil)
  //def pretty(graph: SimulinkGraph, visited: List[SimulinkVertex]) = "Delay(init="+initialValue+",delay="+delay+",stepSize="+sampleStepSize+")"
//  def laplaceTerm(id: Int): Term = {
//    def properArityList(del: Int, l: List[Double]): List[Double] = del match {
//      case 0 => l 
//      case n => properArityList(n-1,(l:+0.0))
//    }
//    val delayTerm = RationalPolynomial(Polynomial(List(1),zVariable()), Polynomial(List(1.0) ++ properArityList(delay-1,Nil), zVariable()))
//    Add(List(Mult(List(delayTerm,vertex2Expression(id))), Mult(List(delayTerm,Constant(initialValue)))))
//  }
   //Mult(List(RationalPolynomial(Polynomial(List(1), zVariable()), Polynomial(List(1,0), LaplaceVariable())), vertex2Expression(id)))
  override def graphviz: String = id + " DelayVertex, delay=" + delay + ", initial=" + initialValue + ", sampleStepSize=" + sampleStepSize
}
/**
 * An Integrator vertex is a time-continuous stateful vertex. It has an initial value as well.
 */
case class IntegratorVertex(override val id: Int, val initialValue: Double, override val b: Option[Block]) extends StatefulVertex(id, TimeContinuous, b) {
  override def vertex2Expression(id: Int, arguments: List[Term]): Term = new IntegratorVariable(id, initialValue)
  def vertex2Expression(id: Int): Term = vertex2Expression(id, Nil)
  //def laplaceTerm(id: Int): Term = Add(List(Mult(List(RationalPolynomial(Polynomial(List(1),LaplaceVariable()),Polynomial(List(1,0), LaplaceVariable())),vertex2Expression(id))), Mult(List(RationalPolynomial(Polynomial(List(1),LaplaceVariable()),Polynomial(List(1,0), LaplaceVariable())),Constant(initialValue)))))
  //def pretty(graph: SimulinkGraph, visited: List[SimulinkVertex]) = id+"Integrator("+initialValue+")"
  override def graphviz: String = id + " Integrator, initial=" + initialValue
}
/**
 * A Transfer Function Vertex represents a transfer function (time-continuous). It contains a numerator and denominator determining the Laplace-transformed term depending on the complex variable s.
 */
case class TransferFunctionVertex(override val id: Int, val numerator: List[Double], val denominator: List[Double], override val b: Option[Block]) extends StatefulVertex(id, TimeContinuous, b) {
  override def vertex2Expression(id: Int, arguments: List[Term]): Term = new TransferFunctionVariable(id, numerator, denominator)
  def vertex2Expression(id: Int): Term = vertex2Expression(id, Nil)
 // def laplaceTerm(id: Int): Term = Mult(List(RationalPolynomial(Polynomial(numerator, LaplaceVariable()), Polynomial(denominator, LaplaceVariable())), vertex2Expression(id)))
  //def pretty(graph: SimulinkGraph, visited: List[SimulinkVertex]) = id+"TransferFcn(num="+numerator.mkString(",")+",den="+denominator.mkString(",")+")"
  override def graphviz: String = id + " TransferFunction, (" + numerator.mkString(",") + "/" + denominator.mkString(",") + ")"
}
/**
 * A Simulink Graph is a pair (B,E) consisting of the set of vertices B and the set of edges, which is a subset of B\times B
 */
case class SimulinkGraph(val vertices: Set[SimulinkVertex], val edges: Set[SimulinkEdge]) {
  /**
   * The function determines if the graph is pure. This means that no control flow vertex is present.
   */
  def isPure: Boolean = vertices.foldLeft(true)((b, v) => b && !v.isInstanceOf[ControlFlowVertex])
  /**
   * The function determines if the graph is hybrid, which means that control flow vertices are present.
   */
  def isHybrid: Boolean = vertices.foldLeft(false)((b, v) => b || v.isInstanceOf[ControlFlowVertex])
  /**
   * The function determines if the graph is purely time-discrete. This means it mus fulfill two critieria: 1) It is pure and 2) all stateful blocks are time-discrete.
   */
  def isPurelyTimeDiscrete: Boolean = isPure && vertices.foldLeft(true)((b, v) => b && (v match {
    case v if v.isInstanceOf[StatefulVertex] => v.asInstanceOf[StatefulVertex].typ == TimeDiscrete
    case _ => true
  }))
  /**
   * The function determines if the graph is purely time-continuous. This means it mus fulfill two critieria: 1) It is pure and 2) all stateful blocks are time-continuous.
   */
  def isPurelyTimeContinuous: Boolean = isPure && vertices.foldLeft(true)((b, v) => b && (v match {
    case v if v.isInstanceOf[StatefulVertex] => v.asInstanceOf[StatefulVertex].typ == TimeContinuous
    case _ => true
  }))
  /**
   * The function determines if the graph is time-hybrid. This means, it is pure, i.e., does not contain control flow elements. However, it contains both time-discrete and time-continuous elements.
   */
  def isTimeHybrid: Boolean = isPure && !(isPurelyTimeDiscrete || isPurelyTimeContinuous)
  
  def isStateLess: Boolean = vertices.foldLeft(true)((b,v)=> b && !v.isInstanceOf[StatefulVertex])
  /**
   * The function returns the maximum number of occuring IDs in vertices. It is used in functions generating new vertices to avoid clashes with other unique identifiers.
   */
  def maxID: Int = vertices.foldLeft(0)((i, v) => v.id match {
    case x if x > i => x
    case _ => i
  })
  /**
   * This function determines if a graph is flat. A graph is defined as flat if all subsystems have been erased. This also includes the deletion of input vertices with preceding blocks and output vertices with
   * subsequent blocks. Both cases occur if subsystems are present.
   */
  def isFlat: Boolean = vertices.foldLeft(true)((b, v) => !v.isInstanceOf[SubSystemVertex] && b) && edges.foldLeft(true)((b, e) => !e.target.isInstanceOf[InputVertex] && b) && edges.foldLeft(true)((b, e) => !e.source.isInstanceOf[OutputVertex] && b)

  /**
   * directpredecessor assumes a vertex with a single inport. It obtains the direct predecessor of the vertex.
   */
  def directpredecessor(v: SimulinkVertex): SimulinkVertex = edges.find { x => x.target == v } match {
    case Some(x) => x.source
    case _ => new UndefinedSimulinkVertex(maxID + 1, None)
  }
  /**
   * The function returns a preceding block whose outgoing signal line connects at the given arity of the given vertex.
   */
  def directpredecessor(v: SimulinkVertex, a: Int): SimulinkVertex = edges.find(x => x.target == v && x.arity == a) match {
    case Some(x) => x.source
    case None => new UndefinedSimulinkVertex(maxID + 1, None)
  }
  /**
   * directPredecessors delivers the direct predecessors of a vertex. This function works also with vertices with multiple inports.
   */
  def directPredecessors(v: SimulinkVertex): Set[SimulinkVertex] = edges.filter { x => x.target == v }.map { x => x.source }
  
  def directPredecessorsSorted(v: SimulinkVertex): List[SimulinkVertex] = edges.filter(x => x.target==v).toList.sortBy(x => x.arity).map(x => x.source)
  /**
   * directPredecessorsSorted delivers a list of SimulinkVertices that directly precede the current vertex and are sorted by the arity at which they are connected
   * with the current vertex.
   */
  //def directPredecessorsSorted(v: SimulinkVertex): List[SimulinkVertex] = directPredecessors(v).toList.sortBy(v => nm)
  /**
   * predecessors delivers all predecessors transitively, i.e., all vertices on which there exists a data dependency to the current vertex
   */
  def predecessors(v: SimulinkVertex, container: Set[SimulinkVertex]): Set[SimulinkVertex] = v.isInstanceOf[InputVertex] match {
    case true => container
    case false if directPredecessors(v).foldLeft(true)((b, e) => b && container.contains(e)) => container
    case _ => directPredecessors(v).map { x => predecessors(x, container ++ directPredecessors(v)) }.flatten
    //alt: directPredecessors(v)++ (directPredecessors(v).map { x => predecessors(x) }).flatten
  }
  /**
   * successors delivers all directly succeeding vertices. The return value is a set of pairs. Each pair consists of a succeeding block and the arity at which the incoming signal originating
   * from the block under consideration arrives at the succeeding vertex.
   */
  def successors(v: SimulinkVertex): Set[(SimulinkVertex, Int)] = edges.filter { x => x.source == v }.map { x => (x.target, x.arity) }
  /**
   * containsConnection checks if an edge between two given vertices exists in the graph, i.e., if the first vertex is directly connected with the second one.
   */
  def containsConnection(s: SimulinkVertex, t: SimulinkVertex): Boolean = edges.find { e => e.source == s && e.target == t } match {
    case Some(e) => true
    case _ => false
  }
  /**
   * This function is used for the flattening process. It deletes a vertex and re-reoutes the connections: All incoming edges are re-reouted to the successors of the
   * vertex that is designated for deletion. The re-routing also respects the arity of subsequent vertices.
   */
  def removeVertexAndReconnect(v: SimulinkVertex): SimulinkGraph = {
    val newVertices = vertices - v
    val edges2Add = for ((v2, i) <- successors(v)) yield new SimulinkEdge(directpredecessor(v), v2, i)
    val edges2Delete = edges.filter { x => x.target == v } union edges.filter { x => x.source == v }
    val newEdges = edges -- edges2Delete ++ edges2Add
    new SimulinkGraph(newVertices, newEdges)
  }
  /**
   * This function replaces a Switch Vertex. It is used to calculate the data flow paths. The Integer dataFlowPath determines which data port is connected with the output of the Switch block.
   * It can be either 1 or 2. The other data port and the control port, which would leave a dangling edge after the deletion of the Switch block, are connected with DataInput or ControlPort Vertices.
   * These are of type OutputVertex because in the equivalence checking process, they are treated as output vertices, i.e., as points of observations, which need to behave equivalently in source and target model.
   */
  def replaceSwitchVertex(v: SwitchVertex, dataFlowPath: Int): SimulinkGraph = {

    val ingoingDataEdge: Option[SimulinkEdge] = edges.find { x => (x.target == v && x.arity == dataFlowPath) }
    val otherDataPortEdge: Option[SimulinkEdge] = dataFlowPath match {
      case 1 => edges.find { x => (x.target == v && x.arity == 3) }
      case 3 => edges.find { x => (x.target == v && x.arity == 1) }
      case _ => None
    }
    val ingoingControlEdge: Option[SimulinkEdge] = edges.find { x => x.target == v && x.arity == 2 }

    val outgoingEdges: Set[SimulinkEdge] = edges.filter { x => x.source == v }

    val reducedEdges: Set[SimulinkEdge] = ingoingDataEdge match {
      case Some(e1) => otherDataPortEdge match {
        case Some(e2) => ingoingControlEdge match {
          case Some(c) => (((edges - e1) - e2) - c) -- outgoingEdges
          case _ => edges - e1 - e2 -- outgoingEdges
        }
        case _ => (edges - e1) -- outgoingEdges
      }
      case _ => edges -- outgoingEdges
    }
    val output4OtherDataPath = dataFlowPath match {
      case 1 => new DataInputVertex(maxID + 1, None, 2)
      case _ => new DataInputVertex(maxID + 1, None, 1)
    }

    val controlOutput = new ControlInputVertex(maxID + 2, None)

    val newVertices = ((vertices - v) + output4OtherDataPath) + controlOutput

    val newEdges: Set[SimulinkEdge] = ingoingDataEdge match {
      case Some(eDF1) => otherDataPortEdge match {
        case Some(eDF2) => ingoingControlEdge match {
          case Some(c) => reducedEdges ++ (for (o <- outgoingEdges) yield new SimulinkEdge(eDF1.source, o.target, o.arity)) + new SimulinkEdge(eDF2.source, output4OtherDataPath, 1) + new SimulinkEdge(c.source, controlOutput, 1)
          case _ => reducedEdges
        }
        case _ => reducedEdges
      }

      case _ => reducedEdges
    }

    new SimulinkGraph(newVertices, newEdges)
  }

  def addInterface(v1: InterfaceInputVertex, v2: SimulinkVertex, l: List[Int]): SimulinkGraph = l.foldLeft(this)((g, i) => new SimulinkGraph(g.vertices + v1, g.edges + new SimulinkEdge(v1, v2, i)))

  // def addInterfaces(x: Set[(SimulinkVertex,List[Int])]): SimulinkGraph = x.foldLeft(this)((g,a)=> addInterface(new InterfaceInputVertex(maxID, None, )

  def graphviz: String = {
    val vStrings = vertices.map(v => "\"" + v.graphviz + "\";\n")
    val eStrings = edges.map(e => "\"" + e.source.graphviz + "\" -> \"" + e.target.graphviz + "\";\n")
    vStrings.foldLeft("")((s, v) => s + v) + eStrings.foldLeft("")((s, e) => s + e)

  }
  /**
   * The function flattens the graph. Subsystems are removed. Inports within subsystems and outport blocks within subsystems are deleted as well, respective edges are re-routed. The result is a model
   * on one level only. This course of action is okay because subsystems have no impact on the semantics.
   */
  def flatten: SimulinkGraph = isFlat match {
    case true => new SimulinkGraph(vertices, edges)
    case _ => {
      val vertices2Replace = (edges.filter { x => x.target.isInstanceOf[InputVertex] }.map { x => x.target } union edges.filter { x => x.source.isInstanceOf[OutputVertex] }.map { x => x.source })
      val verticesSubSystems = vertices.filter { x => x.isInstanceOf[SubSystemVertex] }
      val graph1 = vertices2Replace.foldLeft(this)((g, v) => removeVertexAndReconnect(v))
      new SimulinkGraph(graph1.vertices -- verticesSubSystems, graph1.edges -- graph1.edges.filter { x => x.target.isInstanceOf[SubSystemVertex] }).flatten
    }
  }

  /**
   * This function checks if there exists a path between two vertices within the graph.
   */
  def existsPath(v1: SimulinkVertex, v2: SimulinkVertex): Boolean = existsPath(v1, v2, Nil.toSet)
  /**
   * This is a helping function for existsPath. It uses a working set to avoid unlimited runs in cycles during the recursion.
   */
  private def existsPath(v1: SimulinkVertex, v2: SimulinkVertex, working: Set[SimulinkVertex]): Boolean = v1 == v2 match {
    case true => true

    case false if !successors(v1).foldLeft(true)((b, v) => b && working.contains(v._1)) => successors(v1).filter(x => !working.contains(x._1)) match {
      case s if s.isEmpty => false
      case s => s.foldLeft(false)((b, v) => b || existsPath(v._1, v2, working + v._1))
    }
    case _ => false
  }
  /**
   * The function tells if two vertices in the graph are equivalent, i.e., in relation with respect to the following equivalence relation:
   * v1=v2 if and only if a path exists from v1 to v2 and vice versa
   */
  def equivalent(v1: SimulinkVertex, v2: SimulinkVertex): Boolean = existsPath(v1, v2) && existsPath(v2, v1)

  /**
   * The function is an auxiliary function for the isDAG function. It defines the recursion.
   */
  private def isDAG(l: List[SimulinkVertex]): Boolean = l match {
    case Nil => true
    case (p :: ps) => vertices.foldLeft(true)((b, v) => b && ((p == v) || !equivalent(p, v))) && isDAG(ps)
  }
  /**
   * The function tells if the graph is acyclic. It is the terminating case to calculate the second step of the partitioning, the merging of cyclic elements.
   */
  def isDAG: Boolean = isDAG(vertices.toList)
  /**
   * graphRestriction gives back a restriction of the set of vertioces and edges that are contained in a given Set of SimulinkVertices
   */
  def graphRestriction(s: Set[SimulinkVertex]): SimulinkGraph = new SimulinkGraph(vertices.filter(v => s.contains(v)), edges.filter(e => (s.contains(e.source) && s.contains(e.target))))

  override def toString: String = "Vertices: " + vertices + " ||| Edges: " + edges
}
import de.tu_berlin.pes.memo.MeMoPlugin
object SimulinkGraph {

  private def block2Vertex(b: Block, i: Int): SimulinkVertex = b.getType match {
    case "Inport" => new VariableInputVertex(i, Some(b))
    case "Constant" => new InstantiatedInputVertex(i, CONST, List(b.getParameter("Value").toDouble), Some(b))
    case "Sin" => new InstantiatedInputVertex(i, SIN, List(), Some(b))
    case "Reference" => b.getParameter("SourceType") match {
      case "Ramp" => new InstantiatedInputVertex(i, RAMP, List(), Some(b))
      case _ => new UndefinedSimulinkVertex(i, Some(b))
    }
    case "Bias" => new ArithmeticVertex(i, ADD, List(b.getParameter("Bias").toDouble), "", Some(b))
    case "Sum" => new ArithmeticVertex(i, ADD, Nil, b.getParameter("Inputs"), Some(b))
    case "Math" => b.getParameter("Operator") match {
      case "exp" => new ArithmeticVertex(i, EXP, Nil, "", Some(b))
      case "log" => new ArithmeticVertex(i, LN, Nil, "", Some(b))
      case "square" => new ArithmeticVertex(i, SQUARE, Nil, "", Some(b))
    }
    case "Product" if b.getParameter("Inputs").charAt(0) == "*" || b.getParameter("Inputs").charAt(0) == "/" => new ArithmeticVertex(i, MULT, Nil, b.getParameter("Inputs"), Some(b))
    case "Product" => new ArithmeticVertex(i, MULT, Nil, "**", Some(b))
    case "Sqrt" => new ArithmeticVertex(i, SQRT, Nil, "", Some(b))
    case "Gain" => new ArithmeticVertex(i, MULT, List(b.getParameter("Gain").toDouble), "**", Some(b))
    case "Trigonometry" => b.getParameter("Operator") match {
      case "sin" => new ArithmeticVertex(i, SIN, Nil, "", Some(b))
      case "cos" => new ArithmeticVertex(i, COS, Nil, "", Some(b))
    }
    case "Abs" => new ArithmeticVertex(i, ABS, Nil, "", Some(b))
    case "UnitDelay" => new DelayVertex(i, b.getParameter("InitialCondition").toDouble, 1, b.getParameter("SampleTime").toDouble, Some(b))
    case "Memory" => new DelayVertex(i, b.getParameter("InitialCondition").toDouble, 1, b.getParameter("SampleTime").toDouble, Some(b))
    case "Delay" => new DelayVertex(i, b.getParameter("InitialCondition").toDouble, b.getParameter("DelayLength").toInt, b.getParameter("SampleTime").toDouble, Some(b))
    case "Integrator" => new IntegratorVertex(i, b.getParameter("InitialCondition").toDouble, Some(b))
    case "TransferFcn" => new TransferFunctionVertex(i, b.getParameter("Numerator").replaceAll("\\[", "").replaceAll("\\]", "").replaceAll(",", " ").split(" ").toList.filter(!_.isEmpty()).map { x => x.toDouble }, b.getParameter("Denominator").replaceAll("\\[", "").replaceAll("\\]", "").replaceAll(",", " ").split(" ").toList.filter(!_.isEmpty()).map { x => x.toDouble }, Some(b))
    case "Scope" => new RegularOutputVertex(i, Some(b))
    case "Outport" => new RegularOutputVertex(i, Some(b))
    case "Switch" => new SwitchVertex(i, b.getParameter("Threshold").toDouble, Some(b))
    case "SubSystem" => new SubSystemVertex(i, Some(b))
    case _ => new UndefinedSimulinkVertex(i, Some(b))
  }
  //  catch{
  //    case _=> {
  //       MeMoPlugin.out.println(b)
  //       new UndefinedSimulinkVertex(i,Some(b))
  //    }
  //  }

  //   def TransferFunction2SubSystem(v: TransferFunctionVertex): SimulinkGraph = {
  //     
  //   }

  /**
   * Apply sets up the graph from a Simulink representation in Java (MeMo representation).
   */
  def apply(m: Model): SimulinkGraph = {
    val blocks = scala.collection.JavaConversions.asScalaSet(m.getBlocks).toSet
    val signalLines = scala.collection.JavaConversions.asScalaSet(m.getSignalLines)

    val vertices: Set[SimulinkVertex] = for ((b, i) <- blocks zip Stream.from(1)) yield block2Vertex(b, i)

    val edges: Set[SimulinkEdge] = for {
      b2 <- blocks
      (s, i) <- scala.collection.JavaConversions.asScalaSet(b2.getInSignals).toList.sortBy { x => x.getDstPort.getNumber } zip Stream.from(1)

    } yield vertices.find { x => x.b == Some(s.getSrcBlock) } match {
      case Some(v1) => vertices.find { x => x.b == Some(b2) } match {
        case Some(v2) => new SimulinkEdge(v1, v2, i)

      }
    }
    new SimulinkGraph(vertices, edges).flatten
  }

}