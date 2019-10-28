import org.scalatest.FunSuite

class RankTest extends FunSuite {
  test("Rank.compare") {
    assert(Rank.unsafeApply('2') < Rank.unsafeApply('5'))
  }
}