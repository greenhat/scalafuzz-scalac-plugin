package scalafuzz

object Fuzzer {
  import internals.Runner._

  type Target = Array[Byte] => Unit

  def run(options: FuzzerOptions, target: Target): FuzzerReport = {
    program(options, target).unsafeRunSync()
  }

}
