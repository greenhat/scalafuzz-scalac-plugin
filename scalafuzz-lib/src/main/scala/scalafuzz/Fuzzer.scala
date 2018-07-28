package scalafuzz

object Fuzzer {

  def run(receiver: Array[Byte] => Unit): Unit = {
    while (true) {
      Invoker.reset()
      val bytes = Array[Byte]()
      try {
        receiver(bytes)
      } catch {
        case e: Exception =>
          return
      }
      val invs = Invoker.invocations()
    }
  }

}
