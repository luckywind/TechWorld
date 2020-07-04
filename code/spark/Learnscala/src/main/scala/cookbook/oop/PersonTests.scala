package cookbook.oop


import org.scalatest.FunSuite
class PersonTests extends FunSuite {
  // these first two instances should be equal
  val nimoy = new Person("Leonard Nimoy", 82)
  val nimoy2 = new Person("Leonard Nimoy", 82)
  val shatner = new Person("William Shatner", 82)
  val ed = new Person("Ed Chigliak", 20)
  // all tests pass
  test("nimoy == nimoy") { assert(nimoy == nimoy) }
  test("nimoy == nimoy2") { assert(nimoy == nimoy2) }
  test("nimoy2 == nimoy") { assert(nimoy2 == nimoy) }
  test("nimoy != shatner") { assert(nimoy != shatner) }
  test("shatner != nimoy") { assert(shatner != nimoy) }
  test("nimoy != null") { assert(nimoy != null) }
  test("nimoy != String") { assert(nimoy != "Leonard Nimoy") }
  test("nimoy != ed") { assert(nimoy != ed) }
}