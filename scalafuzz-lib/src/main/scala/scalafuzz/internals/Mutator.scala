package scalafuzz.internals

import cats.data.NonEmptyList
import cats.effect.IO
import scalafuzz.internals.mutations.Mutations.{Mutation, randomBytes}

trait Mutator[F[_]]{
  def mutatedBytes(): F[Array[Byte]]
  def next(input: Array[Byte]): Mutator[F]
}

class StreamedMutator(bytes: IO[Array[Byte]], mutations: NonEmptyList[IO[Mutation]]) extends Mutator[IO] {

  override def mutatedBytes(): IO[Array[Byte]] = bytes

  // todo: extract "endless" list
  override def next(input: Array[Byte]): Mutator[IO] =
    new StreamedMutator(
      mutations.head.map(m => m(input)),
      mutations.tail match {
        case Nil =>
          NonEmptyList.one(mutations.head)
        case tail =>
          NonEmptyList.fromListUnsafe(tail)
      }
    )
}

object StreamedMutator {

  def seed(seed: IO[Array[Byte]]): StreamedMutator =
    new StreamedMutator(seed, NonEmptyList.one(randomBytes))
}
