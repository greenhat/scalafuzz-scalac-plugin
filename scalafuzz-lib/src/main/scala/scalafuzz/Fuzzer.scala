package scalafuzz

import scalafuzz.internals.{IOLoop, IOTargetRunReportAnalyzer, Runner, StreamedMutator}

object Fuzzer {
  type Target = Array[Byte] => Unit

  def run(options: FuzzerOptions, target: Target): FuzzerReport = {
    new Runner(new IOLoop(),
      StreamedMutator.seed(Array.emptyByteArray),
      Log.io,
      new IOTargetRunReportAnalyzer()).program(options, target).unsafeRunSync()
  }

}
