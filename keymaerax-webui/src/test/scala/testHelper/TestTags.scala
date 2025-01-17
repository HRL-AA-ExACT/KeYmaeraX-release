package testHelper

import org.scalatest.Tag

/**
 * Test categories.
 * @todo Figure out a way to specify timeouts for certain tags.
 * @author Nathan Fulton
 * Created by nfulton on 9/11/15.
 */
object KeYmaeraXTestTags {

  /**
   * A small core of very fasts tests that could be run before each check-in or even every compile.
   * Each test should run in under :30 seconds
   * Set runs in a minute or two.
   */
  object CheckinTest extends org.scalatest.Tag("edu.cmu.cs.ls.keymaerax.tags.CheckinTest")

  /**
   * A test that summarizes all of the tests occuring in a package or file.
   * These tests definitely get run before *pushing* any code to GitHub in a normal development process.
   * Set runs in under 15 minutes.
   */
  object SummaryTest extends org.scalatest.Tag("edu.cmu.cs.ls.keymaerax.tags.SummaryTest")

  /**
   * Average set of tests that run usually.
   */
  object UsualTest extends org.scalatest.Tag("edu.cmu.cs.ls.keymaerax.tags.UsualTest")

  /**
   * Usually tests that call QE or test a lot of cases.
   * Set runs for unbounded amount of time.
   */
  object SlowTest extends org.scalatest.Tag("edu.cmu.cs.ls.keymaerax.tags.SlowTest")

  /**
    * Usually tests that call QE or test a lot of cases.
    * Set runs for unbounded amount of time.
    */
  object ExtremeTest extends org.scalatest.Tag("edu.cmu.cs.ls.keymaerax.tags.ExtremeTest")

  /**
   * Case Study tests are (typically long-running) tests that run an entire case study, sometimes
   * step-by-step.
   * Set runs for unbounded amount of time.
   */
  object CaseStudyTest extends org.scalatest.Tag("edu.cmu.cs.ls.keymaerax.tags.CaseStudyTest")

  /**
   * A user-interface test.
   */
  object UITest extends org.scalatest.Tag("edu.cmu.cs.ls.keymaerax.tags.UITest")

  /**
   * Tests the persistence layer (DB, caches, etc.)
   */
  object PersistenceTest extends org.scalatest.Tag("edu.cmu.cs.ls.keymaerax.tags.UITest")

  /**
   * Tests that only make sense to run pre-deployment.
   */
  object DeploymentTest extends org.scalatest.Tag("edu.cmu.cs.ls.keymaerax.tags.Deployment")

  /** Uniform substitution tests */
  object USubstTest extends Tag("edu.cmu.cs.ls.keymaerax.tags.USubstTest")

  /** Tests that are overly optimistic about the capabilities and might want to be ignored. */
  object OptimisticTest extends Tag("edu.cmu.cs.ls.keymaerax.tags.OptimisticTest")

  /** Tests that are obsolete, e.g. from old tactic framework. */
  object ObsoleteTest extends Tag("edu.cmu.cs.ls.keymaerax.tags.ObsoleteTest")

  /** Tests that should be ignored in an automated build via Jenkins. */
  object IgnoreInBuildTest extends Tag("edu.cmu.cs.ls.keymaerax.tags.IgnoreInBuildTest")

  /** Tests codifying todo's. These may be ignored and should be un-ignored occasionally. */
  object TodoTest extends Tag("edu.cmu.cs.ls.keymaerax.tags.TodoTest")

  /** Tests that are added for coverage analysis and not critically used. */
  object CoverageTest extends Tag("edu.cmu.cs.ls.keymaerax.tags.CoverageTest")

  /**
    * An advocatus diavoli test that's sceptical about soundness.
    */
  object AdvocatusTest extends org.scalatest.Tag("edu.cmu.cs.ls.keymaerax.tags.AdvocatusTest")
}
