package scalafuzz.internals

import java.util

import cats.data.State
import scalafuzz.internals.CoverageAnalyzer.{CoverageReport, NoNewCoverage}

class CoverageAnalyzer {

  private def update[S](f: S => S): State[S, Unit] = for {
    v <- State.get[S]
    nv = f(v)
    _ <- State.set(nv)
  } yield ()

  protected def pushCoverageHash(hash: Int): State[List[Int], Unit] = update(hash :: _)
  protected def hasCoverageHash(hash: Int): State[List[Int], Boolean] = State.get[List[Int]].map(_.contains(hash))

  // todo: After each target run check if new coverage achieved
  // (new features discovered, e.g. new invocations collected) then add the input to the corpus.
  // todo: test
  def process(report: TargetRunReport): CoverageReport = {
    val hash = util.Arrays.hashCode(report.invocations.toArray)
//    for {
//      hasCoverage <- hasCoverageHash(hash)
//      coverageReport <- if (!hasCoverage) {
//        pushCoverageHash(hash)
//        NewCoverage(report.input)
//      } else
//        NoNewCoverage
//    } yield coverageReport
    // todo fix
    NoNewCoverage
  }
}

object CoverageAnalyzer {
  sealed trait CoverageReport
  case object NoNewCoverage extends CoverageReport
  case class NewCoverage(input: Array[Byte]) extends CoverageReport
}
