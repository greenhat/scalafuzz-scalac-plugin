package scalafuzz

import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.duration._

class FuzzerTest extends FunSuite
  with Matchers {

  test("receiver throws on the second run") {
    val options = FuzzerOptions(
      TimedDuration(3.hours),
      exitOnFirstFailure = true)

    val expectedRunCountFail = 222

    object TargetObj {
      private var count: Int = 1
      def target(bytes: Array[Byte]): Unit = {
        if (count == 1) {
          // test empty input on first run
          bytes.isEmpty shouldBe true
        }
        if (count == expectedRunCountFail)
          throw new RuntimeException("catch me")
        else
//          println(bytes)
          count = count + 1
      }
    }

    val report = Fuzzer.run(options, TargetObj.target)
    report.failures.length shouldBe 1
    report.stats.runCount shouldBe expectedRunCountFail
  }

}
