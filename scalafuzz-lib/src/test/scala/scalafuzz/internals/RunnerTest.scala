package scalafuzz.internals

import org.scalatest.{FunSuite, Matchers}
import scalafuzz.{FuzzerOptions, Log}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._

class RunnerTest extends FunSuite
  with Matchers {

  implicit val ioGenerator: IOGenerator = new IOGenerator()

  test("target failure add new item to the corpus") {
    val options = FuzzerOptions(
      10.milliseconds,
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

    val corpus = new IOInMemoryCorpus()
    val reports = new Runner(new IOLoop(), corpus, Log.io, new CoverageAnalyzer)
      .program(options, TargetObj.target)
      .unsafeRunSync()

    reports.failures.length shouldBe 1
    reports.newCorpusItems.contains(inputBytesOnFailure)
    corpus.items().length shouldBe 1
  }

  test("newly added(discovered) corpus item is fed to the target later(with mutations)") {
    val options = FuzzerOptions(
      10.milliseconds,
      exitOnFirstFailure = false)
    val expectedRunCountFail = 3
    var hashInputBytesOnFailure = 0
    val inputHashes = new ArrayBuffer[Int]()

    object TargetObj {
      private var count: Int = 0
      def target(bytes: Array[Byte]): Unit = {
        inputHashes.append(bytes.hashCode())
        count = count + 1
        if (count == expectedRunCountFail) {
          hashInputBytesOnFailure = bytes.hashCode()
          throw new RuntimeException("catch me")
        }
      }
    }

    val corpus = new IOInMemoryCorpus()
    val _ = new Runner(new IOLoop(), corpus, Log.io, new CoverageAnalyzer)
      .program(options, TargetObj.target)
      .unsafeRunSync()

    inputHashes.count(_ == hashInputBytesOnFailure) should be >= 2
  }
}
