package scalafuzz

import scalafuzz.internals._

object Fuzzer {
  type Target = Array[Byte] => Unit

  def run(options: FuzzerOptions, target: Target): FuzzerReport = {
    new Runner(new IOLoop(),
      new IOCorpus(),
      StreamedMutator.ioSeeded(Array.emptyByteArray),
      Log.io,
      new IOTargetRunReportAnalyzer()).program(options, target).unsafeRunSync()
  }

  def run2(options: FuzzerOptions, target: Target): Seq[FuzzerReport] = {
    new Runner(new IOLoop(),
      new IOCorpus(),
      StreamedMutator.ioSeeded(Array.emptyByteArray),
      Log.io,
      new IOTargetRunReportAnalyzer()).program2(options, target).unsafeRunSync()
  }

}
