package edu.cmu.cs.ls.keymaerax.btactics

import edu.cmu.cs.ls.keymaerax.bellerophon._
import edu.cmu.cs.ls.keymaerax.core._
import edu.cmu.cs.ls.keymaerax.btactics.Idioms._
import Augmentors._
import edu.cmu.cs.ls.keymaerax.btactics.AxiomIndex.AxiomIndex
import edu.cmu.cs.ls.keymaerax.btactics.ExpressionTraversal.{ExpressionTraversalFunction, StopTraversal}
import edu.cmu.cs.ls.keymaerax.btactics.TactixLibrary._
import edu.cmu.cs.ls.keymaerax.btactics.TacticFactory._

import scala.language.postfixOps


/**
  * Created by bbohrer on 5/21/16.
  */
object Simplifier {
  type Simplification = (Term => Option[(Term, BelleExpr)])

  val timesZeroLeft:Simplification = {
    case Times(n: Number, t: Term) =>
      if (n.value == 0) {Some((n, QE))} else None
    case _ => None
  }

  val timesZeroRight:Simplification = {
    case Times(t: Term, n: Number) =>
      if (n.value == 0) {Some((n, QE))} else None
    case _ => None
  }

  val timesOneLeft:Simplification = {
    case Times(n: Number, t: Term) =>
      if (n.value == 1) {Some((t, QE))} else None
    case _ => None
  }

  val timesOneRight:Simplification = {
    case Times(t: Term, n: Number) =>
      if (n.value == 1) {Some((t, QE))} else None
    case _ => None
  }

  val plusZeroRight:Simplification = {
    case Plus(t: Term, n: Number) =>
      if (n.value == 0) {Some((t, QE))} else None
    case _ => None
  }

  val plusZeroLeft:Simplification = {
    case Plus(n: Number, t: Term) =>
      if (n.value == 0) {Some((t, QE))} else None
    case _ => None
  }

  val powZero:Simplification = {
    case Power(t:Term, n:Number) =>
      if (n.value == 0) {Some (Number(1), QE)} else None
    case _ => None
  }

  val powOne:Simplification = {
    case Power(t:Term, n:Number) =>
      if (n.value == 1) {Some (t, QE)} else None
    case _ => None
  }

  val divOne:Simplification = {
    case Divide(t:Term, n:Number) =>
      if (n.value == 1) {Some (t, QE)} else None
    case _ => None
  }

  val divCancel:Simplification = {
    case Divide(t1:Term, t2:Term) =>
      if (t1 == t2) {Some (Number(1), QE)} else None
    case _ => None
  }

  val minusCancel:Simplification = {
    case Minus(t1:Term, t2:Term) =>
      if(t1 == t2) {Some ((Number(0), QE))} else None
    case _ => None
  }

  val plusConst:Simplification = {
    case Plus(n: Number, m:Number) => Some ((Number(n.value + m.value), QE))
    case _ => None
  }

  val minusConst:Simplification = {
    case Minus(n: Number, m:Number) => Some ((Number(n.value - m.value), QE))
    case _ => None
  }

  val negConst:Simplification = {
    case Neg(n: Number) => Some ((Number(-n.value), QE))
    case _ => None
  }

  val timesConst:Simplification = {
    case Times(n: Number, m:Number) => Some ((Number(n.value * m.value), QE))
    case _ => None
  }

  val divConst:Simplification = {
    case Divide(n: Number, m:Number) if m.value == 0 => None
    case Divide(n: Number, m:Number) => Some ((Number(n.value / m.value), QE))
    case _ => None
  }

  val powConst:Simplification = {
    case Power(n: Number, m:Number) => Some ((Number(n.value.pow(m.value.toIntExact)), QE))
    case _ => None
  }

  val assocPlus:Simplification = {
    case Plus(Plus(a:Term, b:Term), c:Term) => print("\n\nputtin assoc in (" + a +"+" + b+") +"+c+"\n\n");Some(Plus(a, Plus(b,c)), QE)
    case _ => None
  }

  val assocTimes:Simplification = {
    case Times(Times(a:Term, b:Term), c:Term) => Some(Times(a, Times(b,c)), QE)
    case _ => None
  }

  val pushConstPlus:Simplification = {
    case Plus(n: Number, Plus(t1:Term, t2:Term)) =>
      if (!t1.isInstanceOf[Number] && !t1.isInstanceOf[Plus]) {print("\n\npushin down " + n + " vs " + t1+"\n\n"); Some(Plus(t1, Plus(n, t2)), QE)} else None
    case _ => None
  }

  val flipConstPlus:Simplification = {
    case Plus(n: Number, t1:Term) =>
      if (!t1.isInstanceOf[Number] && !t1.isInstanceOf[Plus]) {print("\n\nflippin "+ t1 + " wit " + n+"\n\n"); Some(Plus(t1, n), QE)} else None
    case _ => None
  }

  val flipConstTimes:Simplification = {
    case Times(n: Number, t1:Term) =>
      if (!t1.isInstanceOf[Number] && !t1.isInstanceOf[Times]) {Some(Times(t1, n), QE)} else None
    case _ => None
  }

  val pushConstTimes:Simplification = {
    case Times(n: Number, Times(t1:Term, t2:Term)) =>
      if (!t1.isInstanceOf[Number] && !t1.isInstanceOf[Times]) {Some(Times(t1, Times(n, t2)), QE)} else None
    case _ => None
  }

  /* By default, only use simplifications that obviously terminate */
  val defaultSimps: List[Simplification] = List(
    timesZeroLeft,
    timesZeroRight,
    timesOneLeft,
    timesOneRight,
    plusZeroRight,
    plusZeroLeft,
    plusConst,
    minusConst,
    minusCancel,
    negConst,
    timesConst,
    divConst,
    divOne,
    divCancel,
    powConst,
    powOne,
    powZero
  )

  /* When asked, normalize terms by flattening trees of * or + into lists and then
   * pushing all constants toward the end of the list. This enables a more powerful
   * form of constant folding / cancellation */
  val extendedSimps: List[Simplification] =
    defaultSimps ++ List (
      assocPlus,
      assocTimes,
      pushConstPlus,
      flipConstPlus,
      pushConstTimes,
      flipConstTimes
    )

  def trav(simps:List[Simplification]) = new ExpressionTraversalFunction {
    var result:Option[(PosInExpr, Term, BelleExpr)] = None
    override def preT(p: PosInExpr, e: Term): Either[Option[StopTraversal], Term] = {
      simps.find({case simp => simp(e).isDefined}) match {
        case Some(simp) =>
          simp(e) match {
            case Some((newTerm, belle)) =>
              result = Some((p, newTerm, belle))
              Left(Some(ExpressionTraversal.stop))
            case None => throw new ProverException("Bad pattern-match in Simplifier.scala")
          }
        case None => Left(None)
      }
    }
  }
  def simp(simps:List[Simplification], e:Formula):Option[(PosInExpr, Term, BelleExpr)] = {
    val t = trav(simps)
    ExpressionTraversal.traverse(t, e)
    t.result
  }

  def simp(simps:List[Simplification], e:Term):Option[(PosInExpr, Term, BelleExpr)] = {
    val t = trav(simps)
    ExpressionTraversal.traverse(t, e)
    t.result
  }

  def simp(simps:List[Simplification], e:Program):Option[(PosInExpr, Term, BelleExpr)] = {
    val t = trav(simps)
    ExpressionTraversal.traverse(t, e)
    t.result
  }

  def makeCE(fml:Formula, opt:Option[(PosInExpr, Term, BelleExpr)], where:Position):BelleExpr = {
    opt match {
      case Some((pos, t2, e)) =>
        val (ctx, t1:Term) = fml.at(pos)
        val eqProof = TactixLibrary.proveBy(Equal(t1, t2), e)
        HilbertCalculus.useAt(HilbertCalculus.CE(ctx)(eqProof), PosInExpr(0::Nil))(where)
      case None => TactixLibrary.nil
    }
  }

  def simpOnce(simps:List[Simplification]= defaultSimps):DependentPositionTactic = "simpOnce" by ((pos, sequent) => sequent.sub(pos) match {
    case Some(fml : Formula) => makeCE(fml, simp(simps, fml), pos)
    case None => TactixLibrary.nil
  })

  def simp(simps:List[Simplification] = defaultSimps):DependentPositionTactic = "simp" by ((pos, sequent) =>
    simpOnce(simps)(pos) *@ TheType ())
}

