package scalafuzz

import scalafuzz.internals.{IOLoop, Runner}

object Fuzzer {
  type Target = Array[Byte] => Unit

  def run(options: FuzzerOptions, target: Target): FuzzerReport = {
    new Runner(new IOLoop, Log.io).program(options, target).unsafeRunSync()
  }

}
