package scalafuzz.internals

import java.util

import scalafuzz.internals.CoverageAnalyzer.{CoverageReport, NewCoverage, NoNewCoverage}

import scala.collection.mutable

class CoverageAnalyzer {

  private val store = mutable.Set[Int]()

  def process(report: TargetRunReport): CoverageReport = {
    val hash = util.Arrays.hashCode(report.invocations.toArray)
    if (store.contains(hash)) {
       NoNewCoverage
    } else {
      store.add(hash)
      NewCoverage(report.input)
    }
  }
}

object CoverageAnalyzer {
  sealed trait CoverageReport
  case object NoNewCoverage extends CoverageReport
  case class NewCoverage(input: Array[Byte]) extends CoverageReport
}
