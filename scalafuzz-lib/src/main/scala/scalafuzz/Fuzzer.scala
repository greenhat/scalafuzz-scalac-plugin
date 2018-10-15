package scalafuzz

import scalafuzz.internals._

object Fuzzer {
  type Target = Array[Byte] => Unit

  implicit val ioGenerator: IOGenerator = new IOGenerator()

  def run(options: FuzzerOptions, target: Target): Seq[FuzzerReport] = {
    new Runner(new IOLoop(),
      new IOCorpus(),
      Log.io,
      new IOTargetRunReportAnalyzer()).program(options, target).unsafeRunSync()
  }

}
