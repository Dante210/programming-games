class ProgramTest extends GroovyTestCase {
  void testGenerateStandardDeck() {
    List<Card> generatedDeck = Program.generateStandardDeck()
    List<Card> expectedDeck = [
        new Card(suite: Suite.Hearths, rank: Rank.Two),
        new Card(suite: Suite.Hearths, rank: Rank.Three),
        new Card(suite: Suite.Hearths, rank: Rank.Four),
        new Card(suite: Suite.Hearths, rank: Rank.Five),
        new Card(suite: Suite.Hearths, rank: Rank.Six),
        new Card(suite: Suite.Hearths, rank: Rank.Seven),
        new Card(suite: Suite.Hearths, rank: Rank.Eight),
        new Card(suite: Suite.Hearths, rank: Rank.Nine),
        new Card(suite: Suite.Hearths, rank: Rank.Ten),
        new Card(suite: Suite.Hearths, rank: Rank.Jack),
        new Card(suite: Suite.Hearths, rank: Rank.Queen),
        new Card(suite: Suite.Hearths, rank: Rank.King),
        new Card(suite: Suite.Hearths, rank: Rank.Ace),

        new Card(suite: Suite.Spades, rank: Rank.Two),
        new Card(suite: Suite.Spades, rank: Rank.Three),
        new Card(suite: Suite.Spades, rank: Rank.Four),
        new Card(suite: Suite.Spades, rank: Rank.Five),
        new Card(suite: Suite.Spades, rank: Rank.Six),
        new Card(suite: Suite.Spades, rank: Rank.Seven),
        new Card(suite: Suite.Spades, rank: Rank.Eight),
        new Card(suite: Suite.Spades, rank: Rank.Nine),
        new Card(suite: Suite.Spades, rank: Rank.Ten),
        new Card(suite: Suite.Spades, rank: Rank.Jack),
        new Card(suite: Suite.Spades, rank: Rank.Queen),
        new Card(suite: Suite.Spades, rank: Rank.King),
        new Card(suite: Suite.Spades, rank: Rank.Ace),

        new Card(suite: Suite.Clubs, rank: Rank.Two),
        new Card(suite: Suite.Clubs, rank: Rank.Three),
        new Card(suite: Suite.Clubs, rank: Rank.Four),
        new Card(suite: Suite.Clubs, rank: Rank.Five),
        new Card(suite: Suite.Clubs, rank: Rank.Six),
        new Card(suite: Suite.Clubs, rank: Rank.Seven),
        new Card(suite: Suite.Clubs, rank: Rank.Eight),
        new Card(suite: Suite.Clubs, rank: Rank.Nine),
        new Card(suite: Suite.Clubs, rank: Rank.Ten),
        new Card(suite: Suite.Clubs, rank: Rank.Jack),
        new Card(suite: Suite.Clubs, rank: Rank.Queen),
        new Card(suite: Suite.Clubs, rank: Rank.King),
        new Card(suite: Suite.Clubs, rank: Rank.Ace),

        new Card(suite: Suite.Diamonds, rank: Rank.Two),
        new Card(suite: Suite.Diamonds, rank: Rank.Three),
        new Card(suite: Suite.Diamonds, rank: Rank.Four),
        new Card(suite: Suite.Diamonds, rank: Rank.Five),
        new Card(suite: Suite.Diamonds, rank: Rank.Six),
        new Card(suite: Suite.Diamonds, rank: Rank.Seven),
        new Card(suite: Suite.Diamonds, rank: Rank.Eight),
        new Card(suite: Suite.Diamonds, rank: Rank.Nine),
        new Card(suite: Suite.Diamonds, rank: Rank.Ten),
        new Card(suite: Suite.Diamonds, rank: Rank.Jack),
        new Card(suite: Suite.Diamonds, rank: Rank.Queen),
        new Card(suite: Suite.Diamonds, rank: Rank.King),
        new Card(suite: Suite.Diamonds, rank: Rank.Ace),
    ]

    assert (generatedDeck as Set) == (expectedDeck as Set)
  }
}
