package scalafuzz

import scalafuzz.Invoker.{DataDir, InvocationId, ThreadSafeQueue}
import scalafuzz.Platform.{File, ThreadSafeMap}

import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

object Fuzzer extends ScalafuzzLogging {

  // todo extract
  case class RunOptions(duration: RunDuration,
                        existingCorpus: Option[File] = None,
                        corpusAdditions: Option[File] = None)

  sealed trait RunDuration
  case class NumberOfRunsDuration(n: Long) extends RunDuration
  case class TimedDuration(t: Duration) extends RunDuration
  case object UntilFailure extends RunDuration
  case object UnlimitedDuration extends RunDuration

  // todo extract
  case class RunReport(stats: RunStats, failures: Seq[Failure])

  case class RunStats(runCount: Long)

  sealed trait Failure
  case class ExceptionFailure(input: Array[Byte],
                       exception: Throwable,
                       corpusAddition: Option[File] = None) extends Failure
  case class TimeoutFailure(input: Array[Byte],
                     elapsedTime: Duration,
                     corpusAddition: Option[File] = None) extends Failure

  // todo extract
  type Target = Array[Byte] => Unit

  sealed trait TargetExitStatus
  case object TargetNormalExit extends TargetExitStatus
  case class TargetExceptionThrown(e: Throwable) extends TargetExitStatus

  case class TargetRunOneReport(exitStatus: TargetExitStatus, invocations: Seq[InvocationId])

  def flattenInvocations(raw: ThreadSafeMap[DataDir, ThreadSafeQueue[InvocationId]]): Seq[InvocationId] =
    raw.values.flatMap(_.toArray.map(_.asInstanceOf[InvocationId])).toSeq

  // todo wrap into an IO?
  private def runOne(target: Target, input: Array[Byte]): TargetRunOneReport = {
    Invoker.reset()
    Try { target(input) } match {
      case Failure(e) =>
        TargetRunOneReport(TargetExceptionThrown(e), flattenInvocations(Invoker.invocations()))
      case Success(_) =>
        TargetRunOneReport(TargetNormalExit, flattenInvocations(Invoker.invocations()))
    }
  }

  def run(options: RunOptions, target: Target): RunReport = {
    log.info(s"starting a run with options: $options")
    runOne(target, Array.fill[Byte](1)(1))
    RunReport(RunStats(1), Seq())
  }

}
