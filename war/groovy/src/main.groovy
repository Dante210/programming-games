import groovy.transform.EqualsAndHashCode
import groovy.transform.Immutable

import java.util.function.Consumer
import java.util.function.Function

// Todo 1. Add more test cases
// Todo 2. Implement writing results to the file

class Program {
  enum LogLevel { Info, Critical }
  static LogLevel logLevel = LogLevel.Info

  static void log(LogLevel level = LogLevel.Info, String msg) {
    if (logLevel == LogLevel.Info) println(msg)
    else if (logLevel == LogLevel.Critical && level == LogLevel.Critical) println(msg)
  }

  static <A,B,C> Either<A, B> optToEither(Optional<C> opt, Closure<A> onNone, Function<C, B> onSome) {
    if (opt.present) return new Right<A, B>(onSome.apply(opt.get()))
    return new Left<A, B>(onNone())
  }

  static void main(String[] args) {
    Random random = new Random()
    Suite trumpSuite = getRandomSuite random
    log("trumpSuite: " + trumpSuite)

    List<Card> shuffledDeck = shuffleDeckImmutable generateStandardDeck()

    List<List<Card>> splittedLists = shuffledDeck.collate (Math.round(shuffledDeck.size() / 2) as int)
    Player firstPlayer = Player.create("First player", splittedLists[0])
    Player secondPlayer = Player.create("Second player", splittedLists[1])

    while (firstPlayer.haveCardsToBePlayed() && secondPlayer.haveCardsToBePlayed()) {
      playTurn(firstPlayer, secondPlayer, trumpSuite).voidFold(
        { errorMsg -> log(errorMsg.message) },
        { io -> io.unsafeRun()}
      )
    }

    Player winner = firstPlayer.cardsCountInScoringPile() > secondPlayer.cardsCountInScoringPile()
      ? firstPlayer
      : secondPlayer

    log("The winner is ${winner.getName()}")
  }

  static Either<ErrorMsg, IO<Unit>> playTurn(Player firstPlayer, Player secondPlayer, Suite trumpSuite) {
    def firstPlayerEither = optToEither(
      firstPlayer.drawCard(),
      { new ErrorMsg("Playing turn, but first player does have a card") },
      { card -> card }
    )

    def secondPlayerEither = optToEither(
      secondPlayer.drawCard(),
      { new ErrorMsg("Playing turn, but second player does have a card") },
      { card -> card }
    )

    return firstPlayerEither.flatmapRight(
      { firstPlayerCard ->
        secondPlayerEither.mapRight(
          { secondPlayerCard ->
            new IO<Unit> ({
              log("--------------------Start of the turn------------------------")
              log("firstPlayerCard: " + firstPlayerCard)
              log("secondPlayerCard: " + secondPlayerCard)

              Optional<Card> winningCardOpt = getWinningCard(firstPlayerCard, secondPlayerCard, trumpSuite)
              log("Wining Card: " + winningCardOpt)

              if (winningCardOpt.present) {
                if (firstPlayerCard == winningCardOpt.get()) {
                  firstPlayer.addCardToScoringPile(firstPlayerCard)
                  firstPlayer.addCardToScoringPile(secondPlayerCard)
                } else {
                  secondPlayer.addCardToScoringPile(firstPlayerCard)
                  secondPlayer.addCardToScoringPile(secondPlayerCard)
                }
              } else {
                firstPlayer.addCardToScoringPile(firstPlayerCard)
                secondPlayer.addCardToScoringPile(secondPlayerCard)
              }

              log(LogLevel.Info, "firstPlayer: " + firstPlayer)
              log(LogLevel.Info, "secondPlayer: " + secondPlayer)
              log("--------------------End of the turn--------------------------")
              return new Unit()
            })
          }
        )
      }
    )
  }

  static Optional<Card> getWinningCardByRank(Card firstPlayerCard, Card secondPlayerCard) {
    if (firstPlayerCard.rank == secondPlayerCard.rank) return Optional.empty()
    Card card = firstPlayerCard.rank > secondPlayerCard.rank ? firstPlayerCard : secondPlayerCard
    return Optional.of(card)
  }

  static Optional<Card> getWinningCard(Card firstPlayerCard, Card secondPlayerCard, Suite trumpSuite) {
    if (firstPlayerCard.suite == trumpSuite && secondPlayerCard.suite == trumpSuite) {
      return getWinningCardByRank(firstPlayerCard, secondPlayerCard)
    }
    else if (firstPlayerCard.suite == trumpSuite) {
      return Optional.of(firstPlayerCard)
    }
    else if (secondPlayerCard.suite == trumpSuite) {
      return Optional.of(secondPlayerCard)
    }
    else
      getWinningCardByRank(firstPlayerCard, secondPlayerCard)
  }

  static Suite getRandomSuite(Random random) {
    Suite.values()[random.nextInt(Suite.values().length)]
  }

  static List<Card> generateStandardDeck() {
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

@EqualsAndHashCode
@Immutable class Card {
  Suite suite
  Rank  rank

  @Override
  String toString() { asString() }

  String asString() {
    "$suite $rank"
  }
}

class Player {
  private String name
  private Stack<Card> cardsToBePlayed
  private Stack<Card> scoringPile

  private Player(String name, Stack<Card> cardsToBePlayed, Stack<Card> scoringPile) {
    this.name = name
    this.cardsToBePlayed = cardsToBePlayed
    this.scoringPile = scoringPile
  }

  String getName() { name }

  static Player create (String name, List<Card> cardsToBePlayed) {
    Stack<Card> toBePlayed = new Stack<>()
    cardsToBePlayed.each { toBePlayed.push it }

    return new Player(name, toBePlayed, new Stack<Card>())
  }

  Optional<Card> drawCard() {
    if (cardsToBePlayed.size() > 0)
      return Optional.of(cardsToBePlayed.pop())
    else
      return Optional.empty()
  }

  void addCardToScoringPile(Card card) { scoringPile.push(card) }
  int cardsCountInScoringPile() { scoringPile.size() }
  boolean haveCardsToBePlayed() { cardsToBePlayed.size() > 0 }

  @Override
  String toString() { asString() }

  String asString() {
    "Cards to be played: ${cardsToBePlayed.size()} \n" +
    "Scoring pile: ${ scoringPile }"
  }
}

@Immutable class ErrorMsg {
  String message
}

interface Either<A, B> {
  def <C> C fold(Function<A, C> onLeft, Function<B, C> onRight)
  void voidFold(Consumer<A> onLeft, Consumer<B> onRight)
  def <AA, BB> Either<AA, BB> map(Function<A, AA> onLeft, Function<B, BB> onRight)
  def <BB> Either<A, BB> flatmapRight(Function<B, Either<A, BB>> fn)
  def <BB> Either<A, BB> mapRight(Function<B, BB> fn)
}

class Left<A, B> implements Either<A, B> {
  Left(A a) { this.a = a }
  final A a

  @Override
  <C> C fold(Function<A, C> onLeft, Function<B, C> onRight) { onLeft.apply(a) }

  @Override
  void voidFold(Consumer<A> onLeft, Consumer<B> onRight) { onLeft.accept(a) }

  @Override
  <AA, BB> Either<AA,BB> map(Function<A, AA> onLeft, Function<B, BB> onRight) {
    new Left<AA, BB>(onLeft.apply(a))
  }

  @Override
  <BB> Either<A,BB> flatmapRight(Function<B, Either<A, BB>> fn) {
    new Left<A, BB>(a)
  }

  @Override
  <BB> Either<A,BB> mapRight(Function<B, BB> fn) {
    new Left<A, BB>(a)
  }
}

class Right<A, B> implements Either<A, B> {
  Right(B b) { this.b = b }
  final B b

  @Override
  <C> C fold(Function<A, C> onLeft, Function<B, C> onRight) { onRight.apply(b) }

  @Override
  void voidFold(Consumer<A> onLeft, Consumer<B> onRight) { onRight.accept(b) }

  @Override
  <AA, BB> Either<AA,BB> map(Function<A, AA> onLeft, Function<B, BB> onRight) {
    new Right<AA, BB>(onRight.apply(b))
  }

  @Override
  <BB> Either<A,BB> flatmapRight(Function<B, Either<A, BB>> fn) {
    fn.apply(b)
  }

  @Override
  <BB> Either<A,BB> mapRight(Function<B, BB> fn) {
    new Right<A, BB>(fn.apply(b))
  }
}

class Unit { }

class IO<A> {
  Closure<A> fn

  IO(Closure<A> fn) { this.fn = fn }

  IO<A> andThen(Closure<A> fn) {
    this.fn()
    return new IO<A>(fn)
  }

  void unsafeRun() { fn() }
}

