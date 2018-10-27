package scalafuzz

import scalafuzz.internals._

object Fuzzer {
  type Target = Array[Byte] => Unit

  implicit val ioGenerator: IOGenerator = new IOGenerator()

  def run(options: FuzzerOptions, target: Target): Seq[FuzzerReport] = {
    val corpus = new IOCorpus()
    new Runner(new IOLoop(),
      corpus,
      Log.io,
      new IOTargetRunReportAnalyzer).program(options, target).unsafeRunSync()
  }

}
