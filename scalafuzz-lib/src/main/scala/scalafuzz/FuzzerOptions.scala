package scalafuzz

import scalafuzz.Platform.File

import scala.concurrent.duration.Duration

case class FuzzerOptions(duration: RunDuration,
                         exitOnFirstFailure: Boolean = false,
                         existingCorpus: Option[File] = None,
                         corpusAdditions: Option[File] = None)

sealed trait RunDuration
case class TimedDuration(t: Duration) extends RunDuration
case object UnlimitedDuration extends RunDuration
