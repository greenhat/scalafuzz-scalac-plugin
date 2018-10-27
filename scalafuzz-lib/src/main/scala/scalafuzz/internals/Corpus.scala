package scalafuzz.internals

import cats.effect.IO
import scalafuzz.internals.Corpus.CorpusItem


trait Corpus[F[_]] {

  def load: Seq[F[CorpusItem]]
  def add(inputs: Seq[CorpusItem]): F[Unit]
  def added: Seq[F[CorpusItem]]
}

object Corpus {
  type CorpusItem = Array[Byte]
}

class IOCorpus extends Corpus[IO] {

  // todo implement
  override def load: Seq[IO[Array[Byte]]] = {
    Seq()
  }

  // todo implement
  override def add(inputs: Seq[CorpusItem]): IO[Unit] = inputs match {
    case _ => IO.unit
  }

  // todo implement
  override def added: Seq[IO[CorpusItem]] = Seq()
}

