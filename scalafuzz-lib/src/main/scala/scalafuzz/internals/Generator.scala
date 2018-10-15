package scalafuzz.internals

import cats.effect.IO
import scalafuzz.internals.Corpus.CorpusItem

import scala.util.Random

trait Generator[F[_]] {

  def randomBytesCorpusItem: F[CorpusItem]
  def emptyBytesCorpusItem: F[CorpusItem]

}

class IOGenerator extends Generator[IO] {

  def makeRandomBytes(size: Int): IO[Array[Byte]] = IO {
    Array.fill(size)((Random.nextInt(256) - 128).toByte)
  }

//  def randomInt(min: Int, max: Int): IO[Int] = IO {
//    min + Random.nextInt(max - min)
//  }

  override def randomBytesCorpusItem: IO[CorpusItem] = makeRandomBytes(10)

  override def emptyBytesCorpusItem: IO[CorpusItem] = IO.pure(Array.emptyByteArray)
}
