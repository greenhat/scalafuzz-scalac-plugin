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

  private def runOne(target: Target, input: Array[Byte]): IO[TargetRunReport] = IO {
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

  /*
todo:
In a endless loop:
- check if corpus should be reloaded (new inputs were added);
- choose random input from the corpus or generate random bytes if empty;
- run deterministic mutations (see afl);
- run fixed number of stacked deterministic and random mutations (see afl and libfuzzer);

After each target run check if new coverage achieved (new features discovered, e.g. new invocations collected) then add the input to the corpus.
 */

  override def run(options: FuzzerOptions,
                   target: Target,
                   mutatorGen: Mutator[IO],
                   reportAnalyzer: TargetRunReportAnalyzer[IO]): IO[FuzzerReport] = {
    def innerLoop(currentRunCount: Int, mutatorGen: Mutator[IO]): IO[FuzzerReport] = for {
      bytes <- mutatorGen.mutatedBytes()
      targetRunReport <- runOne(target, bytes)
      _ <- reportAnalyzer.process(targetRunReport)
      report <- targetRunReport.exitStatus match {
        case TargetExceptionThrown(e) if options.exitOnFirstFailure =>
          IO.pure(FuzzerReport(RunStats(currentRunCount), Seq(ExceptionFailure(targetRunReport.input, e))))
        case _ =>
          innerLoop(currentRunCount + 1, mutatorGen.next(targetRunReport.input))
      }
    } yield report
    innerLoop(1, mutatorGen)
  }

}