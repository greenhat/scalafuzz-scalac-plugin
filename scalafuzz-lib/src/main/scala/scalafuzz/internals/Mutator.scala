package scalafuzz.internals

object Mutator {

  sealed trait Mutation
  case object RandomBytes extends Mutation

  // todo generate a stream of mutation descriptions and execute them passing the bytes from the seed
  def mutateBytes(input: Array[Byte], mutation: Mutation): Array[Byte] = Array.fill[Byte](1)(1)
  def randomBytes(): Array[Byte] = Array.fill[Byte](1)(1)

}
