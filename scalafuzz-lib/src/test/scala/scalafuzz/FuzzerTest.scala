package scalafuzz

import org.scalatest.{FunSuite, Matchers}
import scalafuzz.Fuzzer.{ExceptionFailure, RunOptions, TimedDuration, UntilFailure}

import scala.concurrent.duration._

class FuzzerTest extends FunSuite
  with Matchers {

  test("receiver throws") {
    val options = RunOptions(TimedDuration(3.hours))
    val report = Fuzzer.run(options, { _ =>
      throw new RuntimeException("catch me")
    })
    report.stats.runCount shouldBe 1
//    report.failures.length shouldBe 1
  }

}
