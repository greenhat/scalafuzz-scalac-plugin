package scalafuzz.internals

import cats.effect.IO
import scalafuzz.internals.Corpus.CorpusItem

import scala.collection.mutable


trait Corpus[F[_]] {

  def items(): Seq[F[CorpusItem]]
  def add(inputs: Seq[CorpusItem]): F[Unit]
  // todo: remove
  def addedAfterLastCall: Seq[F[CorpusItem]]
}

object Corpus {
  type CorpusItem = Array[Byte]
}

class IOInMemoryCorpus extends Corpus[IO] {

  private val store = mutable.Set[CorpusItem]()
  private val storeForAddedAfterLastCall = mutable.Set[CorpusItem]()

  override def items(): Seq[IO[Array[Byte]]] =
    store.toSeq.map(i => IO.pure(i))

  override def add(inputs: Seq[CorpusItem]): IO[Unit] = IO {
    inputs match {
      case Nil => ()
      case v =>
        v.foreach{i => store.add(i); storeForAddedAfterLastCall.add(i)}
    }
  }

  override def addedAfterLastCall: Seq[IO[CorpusItem]] = {
    val res = storeForAddedAfterLastCall.toSeq.map(i => IO.pure(i))
    storeForAddedAfterLastCall.clear()
    res
  }
}

