package scalafuzz

import org.scalatest.FunSuite

class FuzzerTest extends FunSuite {

  test("receiver throws") {
    Fuzzer.run { _ =>
      throw new RuntimeException("catch me")
    }
  }

}
