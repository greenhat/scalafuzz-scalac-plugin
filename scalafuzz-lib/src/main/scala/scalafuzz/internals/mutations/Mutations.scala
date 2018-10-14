package scalafuzz.internals.mutations

import cats.effect.IO

import scala.util.Random

object Mutations {

  type Mutation = Array[Byte] => Array[Byte]

  def makeRandomBytes(size: Int): IO[Array[Byte]] = IO {
    Array.fill(size)((Random.nextInt(256) - 128).toByte)
  }

  def randomInt(min: Int, max: Int): IO[Int] = IO {
    min + Random.nextInt(max - min)
  }

  def randomBytes: IO[Mutation] =
    for {
      n <- randomInt(1, 10)
      bytes <- makeRandomBytes(n)
    } yield { _: Array[Byte] =>
      bytes
    }
}


