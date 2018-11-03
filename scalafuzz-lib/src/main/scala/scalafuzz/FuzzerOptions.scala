package scalafuzz

import scalafuzz.Platform.File

import scala.concurrent.duration.Duration

case class FuzzerOptions(maxDuration: Duration,
                         exitOnFirstFailure: Boolean = false,
                         existingCorpus: Option[File] = None,
                         corpusAdditions: Option[File] = None)
