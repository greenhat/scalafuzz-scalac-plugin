package scalafuzz.internals

import cats.effect.IO
import scalafuzz.Fuzzer.Target
import scalafuzz.Invoker.{DataDir, InvocationId, ThreadSafeQueue}
import scalafuzz.Platform.ThreadSafeMap
import scalafuzz._

import scala.util.{Failure, Success, Try}

private[scalafuzz] object Runner {

  // todo extract
  sealed trait TargetExitStatus {
    def exceptions: Seq[Throwable] = this match {
      case TargetNormalExit => Seq()
      case TargetExceptionThrown(e) => Seq(e)
    }
  }

  case object TargetNormalExit extends TargetExitStatus
  case class TargetExceptionThrown(e: Throwable) extends TargetExitStatus

  case class TargetRunOneReport(exitStatus: TargetExitStatus, invocations: Seq[InvocationId])

  def flattenInvocations(raw: ThreadSafeMap[DataDir, ThreadSafeQueue[InvocationId]]): Seq[InvocationId] =
    raw.values.flatMap(_.toArray.map(_.asInstanceOf[InvocationId])).toSeq

  // todo wrap into an IO
  // todo extract Invoker into an effect
  private def runOne(target: Target, input: Array[Byte]): TargetRunOneReport = {
    Invoker.reset()
    Try { target(input) } match {
      case Failure(e) =>
        TargetRunOneReport(TargetExceptionThrown(e), flattenInvocations(Invoker.invocations()))
      case Success(_) =>
        TargetRunOneReport(TargetNormalExit, flattenInvocations(Invoker.invocations()))
    }
  }

  def loop(options: FuzzerOptions, target: Target, inputSource: () => Array[Byte]): IO[FuzzerReport] =
    IO(runOne(target, inputSource())).flatMap { report: TargetRunOneReport =>
      // todo submit the report for analysis
      report.exitStatus match {
        case TargetExceptionThrown(e) if options.exitOnFirstFailure =>
          IO.pure(FuzzerReport(RunStats(1), Seq(ExceptionFailure(bytesSource(), e))))
        case _ =>
          loop(options, target, inputSource)
      }
    }

  // todo generate the list of mutation _descriptions_ and execute them passing the bytes from the seed
  private def bytesSource(): Array[Byte] = Array.fill[Byte](1)(1)

  def program(options: FuzzerOptions, target: Target): IO[FuzzerReport] = for {
    report <- loop(options, target, bytesSource)
  } yield report

  //  def program[F[_]: Log](options: RunOptions, target: Target)(implicit L: Log[F]): IO[RunReport] = for {
  //    _ <- L.info(s"starting a run with options: $options")
  //    runReport = RunReport(RunStats(1), Seq())
  //  } yield runReport

}
