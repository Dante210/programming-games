using funkylib;
using NUnit.Framework;

namespace Stupid.Tests {
  [TestFixture()]
  public class CardTests {
    [Test]
    public void parseHighCards() {
      var expected = new Card(Suit.Spades, Rank.Ace).some();
      var target = "SA";
      var actual = Card.parse(target);
      Assert.True(expected.Equals(actual));
    }
    [Test]
    public void parseNumberWithTwoChars() {
      var expected = new Card(Suit.Clubs, Rank.Ten).some();
      var target = "CT";
      var actual = Card.parse(target);
      Assert.True(expected.Equals(actual));
    }
  }
}