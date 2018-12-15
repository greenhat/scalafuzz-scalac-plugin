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
  def program(options: FuzzerOptions, target: Target): F[FuzzerReport] = for {
    _ <- log.info(s"starting a run with options: $options")
    report <-
      loop(NonEmptyList.fromListUnsafe(generator.emptyBytesCorpusItem +: corpus.items().toList),
        options, target, totalDurationNano = 0L).map(FuzzerReport.apply)
    _ <- log.info(s"finished with total runs: ${report.stats.runCount}")
  } yield report

  private def loop(corpusInputs: NonEmptyList[F[CorpusItem]],
                   options: FuzzerOptions,
                   target: Target,
                   totalDurationNano: Long): F[Seq[CorpusItemLoopReport]] = for {
    report <- loop.run(options, target, StreamedMutator.seeded(corpusInputs.head), reportAnalyzer,
      totalDurationOnStartNano = totalDurationNano)
    _ <- corpus.add(report.newCorpusItems)
    nextCorpusItems = corpusInputs.tail ++ report.newCorpusItems.map(i => F.delay(i))
    reports <-
      if ((report.failures.nonEmpty && options.exitOnFirstFailure)
        || (options.maxDuration.isFinite && options.maxDuration.toNanos < totalDurationNano + report.elapsedTimeNano))
        F.delay(Seq())
      else {
        if (nextCorpusItems.isEmpty)
          loop(NonEmptyList.one(generator.randomBytesCorpusItem), options, target,
            totalDurationNano + report.elapsedTimeNano)
        else
          loop(NonEmptyList.fromListUnsafe(nextCorpusItems), options, target,
            totalDurationNano + report.elapsedTimeNano)
      }
  } yield report +: reports
}
