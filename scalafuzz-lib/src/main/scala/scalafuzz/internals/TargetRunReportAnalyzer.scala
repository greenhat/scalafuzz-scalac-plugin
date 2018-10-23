package scalafuzz.internals

import java.util

import cats.data.State
import cats.effect.IO
import scalafuzz.internals.Corpus.AddCorpusItem

trait TargetRunReportAnalyzer[F[_]] {

  val addCorpusItem: AddCorpusItem[F]

  private def update[S](f: S => S): State[S, Unit] = for {
    v <- State.get[S]
    nv = f(v)
    _ <- State.set(nv)
  } yield ()

  protected def pushCoverageHash(hash: Int): State[List[Int], Unit] = update(hash :: _)
  protected def hasCoverageHash(hash: Int): State[List[Int], Boolean] = State.get[List[Int]].map(_.contains(hash))

  // todo: After each target run check if new coverage achieved
  // (new features discovered, e.g. new invocations collected) then add the input to the corpus.
  // todo: test
  def process(report: TargetRunReport): TargetRunReport = {
    val hash = util.Arrays.hashCode(report.invocations.toArray)
    for {
      hasCoverage <- hasCoverageHash(hash)
      _ <- if (!hasCoverage) {
        addCorpusItem(report.input)
        pushCoverageHash(hash)
      } else State.get[List[Int]]
    } yield ()
    report
  }
}

class IOTargetRunReportAnalyzer(override val addCorpusItem: AddCorpusItem[IO])
  extends TargetRunReportAnalyzer[IO]
