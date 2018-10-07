import jdk.nashorn.internal.ir.annotations.Immutable

class Program {
  static void main(String[] args) {
    Random random = new Random()
    println "Random suite is : ${ getRandomSuite random }"

    List<Card> shuffledDeck = shuffleDeckImmutable generateDeck()
    List<List<Card>> splittedLists = shuffledDeck.collate 2

    Player firstPlayer = new Player(splittedLists[0], [])
    Player secondPlayer = new Player(splittedLists[1], [])

    println firstPlayer.asString()
  }

  static Suite getRandomSuite(Random random) {
    Suite.values()[random.nextInt(Suite.values().length)]
  }

  static List<Card> generateDeck() {
    List<Card> cards = []
    for (Rank r : Rank.values()) {
      for (Suite s : Suite.values()) {
        cards.add(new Card(suite: s, rank: r))
      }
    }
    return cards
  }

  static List<Card> shuffleDeckImmutable(List<Card> cards) {
    def temp = []
    temp.addAll(cards)
    Collections.shuffle(temp)
    return temp
  }
}

enum Suite { Hearths, Spades, Clubs, Diamonds }
enum Rank { Two, Three, Four, Five, Six, Seven, Eight, Nine, Ten, Jack, Queen, King, Ace }

@Immutable class Card {
  Suite suite
  Rank rank

  String asString() {
    "$suite $rank"
  }
}

@Immutable class Player {
  List<Card> cardsToBePlayed
  List<Card> scoringPile

  Player(List<Card> cardsToBePlayed, List<Card> scoringPile) {
    this.cardsToBePlayed = cardsToBePlayed.asImmutable()
    this.scoringPile = scoringPile.asImmutable()
  }

  String asString() {
    "Cards to be played: ${cardsToBePlayed.size()} \n" +
    "Scoring pile: ${scoringPile.size()}"
  }
}

