package scalafuzz

import scalafuzz.internals._

object Fuzzer {
  type Target = Array[Byte] => Unit

  implicit val ioGenerator: IOGenerator = new IOGenerator()

  def run(options: FuzzerOptions, target: Target): FuzzerReport = {
    val corpus = new IOInMemoryCorpus()
    new Runner(new IOLoop(),
      corpus,
      Log.io,
      new CoverageAnalyzer).program(options, target).unsafeRunSync()
  }

}
