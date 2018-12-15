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
          totalDurationOnStartNano: Long): F[CorpusItemLoopReport]
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
        TargetRunReport(input, TargetNormalExit, flattenInvocations(Invoker.invocations()),
          math.max(elapsedTimeNano, 10000L))
    }
  }

  override def run(options: FuzzerOptions,
                   target: Target,
                   mutatorGen: Mutator[IO],
                   coverageAnalyzer: CoverageAnalyzer,
                   totalDurationOnStartNano: Long): IO[CorpusItemLoopReport] = {
    def innerLoop(currentRunCount: Int, mutatorGen: Mutator[IO], currentTotalDuration: Long): IO[Seq[TargetRunReport]] = for {
      bytes <- mutatorGen.mutatedBytes()
      targetRunReport <- IO { runOne(target, bytes) }
      newCurrentTotalDuration = currentTotalDuration + targetRunReport.elapsedTimeNano
      reports <- (targetRunReport.exitStatus, options.maxDuration) match {
        case (TargetExceptionThrown(_), _) if options.exitOnFirstFailure =>
          IO.pure(Seq(targetRunReport))
        case (_, maxDuration: FiniteDuration) if newCurrentTotalDuration > maxDuration.toNanos =>
          IO.pure(Seq(targetRunReport))
        case _ =>
          mutatorGen.next(targetRunReport.input) match {
            case Some(mutator) =>
              innerLoop(currentRunCount + 1, mutator, newCurrentTotalDuration)
                .map(rs => targetRunReport +: rs)
            case None =>
              IO.pure(Seq(targetRunReport))
          }
      }
    } yield reports
    innerLoop(1, mutatorGen, totalDurationOnStartNano)
      .map(rs => CorpusItemLoopReport(rs, coverageAnalyzer))
  }

}

// todo rename to WhateverPhaseLoopReport
case class CorpusItemLoopReport(runCount: Int,
                                elapsedTimeNano: Long,
                                failures: Seq[TargetFailure],
                                newCorpusItems: Seq[CorpusItem])

object CorpusItemLoopReport {
  def apply(trr: Seq[TargetRunReport], coverageAnalyzer: CoverageAnalyzer): CorpusItemLoopReport =
    CorpusItemLoopReport(
      runCount = trr.length,
      elapsedTimeNano = trr.map(_.elapsedTimeNano).sum,
      failures = trr.flatMap {
        case TargetRunReport(input, TargetExceptionThrown(e), _, _) => Seq(ExceptionFailure(input, e))
        case _ => Seq()
      },
      trr.map(coverageAnalyzer.process).flatMap {
        case NewCoverage(input) => Seq(input)
        case _ => Seq()
      }
    )
}
