package scalafuzz.internals

import org.scalatest.{FunSuite, Matchers}
import scalafuzz.Invoker.InvocationId
import scalafuzz.internals.CoverageAnalyzer.{NewCoverage, NoNewCoverage}

class CoverageAnalyzerTest extends FunSuite with Matchers {

  test("process - no stored coverages") {
    val a = new CoverageAnalyzer()
    val input1 = Array.fill[Byte](32)(1)
    val coverage1 = Seq[InvocationId](1,2,3)
    a.process(
      TargetRunReport(input1, TargetNormalExit, coverage1)
    ) shouldBe NewCoverage(input1)
  }

  test("process - already in stored coverages") {
    val a = new CoverageAnalyzer()
    val input1 = Array.fill[Byte](32)(1)
    val coverage1 = Seq[InvocationId](1,2,3)
    a.process(
      TargetRunReport(input1, TargetNormalExit, coverage1)
    ) shouldBe NewCoverage(input1)

    val input2 = Array.fill[Byte](32)(2)
    a.process(
      TargetRunReport(input2, TargetNormalExit, coverage1)
    ) shouldBe NoNewCoverage
  }

  test("process - new coverage, not in stored coverages") {
    val a = new CoverageAnalyzer()
    val input1 = Array.fill[Byte](32)(1)
    val coverage1 = Seq[InvocationId](1,2,3)
    a.process(
      TargetRunReport(input1, TargetNormalExit, coverage1)
    ) shouldBe NewCoverage(input1)

    val input2 = Array.fill[Byte](32)(2)
    val coverage2 = Seq[InvocationId](1,2,3,4)
    a.process(
      TargetRunReport(input2, TargetNormalExit, coverage2)
    ) shouldBe NewCoverage(input2)
  }
}
