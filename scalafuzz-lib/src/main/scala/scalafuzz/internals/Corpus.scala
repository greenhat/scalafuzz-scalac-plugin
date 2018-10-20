package scalafuzz.internals

import cats.effect.IO
import scalafuzz.internals.Corpus.CorpusItem


trait Corpus[F[_]] {

  def load: Seq[F[CorpusItem]]
  def add(input: CorpusItem): F[Unit]
  def added: Seq[F[CorpusItem]]
}

object Corpus {
  type CorpusItem = Array[Byte]
  type AddCorpusItem[F[_]] = CorpusItem => F[Unit]
}

class IOCorpus extends Corpus[IO] {

  // todo implement
  override def load: Seq[IO[Array[Byte]]] = {
    Seq()
  }

  // todo implement
  override def add(input: Array[Byte]): IO[Unit] = ???

  // todo implement
  override def added: Seq[IO[CorpusItem]] = Seq()
}

