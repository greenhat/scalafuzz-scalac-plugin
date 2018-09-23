package scalafuzz.internals

import cats.data.NonEmptyList
import scalafuzz.internals.Mutations.{Mutation, randomBytes}

object Mutations {

  type Mutation = Array[Byte] => Array[Byte]

  def randomBytes: Mutation = { _ =>
    // todo proper random
    Array.fill[Byte](1)(1)
  }
}

trait Mutator {
  def mutatedBytes(): Array[Byte]
  def next(input: Array[Byte]): Mutator
}

class StreamedMutator(input: Array[Byte], mutations: NonEmptyList[Mutation]) extends Mutator {

  override def mutatedBytes(): Array[Byte] = input

  override def next(input: Array[Byte]): Mutator =
    new StreamedMutator(
      mutations.head(input),
      mutations.tail match {
        case Nil =>
          NonEmptyList.one(mutations.head)
        case tail =>
          NonEmptyList.fromListUnsafe(tail)
      }
    )
}

object StreamedMutator {

  def seedRandom(): StreamedMutator =
    new StreamedMutator(randomBytes(new Array[Byte](1)), NonEmptyList.one(randomBytes))
}
