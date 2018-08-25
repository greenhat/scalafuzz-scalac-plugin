package scalafuzz.internals

import cats.effect.Effect

trait TargetRunReportAnalyzer[F[_]] {
  def process(report: TargetRunReport): F[Unit]
}

class SyncTargetRunReportAnalyzer[F[_]](implicit E: Effect[F]) extends TargetRunReportAnalyzer[F] {

  override def process(report: TargetRunReport): F[Unit] = {
    // todo create and use CorpusRepository to save input for "interesting" cases
    E.pure(())
  }
}
