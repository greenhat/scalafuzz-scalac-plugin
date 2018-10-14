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
}

class IOCorpus extends Corpus[IO] {

  override def load: Seq[IO[Array[Byte]]] = {
    Seq()
  }

  override def add(input: Array[Byte]): IO[Unit] = ???

  override def added: Seq[IO[CorpusItem]] = ???
}

