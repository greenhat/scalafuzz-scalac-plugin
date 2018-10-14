package scalafuzz.internals

import cats.Functor
import cats.data.NonEmptyList
import cats.effect.IO
import cats.syntax.functor._
import scalafuzz.internals.mutations.Mutations.{Mutation, randomBytes}

trait Mutator[F[_]]{
  def mutatedBytes(): F[Array[Byte]]
  def next(input: Array[Byte]): Mutator[F]
}

class StreamedMutator[F[_] : Functor](bytes: F[Array[Byte]], mutations: NonEmptyList[F[Mutation]]) extends Mutator[F] {

  override def mutatedBytes(): F[Array[Byte]] = bytes

  // todo: extract "endless" list
  override def next(input: Array[Byte]): Mutator[F] =
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

  def ioSeeded(seed: Array[Byte]): StreamedMutator[IO] =
    new StreamedMutator(IO.pure(seed), NonEmptyList.one(randomBytes))

  // todo parametrize randomBytes with effect (F)?
  def seeded[F[_]](seed: F[Array[Byte]]): StreamedMutator[F] = ???

}
