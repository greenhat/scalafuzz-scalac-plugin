package scalafuzz

import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.duration._

class FuzzerTest extends FunSuite
  with Matchers {

  test("receiver throws") {
    val options = FuzzerOptions(
      TimedDuration(3.hours),
      exitOnFirstFailure = true)
    val report = Fuzzer.run(options, { _ =>
      println("")
      throw new RuntimeException("catch me")
    })
    report.stats.runCount shouldBe 1
    report.failures.length shouldBe 1
  }

}
