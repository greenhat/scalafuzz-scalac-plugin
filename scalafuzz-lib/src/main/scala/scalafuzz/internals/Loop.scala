package scalafuzz.internals

import cats.effect.IO
import scalafuzz.Fuzzer.Target
import scalafuzz.Invoker.{DataDir, InvocationId, ThreadSafeQueue}
import scalafuzz.Platform.ThreadSafeMap
import scalafuzz._
import scalafuzz.internals.Corpus.CorpusItem
import scalafuzz.internals.CoverageAnalyzer.{NewCoverage, NoNewCoverage}

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success, Try}

trait Loop[F[_]] {
  def run(options: FuzzerOptions,
          target: Target,
          mutatorGen: Mutator[F],
          coverageAnalyzer: CoverageAnalyzer,
          totalDurationOnStartNano: Long): F[FuzzerReport]
}

class IOLoop extends Loop[IO] {

  private def flattenInvocations(raw: ThreadSafeMap[DataDir, ThreadSafeQueue[InvocationId]]): Seq[InvocationId] =
    raw.values.flatMap(_.toArray.map(_.asInstanceOf[InvocationId])).toSeq

  private def runOne(target: Target, input: Array[Byte]): TargetRunReport = {
    Invoker.reset()
    Try {
      val now = System.nanoTime()
      target(input)
      System.nanoTime() - now
    } match {
      case Failure(e) =>
        TargetRunReport(input, TargetExceptionThrown(e), flattenInvocations(Invoker.invocations()), 0L)
      case Success(elapsedTimeNano) =>
        TargetRunReport(input, TargetNormalExit, flattenInvocations(Invoker.invocations()), elapsedTimeNano)
    }
  }

  override def run(options: FuzzerOptions,
                   target: Target,
                   mutatorGen: Mutator[IO],
                   coverageAnalyzer: CoverageAnalyzer,
                   totalDurationOnStartNano: Long): IO[FuzzerReport] = {
    def innerLoop(currentRunCount: Int, mutatorGen: Mutator[IO], currentTotalDuration: Long): IO[FuzzerReport] = for {
      bytes <- mutatorGen.mutatedBytes()
      // workaround scalac bug with tuple decomposition
      tuple <- IO[(TargetRunReport, Seq[CorpusItem])] {
        val report = runOne(target, bytes)
        coverageAnalyzer.process(report) match {
          case NoNewCoverage => (report, Seq.empty)
          case NewCoverage(input) => (report, Seq(input))
        }
      }
      targetRunReport = tuple._1
      newCorpusItems = tuple._2
      report <- (targetRunReport.exitStatus, options.maxDuration) match {
        case (TargetExceptionThrown(e), _) if options.exitOnFirstFailure =>
          IO.pure(
            FuzzerReport(RunStats(currentRunCount),
              Seq(ExceptionFailure(targetRunReport.input, e)),
              newCorpusItems, elapsedTimeNano = currentTotalDuration - totalDurationOnStartNano))
        case (_, maxDuration: FiniteDuration)
          if (currentTotalDuration + targetRunReport.elapsedTimeNano) > maxDuration.toNanos =>
          IO.pure(
            FuzzerReport(RunStats(currentRunCount),
              Seq(),
              newCorpusItems, currentTotalDuration + targetRunReport.elapsedTimeNano - totalDurationOnStartNano))
        case _ =>
          mutatorGen.next(targetRunReport.input) match {
            case Some(mutator) =>
              innerLoop(currentRunCount + 1, mutator, currentTotalDuration + targetRunReport.elapsedTimeNano)
            case None =>
              IO.pure(FuzzerReport(
                RunStats(currentRunCount), Seq(), newCorpusItems, currentTotalDuration - totalDurationOnStartNano))
          }
      }
    } yield report
    innerLoop(1, mutatorGen, totalDurationOnStartNano)
  }

}
