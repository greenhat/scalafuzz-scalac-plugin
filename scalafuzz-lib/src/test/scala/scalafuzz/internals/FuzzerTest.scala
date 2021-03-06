package scalafuzz.internals

import java.security.MessageDigest
import java.util

import org.scalatest.{FunSuite, Matchers}
import scalafuzz.{Fuzzer, FuzzerOptions}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._

class FuzzerTest extends FunSuite
  with Matchers {

  private def md5(b: Array[Byte]): Array[Byte] = {
    MessageDigest.getInstance("MD5").digest(b)
  }

  private def time(block: => Any): Long = {
    val t0 = System.nanoTime()
    block
    val t1 = System.nanoTime()
    t1 - t0
  }

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
    reports.failures.length shouldBe 1
    reports.stats.runCount shouldBe expectedRunCountFail
  }

  test("empty input on the first run") {
    val options = FuzzerOptions(
      3.hours,
      exitOnFirstFailure = true)
    val reports = Fuzzer.run(options, { bytes =>
      bytes.isEmpty shouldBe true
      throw new RuntimeException("catch me")
    })
    reports.failures.length shouldBe 1
    reports.stats.runCount shouldBe 1
  }

  test("most inputs are unique") {
    val timeToRun = 200.milliseconds
    val options = FuzzerOptions(
      timeToRun,
      exitOnFirstFailure = true)
    var inputHashes = new ArrayBuffer[Int]()

    Fuzzer.run(options, { bytes =>
      inputHashes += util.Arrays.hashCode(bytes)
    })

    val nonUniqueInputsNum = inputHashes.size - inputHashes.toSet.size
    (nonUniqueInputsNum.toDouble / inputHashes.size.toDouble) should be < 0.01
  }

  test("fits in timeToRun") {
    val timeToRun = 1.second
    val options = FuzzerOptions(
      timeToRun,
      exitOnFirstFailure = false)
    var inputHashes = new ArrayBuffer[Int]()

    time {
      Fuzzer.run(options, { bytes =>
        val processedBytes = (1 to 10000).foldLeft(bytes)((acc, _) => md5(acc))
        inputHashes += util.Arrays.hashCode(processedBytes)
      })
    }.toDouble / timeToRun.toNanos.toDouble should be <= 1.3

    val nonUniqueInputsNum = inputHashes.size - inputHashes.toSet.size
    (nonUniqueInputsNum.toDouble / inputHashes.size.toDouble) should be < 0.01
  }

  test("target failure produce new corpus item") {
    val options = FuzzerOptions(
      100.milliseconds,
      exitOnFirstFailure = false)
    val expectedRunCountFail = 3
    var inputBytesOnFailure = Array[Byte]()

    object TargetObj {
      private var count: Int = 0
      def target(bytes: Array[Byte]): Unit = {
        count = count + 1
        if (count == expectedRunCountFail) {
          inputBytesOnFailure = bytes
          throw new RuntimeException("catch me")
        }
      }
    }

    val reports = Fuzzer.run(options, TargetObj.target)
    reports.failures.length shouldBe 1
    reports.newCorpusItems.contains(inputBytesOnFailure)
  }

  // todo test target throwing on every input
}
