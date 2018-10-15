package scalafuzz.internals

import cats.data.NonEmptyList
import cats.effect.IO
import scalafuzz.internals.mutations.{Mutation, RandomBytesMutation}

trait Mutator[F[_]]{
  def mutatedBytes(): F[Array[Byte]]
  def next(input: Array[Byte]): Mutator[F]
}

class StreamedMutator[F[_]](bytes: F[Array[Byte]], mutations: NonEmptyList[Mutation[F]]) extends Mutator[F] {

  override def mutatedBytes(): F[Array[Byte]] = bytes

  // todo: it's not an endless list anymore (corpus inputs are "endless")
  override def next(input: Array[Byte]): Mutator[F] =
    new StreamedMutator(
      mutations.head.mutate(input),
      mutations.tail match {
        case Nil =>
          NonEmptyList.one(mutations.head)
        case tail =>
          NonEmptyList.fromListUnsafe(tail)
      }
    )
}

object StreamedMutator {

  def io(seed: Array[Byte])(implicit generator: Generator[IO]): StreamedMutator[IO] =
    new StreamedMutator(IO.pure(seed), NonEmptyList.one(RandomBytesMutation.io))

  def seeded[F[_]](seed: F[Array[Byte]], generator: Generator[F]): StreamedMutator[F] =
    new StreamedMutator[F](seed, NonEmptyList.one(new RandomBytesMutation(generator)))

}
