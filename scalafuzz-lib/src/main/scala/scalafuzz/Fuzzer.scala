package scalafuzz

import scalafuzz.Platform.File

import scala.concurrent.duration.Duration

object Fuzzer extends ScalafuzzLogging {

  // todo extract?
  case class RunOptions(duration: RunDuration,
                        existingCorpus: Option[File] = None,
                        corpusAdditions: Option[File] = None)

  sealed trait RunDuration
  case class NumberOfRunsDuration(n: Long) extends RunDuration
  case class TimedDuration(t: Duration) extends RunDuration
  case object UntilFailure extends RunDuration
  case object UnlimitedDuration extends RunDuration

  // todo extract?
  case class RunReport(stats: RunStats, failures: Seq[Failure])

  case class RunStats(runCount: Long)

  sealed trait Failure
  case class ExceptionFailure(input: Array[Byte],
                       exception: Throwable,
                       corpusAddition: Option[File] = None) extends Failure
  case class TimeoutFailure(input: Array[Byte],
                     elapsedTime: Duration,
                     corpusAddition: Option[File] = None) extends Failure

  def run(options: RunOptions, receiver: Array[Byte] => Unit): RunReport = {
    log.info(s"starting a run with options: $options")
    var runCount: Long = 1
    while (true) {
      Invoker.reset()
      val bytes = Array[Byte]()
      try {
        receiver(bytes)
        runCount += 1
      } catch {
        case e: Throwable =>
          return RunReport(RunStats(runCount), Seq(ExceptionFailure(bytes, e)))
      }
      val invs = Invoker.invocations()
    }
    RunReport(RunStats(runCount), Seq())
  }

}
