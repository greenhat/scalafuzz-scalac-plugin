package scalafuzz.internals

import cats.data.NonEmptyList
import scalafuzz.internals.Mutator.{Mutation, RandomBytes}

object Mutator {

  sealed trait Mutation
  case object RandomBytes extends Mutation

  // todo generate a stream of mutation descriptions and execute them passing the bytes from the seed
}

trait Mutator {
  def mutatedBytes(): Array[Byte]
  def next(input: Array[Byte]): Mutator
}

class StreamMutator(input: Array[Byte], mutations: NonEmptyList[Mutation]) extends Mutator {
  import StreamMutator._

  override def mutatedBytes(): Array[Byte] = input

  override def next(input: Array[Byte]): Mutator =
    new StreamMutator(applyMutation(input, mutations.head),
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
    new StreamMutator(applyMutation(new Array[Byte](1), RandomBytes),
      NonEmptyList.one(RandomBytes))

  def applyMutation(input: Array[Byte], mutation: Mutation): Array[Byte] = mutation match {
    case Mutator.RandomBytes =>
      // todo proper random bytes
      Array.fill[Byte](1)(1)
  }
}
