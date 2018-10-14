package scalafuzz.internals

import cats.effect.IO

trait TargetRunReportAnalyzer[F[_]] {
  def process(report: TargetRunReport): F[Unit]
}

class IOTargetRunReportAnalyzer extends TargetRunReportAnalyzer[IO] {

  override def process(report: TargetRunReport): IO[Unit] = {
    // todo create and use CorpusRepository to save input for "interesting" cases
    IO.pure(())
  }
}
