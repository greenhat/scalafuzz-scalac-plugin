package scalafuzz.internals

import org.scalatest.{FunSuite, Matchers}
import scalafuzz.Invoker.InvocationId
import scalafuzz.internals.TargetRunReportAnalyzer.NoNewCoverage

class TargetRunReportAnalyzerTest extends FunSuite with Matchers {

  test("testProcess") {
    val a = new IOTargetRunReportAnalyzer
    a.process(
      TargetRunReport(Array[Byte](), TargetNormalExit, Seq[InvocationId]())
    ) shouldBe NoNewCoverage

  }

}
