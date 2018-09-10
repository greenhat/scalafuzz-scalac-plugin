package scalafuzz.internals

import cats.data.NonEmptyList
import scalafuzz.internals.Mutator.{Mutation, RandomBytes}

object Mutator {

  sealed trait Mutation
  case object RandomBytes extends Mutation

}

trait Mutator {
  def mutatedBytes(): Array[Byte]
  def next(input: Array[Byte]): Mutator
}

class StreamMutator(input: Array[Byte], mutations: NonEmptyList[Mutation]) extends Mutator {
  import StreamMutator._

  override def mutatedBytes(): Array[Byte] = input

  override def next(input: Array[Byte]): Mutator =
    new StreamMutator(
      mutate(input, mutations.head),
      mutations.tail match {
        case Nil =>
          NonEmptyList.one(mutations.head)
        case tail =>
          // todo unsafe? meh
          NonEmptyList.fromListUnsafe(tail)
      }
    )
}

object StreamMutator {

  def seedRandom(): StreamMutator =
    new StreamMutator(mutate(new Array[Byte](1), RandomBytes),
      NonEmptyList.one(RandomBytes))

  def mutate(input: Array[Byte], mutation: Mutation): Array[Byte] = mutation match {
    case Mutator.RandomBytes =>
      // todo proper random bytes
      Array.fill[Byte](1)(1)
  }
}
