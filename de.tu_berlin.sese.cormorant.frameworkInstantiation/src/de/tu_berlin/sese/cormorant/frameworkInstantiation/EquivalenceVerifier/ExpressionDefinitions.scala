package de.tu_berlin.sese.cormorant.frameworkInstantiation.EquivalenceVerifier

import de.tu_berlin.pes.memo.model.impl._

abstract class SyntacticExpression

abstract class Term extends SyntacticExpression {
  def eval: Option[Double]
  def rawString: String = toString
  def rawStringInfix: String = toString
  def mathematicaString: String = toString
}
case class UndefinedTerm() extends Term {
  override def toString: String = "Undefined Term"
  override def eval = None
}
abstract class AtomicTerm extends Term {
  override def eval = None
}

abstract class Variable extends AtomicTerm

abstract class InterfaceVariable(val id: Int) extends Variable

case class InputVariable(override val id: Int) extends InterfaceVariable(id) {
  override def toString: String = "i" //+ id
}
case class TimeVariable() extends Variable {
  override def toString: String = "t"
}

case class LaplaceVariable() extends Variable {
  override def toString: String = "s"
}

case class zVariable() extends Variable {
  override def toString: String = "z"
}

case class OutputVariable(override val id: Int) extends InterfaceVariable(id) {
  override def toString: String = "o" + id
}

abstract class StateVariable(val id: Int) extends Variable {
  override def toString: String = "y" + id
}

abstract class TimeContinuousStateVariable(override val id: Int) extends StateVariable(id)

abstract class TimeDiscreteStateVariable(override val id: Int) extends StateVariable(id)

case class IntegratorVariable(override val id: Int, val intialValue: Double) extends TimeContinuousStateVariable(id)

case class TransferFunctionVariable(override val id: Int, val numerator: List[Double], val denominator: List[Double]) extends TimeContinuousStateVariable(id) 


case class UnitDelayVariable(override val id: Int, initialValue: Double) extends TimeDiscreteStateVariable(id)
case class DelayVariable(override val id: Int, initialValue: Double, delay: Int) extends TimeDiscreteStateVariable(id)

abstract class Function extends Term

abstract class ArithmeticFunction(val arguments: List[Term]) extends Function {
  def rawString(fun: String): String = fun + "(" + arguments.foldLeft("")((s,x)=>s+ x.rawString +",").dropRight(1) + ")"
  def toString(fun: String, neutralElement: Int): String = {
    def isSymbolic(t: Term): Boolean = t.eval match {
      case None => true
      case Some(x) if x==x.floor=> false
      case Some(_) => true
    }
    def eraseOuterBrackets(s: String): String = s(1) match {
      case '(' => s.reverse(1) match {
        case ')' => eraseOuterBrackets(s.drop(1).dropRight(1))
        case _ => s
      }
      case _ => s
    }
    eraseOuterBrackets("(" + arguments.foldLeft("")((s, x) => isSymbolic(x) match {
        case true => s + x + fun
        case false => x.eval match {
           case Some(y) if y==y.floor && y.toInt==neutralElement => s+"" 
           case Some(y) if y.floor==y => s+y.toInt.toString+fun
           case Some(y) =>s+y + fun
           case None=> s+""
        }
     }) match {
       case "(" => neutralElement toString
       case s => s.dropRight(1) + ")"
       })
       
    
    
  }
  
  def rawStringInfix(fun: String): String =  {
    arguments.size match {
      case 1 => arguments.head.toString
      case _ =>  "(" + arguments.foldLeft("")((s, x)=> s+ x.rawStringInfix + fun).dropRight(1) + ")"
    }
  
  }
}

object FunctionLifter {
  def lift1(f: Double => Double): Option[Double]=>Option[Double] = x => x match {
    case None => None
    case Some (a) => Some(f(a))
  }
  
  def lift2(f: (Double, Double)=>Double): (Option[Double], Option[Double])=>Option[Double] = (x,y) => (x,y) match {
    case (None,_) => None
    case (_,None) => None
    case (Some(a),Some(b)) => Some(f(a,b))
    
  }
}

case class Constant(val c: Double) extends ArithmeticFunction(Nil) {
  def arity: Int = 0
  override def eval = Some(c)
  override def toString: String = c match {
     case y if y.floor==y => y.toInt.toString
     case y => y toString
   }
  
}
import FunctionLifter._

case class Add(override val arguments: List[Term]) extends ArithmeticFunction(arguments) {
  override def eval = arguments.size match {
    case 1=> arguments.head.eval
    case _=> arguments.foldLeft(Some(0.0):Option[Double])((x,y)=>(lift2((_+_)))(x,y.eval))
  }
  override def toString: String = eval match {
    case None => toString("+",0)
    case Some(x) if x.floor==x => x.toInt.toString
    case Some(x) => x toString
  }
  override def rawString: String = rawString("Add")
  override def rawStringInfix = rawStringInfix("+")
}

case class Mult(override val arguments: List[Term]) extends ArithmeticFunction(arguments) {
  override def eval = arguments.size match {
    case 1=> arguments.head.eval
    case _=> arguments.foldLeft(Some(1.0):Option[Double])((x,y)=>(lift2((_*_)))(x,y.eval))
  }
  override def toString: String = eval match {
    
    case None => arguments.filter(x=>x.eval==Some(0.0)).size match {
      case 0 => toString("*",1)
      case _ => Constant(0).toString()
    }
    case Some(x) if x.floor==x => x.toInt.toString
    case Some(x) => x toString
  }
  override def rawString: String = rawString("Mult")
  override def rawStringInfix = rawStringInfix("*")
}

case class MultInvert(override val arguments: List[Term]) extends ArithmeticFunction(arguments) {
  override def eval = lift2((_/_))(Some(1),arguments.head.eval)
  override def toString: String = "1/(" + arguments.head + ")"
}



case class Square(override val arguments: List[Term]) extends ArithmeticFunction(arguments) {
  override def eval = lift1(x=>x*x)(arguments.head.eval)
  override def toString: String = "(" + arguments + ")²"
  override def mathematicaString = "(" + arguments + ")^2"
}

case class Sqrt(override val arguments: List[Term]) extends ArithmeticFunction(arguments) {
  override def eval = lift1(Math.sqrt(_))(arguments.head.eval)
  override def toString: String = "√(" + arguments.head + ")"
  override def mathematicaString = "Sqrt[" + arguments.head + "]"
}

case class Exp(override val arguments: List[Term]) extends ArithmeticFunction(arguments) {
  override def eval = lift1(Math.exp(_))(arguments.head.eval)
  override def toString: String = "exp(" + arguments.head + ")"
  override def mathematicaString = "Exp[" + arguments.head + "]"
}

case class Ln(override val arguments: List[Term]) extends ArithmeticFunction(arguments) {
  override def eval = lift1(Math.log(_))(arguments.head.eval)
  override def toString: String = "ln(" + arguments.head + ")"
  override def mathematicaString = "Log[" + arguments.head + "]"
}

case class Abs(override val arguments: List[Term]) extends ArithmeticFunction(arguments) {
  override def eval = lift1(Math.abs(_))(arguments.head.eval)
  override def toString: String = "|" + arguments.head + "|"
  override def mathematicaString = "Abs[" + arguments.head + "]"

}

case class Sin(override val arguments: List[Term]) extends ArithmeticFunction(arguments) {
  override def eval = lift1(Math.sin(_))(arguments.head.eval)
  override def toString: String = "sin(" + arguments.head + ")"
  override def mathematicaString = "Sin[" + arguments.head + "]"
}

case class Cos(override val arguments: List[Term]) extends ArithmeticFunction(arguments) {
  override def eval = lift1(Math.cos(_))(arguments.head.eval)
  override def toString: String = "cos(" + arguments.head + ")"
  override def mathematicaString = "Cos[" + arguments.head + "]"
}

case class Polynomial(val coefficients: List[Double], val indeterminate: Variable) extends AtomicTerm {
  // coefficients ordered from high to low
  override def toString: String = {
    val maxArity = coefficients.size-1
    
    def printcoeffs(coeffs: List[Double], arity: Int, s: String): String = coeffs match {
      case Nil => s
      case (c::cs) if c==0.0 => printcoeffs(cs, arity-1,s)
      case (c::cs) if c==1.0 && arity==1 => printcoeffs(cs,arity-1,s+ indeterminate + "+")
      case (c::cs) if c==1.0 && arity==0 => s+c.toInt.toString
      case (c::cs) if c==1.0 => printcoeffs(cs,arity-1,s+ indeterminate + "^" + arity + "+")
      case (c::cs) if arity == 1 && c.floor==c => printcoeffs(cs,arity-1,s+ c.toInt.toString + indeterminate + "+")
      case (c::cs) if arity == 1 => printcoeffs(cs,arity-1,s+ c + indeterminate + "+")
      case (c::cs) if arity==0 && c.floor==c => s+c.toInt.toString
      case (c::cs) if arity==0 => s+c
      //case (c::Nil) => s+ c + indeterminate + "^" + arity
      case (c::cs) if c==c.floor => printcoeffs(cs, arity-1,s+ c.toInt.toString + indeterminate + "^" + arity + "+")
      case (c::cs)=> printcoeffs(cs, arity-1,s+ c + indeterminate + "^" + arity + "+")
    }
    val result = printcoeffs(coefficients,maxArity,"")
    //MeMoPlugin.out.println(coefficients + "/" + maxArity)
    if(result.last == '+') {
      result.dropRight(1)
    }else {
      result
    }
  }
  override def eval = None
}

case class RationalPolynomial(val numerator: Polynomial, val denominator: Polynomial) extends AtomicTerm {
  override def toString: String = "((" + numerator + ")" + "/" +  "(" + denominator + "))"
  override def eval = None
}

case class UndefinedFunction() extends Function {
  def arity: Int = 0

  override def toString: String = "Undefined function"
  override def eval = None
}



abstract class SemanticExpression


case class Matrix(val m: Array[Array[Term]]) extends SemanticExpression {
  override def toString: String = m.foldLeft("")((s,r)=>r.foldLeft(s)((s2,c)=>s2+c+",")+"\n")
  def mathematicaString: String = "{" + m.foldLeft("{")((s,r)=>r.foldLeft(s)((s2,c)=>s2+c+",").dropRight(1)+"},{").dropRight(2) + "}"
  def isZeroMatrix: Boolean = m.foldLeft(true)((b,r)=>r.foldLeft(b)((x,c)=>x&&c.eval==Some(0)))
  def dim: Int = m.size
  def norm: Double = m.foldLeft(0.0)((x,r)=>r.foldLeft(0.0)((y,c)=>y+Math.abs(c.eval match {
    case None => 0
    case Some(x) => x
  })) match {
  
    case y if x>y => x
    case y => y
  })
}

case class Vector(val v: Array[Term]) extends SemanticExpression {
  override def toString: String = v.foldLeft("")((s,r)=>s+r+"\n")
  def mathematicaString: String = "{" + v.foldLeft("")((s,r)=>s+r+",").dropRight(1) + "}"
  def enhancedMathematicaString: String = "{" + v.foldLeft("")((s,r)=>s+r+"[t],").dropRight(1) + "}"
  def derivedMathematicaString: String = "{" + v.foldLeft("")((s,r)=>s+r+"'[t],").dropRight(1) + "}"
}








