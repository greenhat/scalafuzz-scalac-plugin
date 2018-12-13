package scalafuzz

import scalafuzz.Platform.File
import scalafuzz.internals.Corpus.CorpusItem
import scalafuzz.internals.CorpusItemLoopReport

import scala.concurrent.duration.Duration

case class FuzzerReport(stats: RunStats,
                        failures: Seq[TargetFailure],
                        newCorpusItems: Seq[CorpusItem],
                        elapsedTimeNano: Long)

object FuzzerReport {
  def apply(cs: Seq[CorpusItemLoopReport]): FuzzerReport =
    FuzzerReport(
      stats = RunStats(cs.map(_.runCount).sum),
      failures = cs.flatMap(_.failures),
      newCorpusItems = cs.flatMap(_.newCorpusItems),
      elapsedTimeNano = cs.map(_.elapsedTimeNano).sum
    )
}

case class RunStats(runCount: Int)

sealed trait TargetFailure
case class ExceptionFailure(input: Array[Byte],
                            exception: Throwable,
                            corpusAddition: Option[File] = None) extends TargetFailure
case class TimeoutFailure(input: Array[Byte],
                          elapsedTime: Duration,
                          corpusAddition: Option[File] = None) extends TargetFailure

