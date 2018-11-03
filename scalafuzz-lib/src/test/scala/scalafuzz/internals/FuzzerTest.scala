package scalafuzz.internals

import java.util

import org.scalatest.{FunSuite, Matchers}
import scalafuzz.{Fuzzer, FuzzerOptions}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._

class FuzzerTest extends FunSuite
  with Matchers {

  test("receiver throws on the predetermined run") {
    val options = FuzzerOptions(
      3.hours,
      exitOnFirstFailure = true)
    val expectedRunCountFail = 222

    object TargetObj {
      private var count: Int = 1
      def target(bytes: Array[Byte]): Unit = {
        if (count == expectedRunCountFail)
          throw new RuntimeException("catch me")
        else
//          println(bytes)
          count = count + 1
      }
    }

    val reports = Fuzzer.run(options, TargetObj.target)
    reports.flatMap(_.failures).length shouldBe 1
    reports.map(_.stats.runCount).sum shouldBe expectedRunCountFail
  }

  test("empty input on the first run") {
    val options = FuzzerOptions(
      3.hours,
      exitOnFirstFailure = true)
    val reports = Fuzzer.run(options, { bytes =>
      bytes.isEmpty shouldBe true
      throw new RuntimeException("catch me")
    })
    reports.flatMap(_.failures).length shouldBe 1
    reports.map(_.stats.runCount).sum shouldBe 1
  }

  test("tests that inputs are unique") {
    val options = FuzzerOptions(
      1.second,
      exitOnFirstFailure = true)
    var inputHashes = new ArrayBuffer[Int]()

    val _ = Fuzzer.run(options, { bytes =>
      inputHashes += util.Arrays.hashCode(bytes)
    })

    inputHashes.toSet.size shouldBe inputHashes.size
  }
}
