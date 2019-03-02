package edu.cmu.cs.ls.keymaerax.tools

import edu.cmu.cs.ls.keymaerax.core._

import scala.collection.immutable.Map

/** (Hopefully) trustworthy and fast tool to help speed up IntervalArithmeticV2
  * 'proves' quantifier- and variable-free arithmetic formulas by evaluation with BigDecimals
  * @author Fabian Immler
  */
object BigDecimalQETool extends ToolBase("BigDecimal QE Tool") with QETool {
  initialized = true
  def eval(t: Term): BigDecimal = t match {
    case Plus(a, b) => eval(a) + eval(b)
    case Minus(a, b) => eval(a) - eval(b)
    case Neg(a) => -eval(a)
    case Times(a, b) => eval(a) * eval(b)
    case Power(a, Number(n)) if n.isValidInt => eval(a) pow n.toIntExact
    case Number(a) => a
  }
  def eval(fml: Formula): Boolean = fml match {
    case LessEqual(s, t) => eval(s) <= eval(t)
    case GreaterEqual(s, t) => eval(s) >= eval(t)
    case Less(s, t) => eval(s) < eval(t)
    case Greater(s, t) => eval(s) > eval(t)
    case Equal(s, t) => eval(s) == eval(t)
    case NotEqual(s, t) => eval(s) != eval(t)
    case And(f, g) => eval(f) && eval(g)
    case Or(f, g) => eval(f) || eval(g)
    case True => true
  }

  def qeEvidence(formula: Formula) = (if (eval(formula)) True else formula, new Evidence {
    val message = "evaluated BigDecimal numerics"
  })
  def init(config : Map[String,String]) = ()
  def restart() = ()
  def shutdown() = ()
}