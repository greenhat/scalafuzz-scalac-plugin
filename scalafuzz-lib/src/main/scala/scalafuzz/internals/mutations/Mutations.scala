package scalafuzz.internals.mutations

object Mutations {

  type Mutation = Array[Byte] => Array[Byte]

  def randomBytes: Mutation = { _ =>
    // todo proper random
    Array.fill[Byte](1)(1)
  }
}


