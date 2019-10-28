import org.scalatest.FunSuite

class MainTest extends FunSuite {
  implicit val log: Option[GameStateLog] = None
  test("Main.runGame_example1") {
    assert(Main.runGame("example1.txt") === "1")
  }

  test("Main.runGame_example2") {
    assert(Main.runGame("example2.txt") === "20")
  }

  test("Main.runGame_test1") {
    assert(Main.runGame("test1.txt") === "1")
  }

  test("Main.runGame_bad_data") {
    assert(Main.runGame("bad_data.txt").contains("ERRORS WHILE PARSING"))
  }

  test("Main.runGame_data") {
    assert(
      Main.runGame("data.txt") ===
      "1211112121221102212111211012111211111111211211112112111112111111121211111111202222011111011111112111"
    )
  }
}