package scalafuzz.internals

import cats.effect.IO

trait Corpus[F[_]] {

  def load: Seq[F[Array[Byte]]]
  def add(input: Array[Byte]): F[Unit]
  def needsReload: Boolean
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

}

