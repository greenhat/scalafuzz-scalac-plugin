package scalafuzz.internals

import cats.effect.IO
import scalafuzz.internals.Corpus.AddCorpusItem

trait TargetRunReportAnalyzer[F[_]] {

  val addCorpusItem: AddCorpusItem[F]

  // todo: After each target run check if new coverage achieved (new features discovered, e.g. new invocations collected) then add the input to the corpus.
  def process(report: TargetRunReport): F[Unit]
}

class IOTargetRunReportAnalyzer(override val addCorpusItem: AddCorpusItem[IO])
  extends TargetRunReportAnalyzer[IO] {

  def process(report: TargetRunReport): IO[Unit] = {
    IO.pure(())
  }
}
