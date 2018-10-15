package scalafuzz.internals

import scalafuzz.internals.mutations.{Mutation, RandomBytesMutation}

trait Mutator[F[_]]{
  def mutatedBytes(): F[Array[Byte]]
  def next(input: Array[Byte]): Option[Mutator[F]]
}

class StreamedMutator[F[_]](bytes: F[Array[Byte]], mutations: Seq[Mutation[F]]) extends Mutator[F] {

  override def mutatedBytes(): F[Array[Byte]] = bytes

  override def next(input: Array[Byte]): Option[Mutator[F]] = mutations match {
    case Nil => None
    case h :: t => Some(new StreamedMutator(h.mutate(input), t))
  }
}

object StreamedMutator {
  // todo run deterministic mutations (see afl);
  // todo run fixed number of stacked deterministic and random mutations (see afl and libfuzzer);

  def seeded[F[_]](seed: F[Array[Byte]])(implicit generator: Generator[F]): StreamedMutator[F] =
    new StreamedMutator[F](seed, Seq(RandomBytesMutation()))

}
