package scalafuzz.internals

import org.scalatest.{FunSuite, Matchers}

class CorpusTest extends FunSuite with Matchers {

  test("empty corpus") {
    val c = new IOInMemoryCorpus
    c.items().isEmpty shouldBe true
  }

  test("add input") {
    val c = new IOInMemoryCorpus
    val input = Array.fill[Byte](32)(1)
    c.add(Seq(input)).unsafeRunSync()
    c.items().nonEmpty shouldBe true
    c.items().map(_.unsafeRunSync()).head shouldEqual input
    val added = c.addedAfterLastCall
    added.nonEmpty shouldBe true
    c.addedAfterLastCall.isEmpty shouldBe true
    added.map(_.unsafeRunSync()).head shouldEqual input
  }
}
