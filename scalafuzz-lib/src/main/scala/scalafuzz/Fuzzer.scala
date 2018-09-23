package scalafuzz

import cats.effect.IO
import scalafuzz.internals.{IOLoop, Runner, SyncTargetRunReportAnalyzer}

object Fuzzer {
  type Target = Array[Byte] => Unit

  def run(options: FuzzerOptions, target: Target): FuzzerReport = {
    new Runner(new IOLoop[IO],
      Log.io,
      new SyncTargetRunReportAnalyzer[IO]()).program(options, target).unsafeRunSync()
  }

}
