/*
 * Copyright (c) Carnegie Mellon University.
 * See LICENSE.txt for the conditions of this license.
 */

package edu.cmu.cs.ls.keymaerax.btactics.helpers

import java.io.FileWriter

import edu.cmu.cs.ls.keymaerax.Configuration
import edu.cmu.cs.ls.keymaerax.bellerophon.{BelleExpr, BuiltInTactic}
import edu.cmu.cs.ls.keymaerax.btactics.TactixLibrary._
import edu.cmu.cs.ls.keymaerax.btactics.Augmentors._
import edu.cmu.cs.ls.keymaerax.core._
import edu.cmu.cs.ls.keymaerax.parser.KeYmaeraXPrettyPrinter
import edu.cmu.cs.ls.keymaerax.parser.StringConverter._
import edu.cmu.cs.ls.keymaerax.pt.ProvableSig
import edu.cmu.cs.ls.keymaerax.tools.DefaultSMTConverter
import org.apache.logging.log4j.scala.Logging

import scala.collection.mutable.ListBuffer
import scala.io.Source

/**
  * Helper tools to log QE calls
  */
object QELogger extends Logging {

  /* A simple measure of complexity on a first-order sequent
   - Used in QE logger to skip over trivial goals like 0 + 0 = 0
  */
  def measure(t:Term): Int = {
    t match {
      case _: Number => 0
      case _: AtomicTerm => 1
      case f: FuncOf => 1 + measure(f.child)
      case u: UnaryCompositeTerm => 1 + measure(u.child)
      case b: BinaryCompositeTerm => 1 + measure(b.left)+measure(b.right)
    }
  }

  def measure(f:Formula): Int = {
    f match {
      case c: ComparisonFormula => 1 + measure(c.left)+measure(c.right)
      case _: AtomicFormula => 1
      case u: UnaryCompositeFormula => 1 + measure(u.child)
      case b: BinaryCompositeFormula => 1 + measure(b.left) + measure(b.right)
      case q: Quantified => 1 + measure(q.child)
      // Not allowed in QE calls
      // case m: Modal => 1 + measure(m.program) + measure(m.child)
      // case p: PredOf => 1 + measure(p.child)
      // case p: PredicationalOf => 1 + measure(p.child)
    }
  }

  def measure(s:Sequent): Int = s.succ.map(measure).sum + s.ante.map(measure).sum

  /**
    *   A simple QE logging facility
    *   Each sequent is recorded together with the underlying conclusion of the provable that it is linked to
    *   # separates the conclusion and the actual sequent, @ separates lines
    *
    *   @ name_1 # Concl_1 # Seq_1
    *   @ name_2 # Concl_2 # Seq_2
    *   @ name_3 # Concl_3 # Seq_3
    *   ...
    *
    *   Sequents with the same name are grouped together when the file is re-parsed
    */
  private val defaultPath: String = Configuration.path(Configuration.Keys.QE_LOG_PATH)

  private def file(filename: String): java.io.File =
    if (new java.io.File(filename).isDirectory) {
      new java.io.File(filename, "qe.txt")
    } else {
      new java.io.File(filename)
    }

  def clearLog(filename: String = defaultPath): Unit = {
    try {
      val f = file(filename)
      f.delete()
    } catch {
      case ex: Exception =>
        logger.error("Failed to delete log", ex)
    }
  }

  def logSequent(pr: Sequent, s: Sequent, name: String, filename: String = defaultPath): Unit = {
    try {
      val f = file(filename)
      f.mkdirs()
      val namestr = "@"+name+"#"+pr.toString+"#"+s.toString+"\n"
      val fw = new FileWriter(f)
      fw.append(namestr)
      fw.close()
    } catch {
      case ex: Exception =>
        logger.error("Failed to record sequent", ex)
    }
  }

  // Must be of the form Seq # Seq # Seq
  def parseStr(s: String): Option[(String, Sequent, Sequent)] = {
    val ss = s.split("#")
    if (ss.length != 3) None
    else try {
      val pr = ss(1).asSequent
      val seq = ss(2).asSequent
      Some(ss(0),pr,seq)
    } catch {
      case ex: Exception =>
        logger.error("Failed to parse " + s, ex)
        None
    }
  }

  def parseStr2(s: String): Option[(String, Sequent)] = {
    val ss = s.split("#")
    if (ss.length != 3) None
    else try {
      Some(ss(0),ss(2).asSequent)
    } catch {
      case ex: Exception =>
        logger.error("Failed to parse " + s, ex)
        None
    }
  }

  /** Parses the log into a map (reads large log files into memory!). */
  def parseLog(filename: String = defaultPath): Map[String, List[(Sequent, Sequent)]] = {
    class LogCollector extends (((String, Sequent, Sequent)) => Unit) {
      val entries: ListBuffer[(String, Sequent, Sequent)] = new ListBuffer[(String, Sequent, Sequent)]()
      def apply(e: (String, Sequent, Sequent)): Unit = entries += e
    }

    val c = new LogCollector()
    processLog(parseStr, c, filename)
    c.entries.toList.groupBy(_._1).mapValues(_.map(p => (p._2,p._3)))
  }

  /** Process log one entry at a time. */
  def processLog[T](parseEntry: String => Option[T], processEntry: T => Unit, filename: String = defaultPath): Unit = {
    // Upon reading @, save the previous sequent and provable
    var curString = ""

    val src = Source.fromFile(file(filename).toURI)
    try {
      for (line <- src.getLines()) {
        if (line.startsWith("@")) {
          parseEntry(curString) match {
            case None => ()
            case Some(p) => processEntry(p)
          }
          curString = line.substring(1)
        } else curString += line
      }
      parseEntry(curString) match {
        case None => ()
        case Some(p) => processEntry(p)
      }
    } catch {
      case ex: Exception => logger.error("File I/O exception", ex)
    } finally {
      src.close()
    }
  }

  type LogConfig = (Int,String)
  private val defaultConf = (0, "")

  def measureRecordQE(lb: LogConfig = defaultConf): BuiltInTactic = new BuiltInTactic("logQE") {
      override def result(pr: ProvableSig): ProvableSig = {
        if (pr.subgoals.length == 1) {
          val sequent = pr.subgoals(0)
          if (measure(sequent) > lb._1)
            logSequent(pr.conclusion,sequent, lb._2)
        }
        pr
      }
    }

  private var logTactic = nil

  def getLogTactic: BelleExpr = logTactic

  //This bakes the recorder into the QE tactic, so it will record every single QE call, including internal ones made by
  //e.g. the ODE solver
  def enableLogging(loglevel: LogConfig = defaultConf ): Unit = {
    logTactic = measureRecordQE(loglevel)
  }

  def disableLogging(): Unit = {
    logTactic = nil
  }

  /** Exports the entries of `logPath` as separate files in SMT-Lib format to `exportPath` */
  def exportSmtLibFormat(logPath: String, exportPath: String): Unit = {
    val filePath = if (exportPath.contains("${entryname}")) exportPath else exportPath + "${entryname}"
    def export(e: (String, Sequent)): Unit = {
      print("Exporting " + e._1 + "...")
      try {
        exportSmtLibFormat(e._2.toFormula, filePath.replace("${entryname}", e._1.replaceAllLiterally(" ", "_")))
        println("done")
      } catch {
        case ex: Throwable => println("failed: " + ex.getMessage)
      }
    }
    processLog(parseStr2, export, logPath)
  }

  /** Exports the formula `fml` in SMT-Lib format to `exportFile`. */
  def exportSmtLibFormat(fml: Formula, exportFile: String): Unit = {
    val f = file(exportFile)
    val fw = new FileWriter(f)
    fw.append(DefaultSMTConverter(fml))
    fw.close()
  }

  def main(args: Array[String]): Unit = {
    val options = parseOptions(Map(), args.toList)
    options.get("convert") match {
      case Some(format) => format match {
        case "smtlib" =>
          val logPath = options.getOrElse("logpath", throw new Exception("Missing log file path, use argument -logpath path/to/logfile"))
          val outputPath = options.getOrElse("outputpath", logPath + "${entryname}.smt")
          PrettyPrinter.setPrinter(KeYmaeraXPrettyPrinter.pp)
          exportSmtLibFormat(logPath, outputPath)
        case _ => throw new Exception("Unknown output format " + format + ", use one of [smtlib]")
      }
      case None => throw new Exception("Missing output format, use -convert smtlib")
    }
  }

  private def parseOptions(parsedOptions: Map[String, String], options: List[String]): Map[String, String] = options match {
    case Nil => parsedOptions
    case "-convert" :: value :: tail => parseOptions(parsedOptions ++ Map("convert" -> value), tail)
    case "-logpath" :: value :: tail => parseOptions(parsedOptions ++ Map("logpath" -> value), tail)
    case "-outputpath" :: value :: tail => parseOptions(parsedOptions ++ Map("outputpath" -> value), tail)
    case option :: tail => logger.warn("[Warning] Unknown option " + option + "\n\n"); parseOptions(parsedOptions, tail)
  }

}
