package AbstractRepresentation

/**
 * @author gauss
 * This file contains the definitions for the Abstract Representations, namely terms and equations
 */
sealed abstract class Term

case class Constant (val value: Double) extends Term {
  override def toString = value.toString()
}

case class Variable(val id: Int, val t: TimeVariable) extends Term {
  require(id>=0)
  override def toString: String = "l_" + id + "(" + t + ")"
}

case class TimeVariable(val offset: Int) extends Term {
  require(offset>=0)
  override def toString: String = offset match  {
    case 0 => "t"
    case 1 => "t+h"
    case _ => "t+" + offset.toString() + "h"
  }
  
}

object FunctionType extends Enumeration {
  type FunctionType=Value
  val ADD, MULT, EXP, ABS, INVERT, SQRT, LN, SQUARE, SIN, COS  = Value
}

import FunctionType._

case class Function(val ftype: FunctionType, val arguments: List[Term]) extends Term {
  
  def arity: Int = arguments.length
  
  override def toString: String = ftype match {
    case ADD => "("+arguments.foldLeft("")((s,x)=>s+x.toString()+"+").dropRight(1)+")"
    case MULT => arguments.foldLeft("")((s,x)=>s+x.toString()+"*").dropRight(1)
    case EXP => "exp("+arguments(0).toString()+")"
    case ABS => "|"+arguments(0)+"|"
    case INVERT => "(1/("+arguments(0)+"))"
    case SQRT => "SQRT"+arguments(0)
    case LN => "ln("+arguments(0)+")"
    case SQUARE => "(" + arguments(0) + ")^2"
    case SIN => "sin(" + arguments(0) + ")"
    case COS => "cos(" + arguments(0) + ")"
    case _ => "illegal symbolic function"
  }; 
  
  
}

case class Equation(val left: Variable, val right: Term, val derivative: Boolean) {
  override def toString: String = derivative match {
    case true => "d/dt " + left.toString() + "=" + right.toString()
    case _ => left.toString() + "=" + right.toString()
  }
  
}
