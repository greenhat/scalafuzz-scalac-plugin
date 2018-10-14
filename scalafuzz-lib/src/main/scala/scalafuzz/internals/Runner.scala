package scalafuzz.internals

import cats.Monad
import cats.data.NonEmptyList
import cats.syntax.flatMap._
import cats.syntax.functor._
import scalafuzz.Fuzzer.Target
import scalafuzz._
import scalafuzz.internals.Corpus.CorpusItem

private[scalafuzz] class Runner[F[_]: Monad](loop: Loop[F],
                                             corpus: Corpus[F],
                                             mutator: Mutator[F],
                                             log: Log[F],
                                             reportAnalyzer: TargetRunReportAnalyzer[F]){

  /*
   todo:
  Initial stage:
- load inputs from Corpus;
- run each input (w/o mutations) through the target;
   */

  def program(options: FuzzerOptions, target: Target): F[FuzzerReport] = for {
    _ <- log.info(s"starting a run with options: $options")
    report <- loop.run(options, target, mutator, reportAnalyzer)
    _ <- log.info(s"finished with results: $report")
  } yield report

  def program2(options: FuzzerOptions, target: Target): F[Seq[FuzzerReport]] = for {
    _ <- log.info(s"starting a run with options: $options")
    reports <- loop(NonEmptyList.fromListUnsafe(emptyBytesCorpusItem +: corpus.load.toList), options, target)
    _ <- log.info(s"finished with results: $reports")
  } yield reports

  private def randomBytesCorpusItem: F[CorpusItem] = ???
  private def emptyBytesCorpusItem: F[CorpusItem] = ???

  private def loop(corpusInputs: NonEmptyList[F[CorpusItem]], options: FuzzerOptions, target: Target): F[Seq[FuzzerReport]] = for {
    report <- loop.run(options, target, StreamedMutator.seeded(corpusInputs.head), reportAnalyzer)
    reports <- corpusInputs.tail match {
      case Nil =>
        corpus.added match {
          case Nil =>
            loop(NonEmptyList.one(randomBytesCorpusItem), options, target)
          case addedItems =>
            loop(NonEmptyList.fromListUnsafe(addedItems.toList), options, target)
        }
      case tail =>
        loop(NonEmptyList.fromListUnsafe(tail), options, target)
    }
  } yield report +: reports

}
