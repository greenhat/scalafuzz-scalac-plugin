package scalafuzz.internals

import org.scalatest.{FunSuite, Matchers}
import scalafuzz.Invoker.InvocationId
import scalafuzz.internals.CoverageAnalyzer.NoNewCoverage

class CoverageAnalyzerTest extends FunSuite with Matchers {

  test("testProcess") {
    val a = new CoverageAnalyzer
    a.process(
      TargetRunReport(Array[Byte](), TargetNormalExit, Seq[InvocationId]())
    ) shouldBe NoNewCoverage

  }

}
