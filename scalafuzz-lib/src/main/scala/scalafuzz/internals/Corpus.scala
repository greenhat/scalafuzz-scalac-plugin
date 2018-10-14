package scalafuzz.internals

import cats.effect.IO
import scalafuzz.internals.Corpus.CorpusItem


trait Corpus[F[_]] {

  def load: Seq[F[CorpusItem]]
  def add(input: CorpusItem): F[Unit]
  def added: Seq[F[CorpusItem]]
  def needsReload: Boolean
}

object Corpus {
  type CorpusItem = Array[Byte]
}

class IOCorpus extends Corpus[IO] {

  private var _needsReload = true
  override def needsReload: Boolean = _needsReload

  override def load: Seq[IO[Array[Byte]]] = {
    _needsReload = false
    Seq(
      IO {
        Array.emptyByteArray
      }
    )
  }

  override def add(input: Array[Byte]): IO[Unit] = ???

  override def added: Seq[IO[CorpusItem]] = ???
}

