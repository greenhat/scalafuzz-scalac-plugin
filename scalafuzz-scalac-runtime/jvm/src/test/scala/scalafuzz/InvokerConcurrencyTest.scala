package scalafuzz

import java.io.File
import java.util.concurrent.Executors

import org.scalatest.{BeforeAndAfter, FunSuite}

import scala.collection.breakOut
import scala.concurrent._
import scala.concurrent.duration._

/**
 * Verify that [[Invoker.invoked()]] is thread-safe
 */
class InvokerConcurrencyTest extends FunSuite with BeforeAndAfter {

  implicit val executor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(8))

  val measurementDir = new File("target/invoker-test.measurement")

  before {
    measurementDir.mkdirs()
  }

  test("calling Invoker.invoked on multiple threads does not corrupt the measurement file") {

    val testIds: Set[Int] = (1 to 1000).toSet

    val dirStr = measurementDir.toString
    // Create 1k "invoked" calls on the common thread pool, to stress test
    // the method
    val futures: List[Future[Unit]] = testIds.map { i: Int =>
      Future {
        Invoker.invoked(i, dirStr)
      }
    }(breakOut)

    futures.foreach(Await.result(_, 1.second))

    val idsFromInvoker = Invoker.invocations()(dirStr)
      .toArray
      .map(_.asInstanceOf[Invoker.Invocation].invocationId)
    idsFromInvoker === testIds
  }

  after {
    measurementDir.delete()
  }

}
