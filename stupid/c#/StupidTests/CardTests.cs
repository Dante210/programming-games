using NUnit.Framework;
using pzd.lib.functional;

namespace Stupid.Tests {
  [TestFixture()]
  public class CardTests {
    [Test]
    public void parseHighCards() {
      var expected = new Option<Card>(new Card(Suit.Spades, Rank.Ace));
      var target = "SA";
      var actual = Card.parse(target);
      Assert.True(expected.Equals(actual));
    }
    [Test]
    public void parseNumberWithTwoChars() {
      var expected = new Option<Card>(new Card(Suit.Clubs, Rank.Ten));
      var target = "CT";
      var actual = Card.parse(target);
      Assert.True(expected.Equals(actual));
    }
  }
}