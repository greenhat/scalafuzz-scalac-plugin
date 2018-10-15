package scalafuzz.internals.mutations

import cats.effect.IO
import scalafuzz.internals.Generator

object Mutations {
}

trait Mutation[F[_]] {
  def mutate(input: Array[Byte]): F[Array[Byte]]
}

class RandomBytesMutation[F[_]](generator: Generator[F]) extends Mutation[F] {
  override def mutate(input: Array[Byte]): F[Array[Byte]] = generator.randomBytesCorpusItem
}

object RandomBytesMutation {
  def io(implicit generator: Generator[IO]): RandomBytesMutation[IO] = new RandomBytesMutation(generator)
}



