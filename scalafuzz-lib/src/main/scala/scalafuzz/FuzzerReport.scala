package scalafuzz

import scalafuzz.Platform.File
import scalafuzz.internals.Corpus.CorpusItem

import scala.concurrent.duration.Duration

case class FuzzerReport(stats: RunStats, failures: Seq[Failure], newCorpusItems: Seq[CorpusItem])

case class RunStats(runCount: Long)

sealed trait Failure
case class ExceptionFailure(input: Array[Byte],
                            exception: Throwable,
                            corpusAddition: Option[File] = None) extends Failure
case class TimeoutFailure(input: Array[Byte],
                          elapsedTime: Duration,
                          corpusAddition: Option[File] = None) extends Failure

