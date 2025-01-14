/**
* Copyright (c) Carnegie Mellon University.
* See LICENSE.txt for the conditions of this license.
*/
package edu.cmu.cs.ls.keymaerax.btactics

import edu.cmu.cs.ls.keymaerax.bellerophon.{OnAll, SaturateTactic, UnificationMatch}
import edu.cmu.cs.ls.keymaerax.btactics.TactixLibrary._
import edu.cmu.cs.ls.keymaerax.btactics.DebuggingTactics.{print, printIndexed}
import edu.cmu.cs.ls.keymaerax.btactics.ArithmeticSimplification._
import edu.cmu.cs.ls.keymaerax.btactics.arithmetic.speculative.ArithmeticSpeculativeSimplification._
import edu.cmu.cs.ls.keymaerax.core._
import edu.cmu.cs.ls.keymaerax.parser.KeYmaeraXArchiveParser
import edu.cmu.cs.ls.keymaerax.parser.StringConverter._
import edu.cmu.cs.ls.keymaerax.tags.SlowTest
import testHelper.KeYmaeraXTestTags.TodoTest
import testHelper.ParserFactory._

import scala.collection.immutable._
import scala.language.postfixOps

/**
 * Tutorial test cases.
 *
 * @author Stefan Mitsch
 */
@SlowTest
class StttTutorial extends TacticTestBase {

  "Example 1" should "be provable" in withQE { _ => withDatabase { db =>
    val modelContent = KeYmaeraXArchiveParser.getEntry("STTT Tutorial Example 1", io.Source.fromInputStream(
      getClass.getResourceAsStream("/examples/tutorials/sttt/sttt.kyx")).mkString).get.fileContent
    val tactic = implyR('_) & andL('_) & dC("v>=0".asFormula)(1) <(
      /* use */ dW(1) & prop,
      /* show */ dI()(1)
      )
    db.proveBy(modelContent, tactic) shouldBe 'proved
  }}

  it should "be provable with diffSolve" in withQE { _ => withDatabase { db =>
    val modelContent = KeYmaeraXArchiveParser.getEntry("STTT Tutorial Example 1", io.Source.fromInputStream(
      getClass.getResourceAsStream("/examples/tutorials/sttt/sttt.kyx")).mkString).get.fileContent
    val tactic = implyR('_) & andL('_) & solve(1) & QE
    db.proveBy(modelContent, tactic) shouldBe 'proved
  }}

  it should "be provable with master" in withQE { _ => withDatabase { db =>
    val modelContent = KeYmaeraXArchiveParser.getEntry("STTT Tutorial Example 1", io.Source.fromInputStream(
      getClass.getResourceAsStream("/examples/tutorials/sttt/sttt.kyx")).mkString).get.fileContent
    db.proveBy(modelContent, master()) shouldBe 'proved
  }}

  it should "be provable with diffInvariant" in withQE { _ => withDatabase { db =>
    val modelContent = KeYmaeraXArchiveParser.getEntry("STTT Tutorial Example 1", io.Source.fromInputStream(
      getClass.getResourceAsStream("/examples/tutorials/sttt/sttt.kyx")).mkString).get.fileContent
    val tactic = implyR('_) & andL('_) & diffInvariant("v>=0".asFormula)('R) & dW('R) & prop
    db.proveBy(modelContent, tactic) shouldBe 'proved
  }}

  "Example 1a" should "be provable" in withQE { _ => withDatabase { db =>
    val modelContent = io.Source.fromInputStream(getClass.getResourceAsStream("/examples/tutorials/sttt/example1a.kyx")).mkString
    val tactic = implyR('_) & SaturateTactic(andL('_)) & dC("v>=0".asFormula)(1) & Idioms.<(
      dC("x>=old(x)".asFormula)(1) & Idioms.<(
        dW(1) & exhaustiveEqL2R('L, "x0=x_0".asFormula) & prop,
        dI()(1)
      ),
      dI()(1)
    )

    db.proveBy(modelContent, tactic) shouldBe 'proved
  }}

  it should "be provable with multi-arg invariant" in withQE { _ => withDatabase { _ =>
    val modelContent = io.Source.fromInputStream(getClass.getResourceAsStream("/examples/tutorials/sttt/example1a.kyx")).mkString
    val tactic = implyR('_) & SaturateTactic(andL('_)) & diffInvariant("v>=0".asFormula, "x>=old(x)".asFormula)(1) &
      dW(1) & exhaustiveEqL2R('L, "x0=x_0".asFormula) & prop

    //@todo multi-argument diffInvariant not yet supported by TacticExtraction/BelleParser
//    db.proveBy(modelContent, tactic) shouldBe 'proved
    proveBy(KeYmaeraXArchiveParser.parseAsProblemOrFormula(modelContent), tactic) shouldBe 'proved
  }}

  "Example 2" should "be provable with master and custom loop invariant" in withQE { _ => withDatabase { db =>
    val modelContent = KeYmaeraXArchiveParser.getEntry("STTT Tutorial Example 2", io.Source.fromInputStream(
      getClass.getResourceAsStream("/examples/tutorials/sttt/sttt.kyx")).mkString).get.fileContent
    val Imply(_, Box(loop, _)) = KeYmaeraXArchiveParser.parseAsProblemOrFormula(modelContent)
    db.proveBy(modelContent, master(new ConfigurableGenerator(Map((loop, ("v>=0".asFormula, None)::Nil))))) shouldBe 'proved
  }}

  it should "be provable with master and loop invariant from file" in withQE { _ => withDatabase { db =>
    val modelContent = KeYmaeraXArchiveParser.getEntry("STTT Tutorial Example 2", io.Source.fromInputStream(
      getClass.getResourceAsStream("/examples/tutorials/sttt/sttt.kyx")).mkString).get.fileContent
    db.proveBy(modelContent, master()) shouldBe 'proved
  }}

  it should "be provable with abstract loop invariant" taggedAs TodoTest ignore withMathematica { _ => withDatabase { db =>
    val modelContent = KeYmaeraXArchiveParser.getEntry("STTT Tutorial Example 2", io.Source.fromInputStream(
      getClass.getResourceAsStream("/examples/tutorials/sttt/sttt.kyx")).mkString).get.fileContent

    val tactic = implyR('_) & SaturateTactic(andL('_)) & loop("J(v)".asFormula)('R) <(
      skip,
      skip,
      chase('R) & prop & OnAll(solve('R))
      ) &
      US(UnificationMatch("J(v)".asFormula, "v>=0".asFormula).usubst) &
      OnAll(QE)

    //@todo Rewrite the US tactic in terms of the Bellerophon language, not arbitrary scala code.
    db.proveBy(modelContent, tactic) shouldBe 'proved
  }}

  "Example 3a" should "be provable with master and loop invariant from file" in withQE { _ => withDatabase { db =>
    // // needs evolution domain at time 0
    val modelContent = KeYmaeraXArchiveParser.getEntry("STTT Tutorial Example 3a", io.Source.fromInputStream(
      getClass.getResourceAsStream("/examples/tutorials/sttt/sttt.kyx")).mkString).get.fileContent
    db.proveBy(modelContent, master()) shouldBe 'proved
  }}

  "Example3b" should "find correct safety condition" in withMathematica { _ => withDatabase { db =>
    val modelContent = KeYmaeraXArchiveParser.getEntry("STTT Tutorial Example 3b", io.Source.fromInputStream(
      getClass.getResourceAsStream("/examples/tutorials/sttt/sttt.kyx")).mkString).get.fileContent

    val tactic = implyR('_) & SaturateTactic(andL('_)) & chase('R) & normalize & OnAll(solve('R))
    val intermediate = db.proveBy(modelContent, tactic)
    intermediate.subgoals should have size 3
    intermediate.subgoals(0) shouldBe Sequent(
      IndexedSeq("v>=0".asFormula, "A()>0".asFormula, "B()>0".asFormula, "true".asFormula, "x<=S()".asFormula, "true".asFormula),
      IndexedSeq("\\forall t_ (t_>=0 -> \\forall s_ (0<=s_ & s_<=t_ -> A()*s_+v>=0) -> A()*(t_^2/2)+v*t_+x<=S())".asFormula))
    intermediate.subgoals(1) shouldBe Sequent(
      IndexedSeq("v>=0".asFormula, "A()>0".asFormula, "B()>0".asFormula, "true".asFormula, "x<=S()".asFormula, "v=0".asFormula),
      IndexedSeq("\\forall t_ (t_>=0 -> \\forall s_ (0<=s_ & s_<=t_ -> v>=0) -> v*t_+x<=S())".asFormula))
    intermediate.subgoals(2) shouldBe Sequent(
      IndexedSeq("v>=0".asFormula, "A()>0".asFormula, "B()>0".asFormula, "true".asFormula, "x<=S()".asFormula),
      IndexedSeq("\\forall t_ (t_>=0 -> \\forall s_ (0<=s_ & s_<=t_ -> (-B())*s_+v>=0) -> (-B())*(t_^2/2)+v*t_+x<=S())".asFormula))

    val brake = proveBy(intermediate.subgoals(2), TactixLibrary.partialQE)
    brake.subgoals should have size 1
    brake.subgoals.head shouldBe Sequent(
      IndexedSeq(),
      // here is our evolution domain constraint ------------------------------------------------v
      IndexedSeq("(x < S()&(v<=0|v>0&((B()<=0|(0 < B()&B() < -1*v^2*(2*x+-2*S())^-1)&A()<=0)|B()>=-1*v^2*(2*x+-2*S())^-1))|x=S()&(v<=0|v>0&(B()<=0|B()>0&A()<=0)))|x>S()".asFormula))
  }}

  it should "stop at correct spot when tactic is parsed from file" in withQE { _ => withDatabase { db =>
    val entry = KeYmaeraXArchiveParser.getEntry("STTT Tutorial Example 3b", io.Source.fromInputStream(
      getClass.getResourceAsStream("/examples/tutorials/sttt/sttt.kyx")).mkString).get
    val modelContent = entry.fileContent
    val tactic = entry.tactics.head._3
    val intermediate = db.proveBy(modelContent, tactic)
    intermediate.subgoals should have size 3
    intermediate.subgoals(0) shouldBe Sequent(
      IndexedSeq("v>=0".asFormula, "A()>0".asFormula, "B()>0".asFormula, "true".asFormula, "x<=S()".asFormula, "true".asFormula),
      IndexedSeq("\\forall t_ (t_>=0 -> \\forall s_ (0<=s_ & s_<=t_ -> A()*s_+v>=0) -> A()*(t_^2/2)+v*t_+x<=S())".asFormula))
    intermediate.subgoals(1) shouldBe Sequent(
      IndexedSeq("v>=0".asFormula, "A()>0".asFormula, "B()>0".asFormula, "true".asFormula, "x<=S()".asFormula, "v=0".asFormula),
      IndexedSeq("\\forall t_ (t_>=0 -> \\forall s_ (0<=s_ & s_<=t_ -> v>=0) -> v*t_+x<=S())".asFormula))
    intermediate.subgoals(2) shouldBe Sequent(
      IndexedSeq("v>=0".asFormula, "A()>0".asFormula, "B()>0".asFormula, "true".asFormula, "x<=S()".asFormula),
      IndexedSeq("\\forall t_ (t_>=0 -> \\forall s_ (0<=s_ & s_<=t_ -> (-B())*s_+v>=0) -> (-B())*(t_^2/2)+v*t_+x<=S())".asFormula))
  }}

  "Example 4a" should "be provable with master and loop invariant from file" in withQE { _ => withDatabase { db =>
    val modelContent = KeYmaeraXArchiveParser.getEntry("STTT Tutorial Example 4a", io.Source.fromInputStream(
      getClass.getResourceAsStream("/examples/tutorials/sttt/sttt.kyx")).mkString).get.fileContent
    db.proveBy(modelContent, master()) shouldBe 'proved
  }}

  "Example 4b" should "be provable with master and loop invariant from file" in withQE { _ => withDatabase { db =>
    val modelContent = KeYmaeraXArchiveParser.getEntry("STTT Tutorial Example 4b", io.Source.fromInputStream(
      getClass.getResourceAsStream("/examples/tutorials/sttt/sttt.kyx")).mkString).get.fileContent
    db.proveBy(modelContent, master()) shouldBe 'proved
  }}

  "Example 4c" should "be provable with master and loop invariant from file" in withQE { _ => withDatabase { db =>
    // needs evolution domain at time 0
    val modelContent = KeYmaeraXArchiveParser.getEntry("STTT Tutorial Example 4c", io.Source.fromInputStream(
      getClass.getResourceAsStream("/examples/tutorials/sttt/sttt.kyx")).mkString).get.fileContent
    db.proveBy(modelContent, master()) shouldBe 'proved
  }}

  "Example 5 with simple control" should "be provable" in withQE { _ => withDatabase { db =>
    val modelContent = io.Source.fromInputStream(getClass.getResourceAsStream("/examples/tutorials/sttt/example5_simplectrl.kyx")).mkString

    val plant = print("plant") & composeb('R) & assignb('R) & solveEnd('R)

    val tactic = implyR('R) & SaturateTactic(andL('L)) &
      loop("v >= 0 & x+v^2/(2*B()) <= S()".asFormula)('R) <(
      print("Base Case") & andR('R) & OnAll(closeId),
      print("Use Case") & QE,
      print("Step") & andL('L) & composeb('R) & assignb('R) & plant & QE
    )

    db.proveBy(modelContent, tactic) shouldBe 'proved
  }}

  it should "be provable automatically" in withQE { _ => withDatabase { db =>
    val modelContent = io.Source.fromInputStream(getClass.getResourceAsStream("/examples/tutorials/sttt/example5_simplectrl.kyx")).mkString
    db.proveBy(modelContent, master()) shouldBe 'proved
  }}

  "Example 5" should "be provable automatically" in withQE { _ => withDatabase { db =>
    val modelContent = KeYmaeraXArchiveParser.getEntry("STTT Tutorial Example 5", io.Source.fromInputStream(
      getClass.getResourceAsStream("/examples/tutorials/sttt/sttt.kyx")).mkString).get.fileContent
    db.proveBy(modelContent, master()) shouldBe 'proved
  }}

  it should "be provable with chase etc" in withQE { _ => withDatabase { db =>
    val modelContent = KeYmaeraXArchiveParser.getEntry("STTT Tutorial Example 5", io.Source.fromInputStream(
      getClass.getResourceAsStream("/examples/tutorials/sttt/sttt.kyx")).mkString).get.fileContent

    val tactic = implyR('R) & SaturateTactic(andL('L)) &
      loop("v >= 0 & x+v^2/(2*B()) <= S()".asFormula)('R) <(
        printIndexed("Base case") & andR('R) & OnAll(closeId),
        printIndexed("Use case") & QE,
        printIndexed("Step") & chase('R) & printIndexed("After chase") & normalize & printIndexed("Normalized") &
          OnAll(solveEnd('R)) & printIndexed("After diffSolve") & OnAll(QE)
        )

    db.proveBy(modelContent, tactic) shouldBe 'proved
  }}

  it should "be provable with abstract loop invariant" ignore withMathematica { _ =>
    val s = KeYmaeraXArchiveParser.getEntry("STTT Tutorial Example 5", io.Source.fromInputStream(
      getClass.getResourceAsStream("/examples/tutorials/sttt/sttt.kyx")).mkString).get.model.asInstanceOf[Formula]


    val tactic = implyR('R) & SaturateTactic(andL('L)) &
      loop("J(v,x,B,S)".asFormula)('R) <(
        skip,
        skip,
        chase('R) & normalize & OnAll(solve('R))
        ) &
      US(UnificationMatch("J(v,x,B,S)".asFormula, "v >= 0 & x+v^2/(2*B) <= S".asFormula).usubst) &
      OnAll(QE)

    proveBy(s, tactic) shouldBe 'proved
  }

  "Example 6" should "be provable automatically" in withQE { _ => withDatabase { db =>
    val modelContent = KeYmaeraXArchiveParser.getEntry("STTT Tutorial Example 6", io.Source.fromInputStream(
      getClass.getResourceAsStream("/examples/tutorials/sttt/sttt.kyx")).mkString).get.fileContent
    db.proveBy(modelContent, master()) shouldBe 'proved
  }}

  "Example 7" should "be provable automatically" in withQE { _ => withDatabase { db =>
    val modelContent = KeYmaeraXArchiveParser.getEntry("STTT Tutorial Example 7", io.Source.fromInputStream(
      getClass.getResourceAsStream("/examples/tutorials/sttt/sttt.kyx")).mkString).get.fileContent
    db.proveBy(modelContent, master()) shouldBe 'proved
  }}

  "Example 8" should "be provable automatically with Mathematica" ignore withMathematica { _ =>
    // x' <= a*d
    val s = parseToSequent(getClass.getResourceAsStream("/examples/tutorials/sttt/example8.kyx"))
    proveBy(s, master()) shouldBe 'proved
  }

  "Example 9a" should "be provable" in withQE { _ => withDatabase { db =>
    val modelContent = KeYmaeraXArchiveParser.getEntry("STTT Tutorial Example 9a", io.Source.fromInputStream(
      getClass.getResourceAsStream("/examples/tutorials/sttt/sttt.kyx")).mkString).get.fileContent
    val tactic = implyR('R) & SaturateTactic(andL('L)) & dI()('R)
    db.proveBy(modelContent, tactic) shouldBe 'proved
  }}

  "Example 9b" should "be provable" in withQE { _ => withDatabase { db =>
    val modelContent = KeYmaeraXArchiveParser.getEntry("STTT Tutorial Example 9b", io.Source.fromInputStream(
      getClass.getResourceAsStream("/examples/tutorials/sttt/sttt.kyx")).mkString).get.fileContent

    val ode =
      // xr = (xm+S)/2
      diffInvariant("xm<=x".asFormula)('R) &
      diffInvariant("5/4*(x-(xm+S())/2)^2 + (x-(xm+S())/2)*v/2 + v^2/4 < ((S()-xm)/2)^2".asFormula)('R) &
      dW('R)

    val tactic = implyR('R) & SaturateTactic(andL('L)) &
      loop("v >= 0 & xm <= x & xr = (xm + S())/2 & 5/4*(x-xr)^2 + (x-xr)*v/2 + v^2/4 < ((S() - xm)/2)^2".asFormula)('R) <(
        print("Base case") & SaturateTactic(andR('R) <(closeId, skip)) & closeId,
        print("Use case") & QE,
        print("Step") & SaturateTactic(andL('L)) & chase('R) & andR('R) <(
          allR('R) & SaturateTactic(implyR('R)) & ode & implyR('R) & SaturateTactic(andL('L)) & QE,
          implyR('R) & ode & printIndexed("Bar") & QE
        )
      )

    db.proveBy(modelContent, tactic) shouldBe 'proved
  }}

  "Example 10" should "be provable" in withQE { _ => withDatabase { db =>
    val modelContent = KeYmaeraXArchiveParser.getEntry("STTT Tutorial Example 10", io.Source.fromInputStream(
      getClass.getResourceAsStream("/examples/tutorials/sttt/sttt.kyx")).mkString).get.fileContent

    def ode(a: String) =
      dC("c>=0".asFormula)(1) & Idioms.<(
        dC("dx^2+dy^2=1".asFormula)(1) & Idioms.<(
          dC(s"v=old(v)+$a*c".asFormula)(1) & Idioms.<(
            dC(s"-c*(v-$a/2*c) <= y - old(y) & y - old(y) <= c*(v-$a/2*c)".asFormula)(1) & Idioms.<(
              skip,
              dI()(1)
            ),
            dI()(1)),
          dI()(1)),
        dI()(1)
        ) & SaturateTactic(andL('L)) & dW('R)

    def hideQE = SaturateTactic(hideL('Llike, "dx_0^2+dy_0^2=1".asFormula)) & hideL('L, "c<=ep()".asFormula) & hideL('L, "r!=0".asFormula)

    val tactic = implyR('R) & SaturateTactic(andL('L)) &
      loop("v >= 0 & dx^2+dy^2 = 1 & r != 0 & abs(y-ly()) + v^2/(2*b()) < lw()".asFormula)('R) <(
        print("Base case") & speculativeQE,
        print("Use case") & speculativeQE,
        print("Step") & chase('R) & normalize & printIndexed("Normalized") <(
          printIndexed("Acc") & hideL(-15, "abs(y-ly())+v^2/(2*b()) < lw()".asFormula) & ode("a") &
            SaturateTactic(alphaRule) &
            printIndexed("Before replaceTransform") & replaceTransform("ep()".asTerm, "c".asTerm)(-6, "abs(y_0-ly())+v_0^2/(2*b())+(A()/b()+1)*(A()/2*ep()^2+ep()*v_0) < lw()".asFormula) &
            prop & OnAll(speculativeQE),
          printIndexed("Stop") & ode("0") & prop & OnAll(hideQE & speculativeQE),
          printIndexed("Brake") & ode("a") & prop & OnAll(hideQE & speculativeQE)
          )
        )

    db.proveBy(modelContent, tactic) shouldBe 'proved
  }}

  it should "be provable with multi-arg diff. invariant" in withQE { _ => withDatabase { _ =>
    val modelContent = KeYmaeraXArchiveParser.getEntry("STTT Tutorial Example 10", io.Source.fromInputStream(
      getClass.getResourceAsStream("/examples/tutorials/sttt/sttt.kyx")).mkString).get.fileContent

    def ode(a: String) = diffInvariant("c>=0".asFormula, "dx^2+dy^2=1".asFormula, s"v=old(v)+$a*c".asFormula,
      s"-c*(v-$a/2*c) <= y - old(y) & y - old(y) <= c*(v-$a/2*c)".asFormula)('R) & dW('R)

    def hideQE = SaturateTactic(hideL('Llike, "dx_0^2+dy_0^2=1".asFormula)) & hideL('L, "c<=ep()".asFormula) & hideL('L, "r!=0".asFormula)

    val tactic = implyR('R) & SaturateTactic(andL('L)) &
      loop("v >= 0 & dx^2+dy^2 = 1 & r != 0 & abs(y-ly()) + v^2/(2*b()) < lw()".asFormula)('R) <(
        print("Base case") & speculativeQE,
        print("Use case") & speculativeQE,
        print("Step") & chase('R) & normalize & printIndexed("Normalized") <(
          printIndexed("Acc") & hideL(-15, "abs(y-ly())+v^2/(2*b()) < lw()".asFormula) & ode("a") &
            SaturateTactic(alphaRule) &
            printIndexed("Before replaceTransform") & replaceTransform("ep()".asTerm, "c".asTerm)(-6, "abs(y_0-ly())+v_0^2/(2*b())+(A()/b()+1)*(A()/2*ep()^2+ep()*v_0) < lw()".asFormula) &
            prop & OnAll(speculativeQE),
          printIndexed("Stop") & ode("0") & prop & OnAll(hideQE & speculativeQE),
          printIndexed("Brake") & ode("a") & prop & OnAll(hideQE & speculativeQE)
          )
        )

    //@todo multi-argument diffInvariant not yet supported by TacticExtraction/BelleParser
    //db.proveBy(modelContent, tactic) shouldBe 'proved
    proveBy(KeYmaeraXArchiveParser.parseAsProblemOrFormula(modelContent), tactic) shouldBe 'proved
  }}

}
