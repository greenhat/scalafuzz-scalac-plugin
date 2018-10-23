package scalafuzz.internals

import cats.effect.IO
import scalafuzz.Fuzzer.Target
import scalafuzz.Invoker.{DataDir, InvocationId, ThreadSafeQueue}
import scalafuzz.Platform.ThreadSafeMap
import scalafuzz._

import scala.util.{Failure, Success, Try}

trait Loop[F[_]] {
  def run(options: FuzzerOptions,
          target: Target,
          mutatorGen: Mutator[F],
          reportAnalyzer: TargetRunReportAnalyzer[F]): F[FuzzerReport]
}

class IOLoop extends Loop[IO] {

  private def flattenInvocations(raw: ThreadSafeMap[DataDir, ThreadSafeQueue[InvocationId]]): Seq[InvocationId] =
    raw.values.flatMap(_.toArray.map(_.asInstanceOf[InvocationId])).toSeq

  private def runOne(target: Target, input: Array[Byte]): TargetRunReport = {
    Invoker.reset()
    Try {
      target(input)
    } match {
      case Failure(e) =>
        TargetRunReport(input, TargetExceptionThrown(e), flattenInvocations(Invoker.invocations()))
      case Success(_) =>
        TargetRunReport(input, TargetNormalExit, flattenInvocations(Invoker.invocations()))
    }
  }

  override def run(options: FuzzerOptions,
                   target: Target,
                   mutatorGen: Mutator[IO],
                   reportAnalyzer: TargetRunReportAnalyzer[IO]): IO[FuzzerReport] = {
    def innerLoop(currentRunCount: Int, mutatorGen: Mutator[IO]): IO[FuzzerReport] = for {
      bytes <- mutatorGen.mutatedBytes()
      targetRunReport <- IO { reportAnalyzer.process(runOne(target, bytes)) }
      report <- targetRunReport.exitStatus match {
        case TargetExceptionThrown(e) if options.exitOnFirstFailure =>
          IO.pure(FuzzerReport(RunStats(currentRunCount), Seq(ExceptionFailure(targetRunReport.input, e))))
        case _ => mutatorGen.next(targetRunReport.input) match {
          case Some(mutator) =>
            innerLoop(currentRunCount + 1, mutator)
          case None =>
            IO.pure(FuzzerReport(RunStats(currentRunCount), Seq()))
        }
      }
    } yield report
    innerLoop(1, mutatorGen)
  }

}
