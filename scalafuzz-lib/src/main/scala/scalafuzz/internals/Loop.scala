package scalafuzz.internals

import cats.effect.IO
import scalafuzz.Fuzzer.Target
import scalafuzz.Invoker.{DataDir, InvocationId, ThreadSafeQueue}
import scalafuzz.Platform.ThreadSafeMap
import scalafuzz._

import scala.util.{Failure, Success, Try}

trait Loop[F[_], G[_]] {
  def run(options: FuzzerOptions,
          target: Target,
          inputSource: () => Array[Byte],
          reportAnalyzer: TargetRunReportAnalyzer[G]): F[FuzzerReport]
}

class IOLoop[G[_]] extends Loop[IO, G] {

  private def flattenInvocations(raw: ThreadSafeMap[DataDir, ThreadSafeQueue[InvocationId]]): Seq[InvocationId] =
    raw.values.flatMap(_.toArray.map(_.asInstanceOf[InvocationId])).toSeq

  private def runOne(target: Target, input: Array[Byte]): TargetRunReport = {
    Invoker.reset()
    Try { target(input) } match {
      case Failure(e) =>
        TargetRunReport(input, TargetExceptionThrown(e), flattenInvocations(Invoker.invocations()))
      case Success(_) =>
        TargetRunReport(input, TargetNormalExit, flattenInvocations(Invoker.invocations()))
    }
  }

  override def run(options: FuzzerOptions,
                   target: Target,
                   inputSource: () => Array[Byte],
                   reportAnalyzer: TargetRunReportAnalyzer[G]): IO[FuzzerReport] =
    IO(runOne(target, inputSource())).flatMap { report: TargetRunReport =>
      reportAnalyzer.process(report)
      report.exitStatus match {
        case TargetExceptionThrown(e) if options.exitOnFirstFailure =>
          IO.pure(FuzzerReport(RunStats(1), Seq(ExceptionFailure(report.input, e))))
        case _ =>
          run(options, target, inputSource, reportAnalyzer)
      }
    }
}

