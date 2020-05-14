package ui

import utest._

object UITest extends TestSuite {
  def tests: Tests =
    Tests {
      test("HelloWorld") {
        assert(1 == 1)
      }
    }
}
