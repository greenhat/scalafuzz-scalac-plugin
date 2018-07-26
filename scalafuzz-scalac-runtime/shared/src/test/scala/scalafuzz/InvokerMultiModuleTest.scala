package scalafuzz

import scalafuzz.Platform.File

import org.scalatest.{BeforeAndAfter, FunSuite}

/**
 * Verify that [[Invoker.invoked()]] can handle a multi-module project
 */
class InvokerMultiModuleTest extends FunSuite with BeforeAndAfter {

  val measurementDir = Array(
    new File("target/invoker-test.measurement0"),
    new File("target/invoker-test.measurement1")
  )

  before {
    measurementDir.foreach(_.mkdirs())
  }

  test("calling Invoker.invoked on with different directories puts measurements in different directories") {

    val testIds: Set[Int] = (1 to 10).toSet

    testIds.map { i: Int => Invoker.invoked(i, measurementDir(i % 2).toString) }

    // Verify measurements went to correct directory
    val idsFromFile0 = Invoker.invocations()(measurementDir(0).getAbsolutePath).keys.toSet

    idsFromFile0 === testIds.filter { i: Int => i % 2 == 0 }

    val idsFromFile1 = Invoker.invocations()(measurementDir(1).getAbsolutePath).keys.toSet
    idsFromFile1 === testIds.filter { i: Int => i % 2 == 1 }
  }

  after {
    measurementDir.foreach(_.delete)
  }

}
