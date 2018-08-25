package scalafuzz.internals

import cats.Monad
import cats.syntax.flatMap._
import cats.syntax.functor._
import scalafuzz.Fuzzer.Target
import scalafuzz._
import scalafuzz.internals.Mutator.{RandomBytes, randomBytes}

private[scalafuzz] class Runner[F[_]: Monad](loop: Loop[F], log: Log[F]){

  def program(options: FuzzerOptions, target: Target): F[FuzzerReport] = for {
    _ <- log.info(s"starting a run with options: $options")
    report <- loop.run(options, target, () => Mutator.mutateBytes(randomBytes(), RandomBytes))
    _ <- log.info(s"finished with results: $report")
  } yield report

}
