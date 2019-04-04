package de.tu_berlin.pes.cormorant.ExpressionExtraction.denotation
import AbstractRepresentation.FunctionType._
/**
 * Expressions can be either Terms or Equations.
 *
 */
sealed abstract class Expression
/**
 * A term is inductively defined. Atomic terms are variable symbols
 */
abstract class Term extends Expression {
  def eval: Option[Double]
}

abstract class AtomicTerm extends Term
/**
 * A Time Variable is an atomic term
 */

abstract class Variable extends AtomicTerm

case class TimeVariable(val lambda: Int) extends Variable {
  require(lambda>=0)
  def eval: Option[Double] = None
  override def toString: String = lambda match {
    case n if n>0 => "t_{n+" + lambda + "}"
    case _ => "t_n"
  }
}

//case class Constant(val value: Double) extends AtomicTerm {
//  def eval: Option[Double] = Some(value)
//  override def toString = value toString
//}

case class StateVariable(val id: Int, val t: TimeVariable) extends Variable {
  def eval: Option[Double] = None
  override def toString: String = "y_{" + id + "}(" + t + ")"
}

case class OutputVariable(val id: Int, val t: TimeVariable) extends Variable {
  def eval: Option[Double] = None
  override def toString: String = "out_{" + id + "}(" + t + ")"
}
   
case class InputVariable(val id: Int, val t: TimeVariable) extends Variable {
  def eval: Option[Double] = None
  override def toString: String = "in_{" + id + "}(" + t + ")"
}

abstract class Function extends Term {
  def arity: Int
}


case class TransferFcn(val num: Array[Double], val den: Array[Double], val arguments: List[Term]) extends Function {
  // simulink doc says the order of the denominator must be greater or equal to the order of the numerator
  require(den.size >= num.size)
  
  def arity: Int = 1
  // todo add an eval
  def eval: Option[Double] = None
  
  private def getMonom(value: Array[Double], counter: Int): String = {
    var result: String = new String()
    val maxSize = value.size
    result += value(counter)
    if (maxSize-counter-1 != 0) {
      if (maxSize-counter-1 != 1) {
        result += "s^"+(maxSize-counter-1)
      } else {
        result += "s"
      }
    }
    result
  }
  
  override def toString: String = {
    val top = (0 to (num.size-1)).map(x => getMonom(num,x)).filter(x => !x.isEmpty()).mkString("+")
    val bottom = (0 to (den.size-1)).map(x => getMonom(den,x)).filter(x => !x.isEmpty()).mkString("+")
    "TransferFcn { num:"+top+"; den:"+bottom+ " }("+arguments.mkString(",")+")"
  }
}

abstract class ArithmeticFunction extends Function {
  def eval: Option[Double]
}

case class Constant(val c:() => Double) extends ArithmeticFunction {
  def arity: Int = 0
  def eval: Option[Double]=Some(c())
  override def toString: String = c() toString
}

abstract class unaryArithmeticFunction(val f: Double=>Double, val argument: List[Term]) extends ArithmeticFunction {
  def arity = 1
  def eval: Option[Double] = argument.head.eval match {
    case Some(a) => Some(f(a))
    case _ => None
  }
}

abstract class multiDigitArithmeticFunction(val f: (Double,Double)=>Double, val arguments: List[Term]) extends ArithmeticFunction {
  require(arguments.length>1)
  def arity = arguments.length
  def eval: Option [Double] = arguments.tail.map { x => x.eval }.foldLeft(arguments.head.eval)((x,y)=>(x,y) match {
    case (Some(a),Some(b))=> Some(f(a,b))
    case _=> None
  })
}

case class Add(override val arguments: List[Term]) extends multiDigitArithmeticFunction((_+_),arguments) {
  
  //def eval: Option[Double]=arguments.map {x => x.eval}.foldLeft[Option[Double]](Some(0.0))((x,y)=> (x,y) match {
   //                                                                     case (Some(a),Some(b)) => Some(a+b)
    //                                                                    case _ => None
     //                                                                   })
                                                                     
  override def toString: String = "("+arguments.foldLeft("")((s,x)=>s+x+"+").dropRight(1)+")"
  
}

case class Mult(override val arguments: List[Term]) extends multiDigitArithmeticFunction((_*_),arguments) {
                                                                        
  override def toString: String = "("+arguments.foldLeft("")((s,x)=>s+x+"*").dropRight(1)+")"
  
}

case class MultInvert(override val argument: List[Term]) extends unaryArithmeticFunction((1/_),argument) {
  
  override def eval: Option[Double]= argument.head.eval match {
    case Some(a) if a!=0 => super.eval
    case _ => None
  }
  override def toString: String = "1/(" + argument.head + ")"
}

case class Square(override val argument: List[Term]) extends unaryArithmeticFunction(a=>(a*a),argument) {
                                                                        
  override def toString: String =  "(" + argument + ")²"
}

case class Sqrt(override val argument: List[Term]) extends unaryArithmeticFunction(Math.sqrt(_),argument) {
  
  override def toString: String = "√(" + argument.head + ")"
}

case class Exp(override val argument: List[Term]) extends unaryArithmeticFunction(scala.math.exp(_),argument) {
                                                                        
  override def toString: String = "exp(" + argument.head + ")"
}

case class Ln(override val argument: List[Term]) extends unaryArithmeticFunction(scala.math.log(_),argument)  {
  
  override def eval: Option[Double]=argument.head.eval match {
     case Some(a) if a>0.0 => super.eval
     case _ => None
    
  }
                                                                        
  override def toString: String = "ln(" + argument.head + ")"
}

case class Abs(override val argument: List[Term]) extends unaryArithmeticFunction(scala.math.abs(_),argument) {
                                                                     
  override def toString: String = "|" + argument.head+ "|"
  
}

case class Sin(override val argument: List[Term]) extends unaryArithmeticFunction(scala.math.sin(_),argument) {
                                                                        
  override def toString: String = "sin(" + argument.head + ")"
}

case class Cos(override val argument: List[Term]) extends unaryArithmeticFunction(scala.math.cos(_),argument) {
                                                                        
  override def toString: String = "cos(" + argument.head + ")"
}

case class UndefinedFunction() extends Function {
  def arity: Int = 0
  def eval: Option[Double]=None
  override def toString: String = "Undefined function"
}

object EquationType extends Enumeration {
  type EquationType = Value
  val ODE, Difference, DDE = Value
}

import EquationType._

abstract class Equation extends Expression

case class StateEquation(val left: StateVariable, val right: Term, val eqType: EquationType) extends Equation {
  override def toString: String = eqType match {
    case ODE => "d/dt" + left + "=" + right
    case _ => left + "=" + right
  }
  
}

case class OutputEquation(val left: OutputVariable, val right: Term) extends Equation {
  override def toString: String = left + "=" + right
}

case class UndefinedEquation() extends Equation {
  override def toString: String = "Undefined Equation"
}

