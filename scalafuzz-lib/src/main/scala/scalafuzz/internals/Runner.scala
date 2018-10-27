package scalafuzz.internals

import cats.Monad
import cats.data.NonEmptyList
import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import scalafuzz.Fuzzer.Target
import scalafuzz._
import scalafuzz.internals.Corpus.CorpusItem

private[scalafuzz] class Runner[F[_]: Monad](loop: Loop[F],
                                             corpus: Corpus[F],
                                             log: Log[F],
                                             reportAnalyzer: CoverageAnalyzer)
                                            (implicit F: Sync[F], generator: Generator[F]) {
  def program(options: FuzzerOptions, target: Target): F[Seq[FuzzerReport]] = for {
    _ <- log.info(s"starting a run with options: $options")
    reports <-
      loop(NonEmptyList.fromListUnsafe(generator.emptyBytesCorpusItem +: corpus.items().toList),
        options, target)
    _ <- log.info(s"finished with results: $reports")
  } yield reports

  private def loop(corpusInputs: NonEmptyList[F[CorpusItem]],
                   options: FuzzerOptions,
                   target: Target): F[Seq[FuzzerReport]] = for {
    report <- loop.run(options, target, StreamedMutator.seeded(corpusInputs.head), reportAnalyzer)
    _ <- corpus.add(report.newCorpusItems)
    reports <-
      if (report.failures.nonEmpty && options.exitOnFirstFailure)
        F.delay(Seq())
      else
        corpusInputs.tail match {
          case Nil =>
            corpus.addedAfterLastCall match {
              case Nil =>
                loop(NonEmptyList.one(generator.randomBytesCorpusItem), options, target)
              case addedItems =>
                loop(NonEmptyList.fromListUnsafe(addedItems.toList), options, target)
            }
          case tail =>
            loop(NonEmptyList.fromListUnsafe(tail), options, target)
        }
  } yield report +: reports

}
