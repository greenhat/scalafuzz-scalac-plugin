package scalafuzz.internals.mutations

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
  def apply[F[_]]()(implicit generator: Generator[F]): RandomBytesMutation[F] = new RandomBytesMutation(generator)
}



