package scalafuzz.internals

import cats.Monad
import cats.effect.IO
import cats.syntax.flatMap._
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

  case class TargetRunOneReport(input: Array[Byte], exitStatus: TargetExitStatus, invocations: Seq[InvocationId])

  def flattenInvocations(raw: ThreadSafeMap[DataDir, ThreadSafeQueue[InvocationId]]): Seq[InvocationId] =
    raw.values.flatMap(_.toArray.map(_.asInstanceOf[InvocationId])).toSeq

  // todo wrap into an IO
  // todo extract Invoker into an effect
  private def runOne(target: Target, input: Array[Byte]): TargetRunOneReport = {
    Invoker.reset()
    Try { target(input) } match {
      case Failure(e) =>
        TargetRunOneReport(input, TargetExceptionThrown(e), flattenInvocations(Invoker.invocations()))
      case Success(_) =>
        TargetRunOneReport(input, TargetNormalExit, flattenInvocations(Invoker.invocations()))
    }
  }

  def loop(options: FuzzerOptions, target: Target, inputSource: () => Array[Byte]): IO[FuzzerReport] =
    IO(runOne(target, inputSource())).flatMap { report: TargetRunOneReport =>
      // todo submit the report for analysis
      report.exitStatus match {
        case TargetExceptionThrown(e) if options.exitOnFirstFailure =>
          IO.pure(FuzzerReport(RunStats(1), Seq(ExceptionFailure(report.input, e))))
        case _ =>
          loop(options, target, inputSource)
      }
    }

  def loopTF[F[_]](options: FuzzerOptions, target: Target, inputSource: () => Array[Byte]): F[FuzzerReport] = ???

  // todo extract
  sealed trait Mutation
  case object RandomBytes extends Mutation

  // todo generate a stream of mutation descriptions and execute them passing the bytes from the seed
  def mutateBytes(input: Array[Byte], mutation: Mutation): Array[Byte] = Array.fill[Byte](1)(1)
  def randomBytes(): Array[Byte] = Array.fill[Byte](1)(1)

  def program(options: FuzzerOptions, target: Target): IO[FuzzerReport] = for {
    report <- loop(options, target, () => mutateBytes(randomBytes(), RandomBytes) )
  } yield report

  def programTF[F[_]: Monad](options: FuzzerOptions, target: Target)(implicit L: Log[F]): F[FuzzerReport] = for {
    _ <- L.info(s"starting a run with options: $options")
    report <- loopTF(options, target, () => mutateBytes(randomBytes(), RandomBytes))
  } yield report

          }
