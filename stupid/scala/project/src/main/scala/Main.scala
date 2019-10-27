import cats.data.Validated
import cats.data.Validated._
import cats.implicits._

import scala.collection.SortedSet

class Suite private (val suite : Char) extends Ordered[Suite] {
  override def compare(that: Suite): Int = {
    val indexedSeq = Suite.validOrderedSuites.toIndexedSeq
    indexedSeq.indexOf(suite) - indexedSeq.indexOf(that.suite)
  }
  override def equals(o: Any): Boolean = o match {
    case that: Suite => compare(that) == 0
    case _ => false
  }
  override def toString: String = s"suite = $suite"
}
object Suite {
  val validOrderedSuites: Set[Char] = Set(
    'H', // Hearths
    'D', // Diamonds
    'C', // Clubs
    'S'  // Spades
  )

  def apply(suite: Char): Validated[ErrorMsg, Suite] = {
    if (validOrderedSuites.contains(suite)) Valid(new Suite(suite))
    else Invalid(ErrorMsg(s"$suite is not valid field for ${Suite.getClass.getName}"))
  }

  def unsafeApply(suite: Char): Suite = new Suite(suite)
}

class Rank private (val rank : Char) extends Ordered[Rank] {
  override def compare(that: Rank): Int = {
    val indexedSeq = Rank.validOrderedRanks.toIndexedSeq
    indexedSeq.indexOf(rank) - indexedSeq.indexOf(that.rank)
  }

  override def equals(o: Any): Boolean = o match {
    case that: Rank => compare(that) == 0
    case _ => false
  }

  override def toString: String = s"rank = $rank"
}
object Rank {
  val validOrderedRanks: Set[Char] = Set(
    '2', '3', '4', '5', '6', '7', '8', '9',
    'T', // 10
    'J', // Jack
    'Q', // Queen,
    'K', // King,
    'A'  // Ace
  )

  def apply(rank : Char) : Validated[ErrorMsg, Rank] = {
    if (validOrderedRanks.contains(rank)) Valid(new Rank(rank))
    else Invalid(ErrorMsg(s"$rank is not valid field for ${Rank.getClass.getName}"))
  }

  def unsafeApply(rank: Char): Rank = new Rank(rank)
}

case class Card(suite: Suite, rank: Rank) extends Ordered[Card] {
  override def compare(that: Card): Int = {
    val rank = this.rank.compare(that.rank)
    if (rank == 0) this.suite.compare(that.suite) else rank
  }
}

case class Cards private(value: SortedSet[Card]) {
  def cardToAttack : Option[Card] = this.value.headOption
  def cardToReflect(rankToReflect: Rank): Option[Card] = this.value.find(card => card.rank == rankToReflect)
  def cardToDefend(cardNeededToDefeat: Card): Option[Card] = this.value.find(card => card > cardNeededToDefeat)

  def withCards(fn : SortedSet[Card] => SortedSet[Card]): Cards = this.copy(value = fn(value))
  def removeCard(cardToRemove: Card): Cards = withCards(cards => value.diff(Set(cardToRemove)))
}
object Cards {
  def apply(cards: Set[Card])(implicit trumpSuite: Suite): Cards = {
    implicit val orderByTrumpSuite: Ordering[Card] =
      Ordering
        .by[Card, Boolean](card => card.suite == trumpSuite)
        .orElse(Ordering.by[Card, Rank](c => c.rank))
        .orElse(Ordering.by[Card, Suite](c => c.suite))

    new Cards(SortedSet.empty ++ cards)
  }
}

case class CardPair(placed: Card, covering: Option[Card])
case class Player(id: Int, cards: Cards) {
  def withCards(fn: Cards => Cards): Player = this.copy(cards = fn(cards))
}

case class GameState(offense: Player, defense: Player, cardsOnTable: Set[CardPair])(implicit trumpSuite: Suite) {
  private def withCardsOnTable(fn: Set[CardPair] => Set[CardPair]) = this.copy(cardsOnTable = fn(cardsOnTable))
  private def withOffense(fn: Player => Player) = this.copy(offense = fn(offense))
  private def withDefense(fn: Player => Player) = this.copy(defense = fn(defense))

  private def addCardsToTable(cardsToAdd: Iterable[Card]) =
    withCardsOnTable(onTable => onTable ++ cardsToAdd.map(c => CardPair(c, None)))

  private def getCardsToDefeat: Option[Cards] = {
    val cards = Cards.apply(this.cardsOnTable.filter(pair => pair.covering.isEmpty).map(pair => pair.placed))
    if (cards.value.isEmpty) None else cards.some
  }

  private def canTryToReflect(rankToCheckForReflect: Rank) = {
    getCardsToDefeat match {
      case Some(cards) =>
        cards.value.forall(card => card.rank == rankToCheckForReflect) && this.offense.cards.value.size > cards.value.size
      case None => false
    }
  }

  private def getAllCardsOnTable =
    cardsOnTable.flatMap(pair => pair.covering.map(c => c).toList ++ List(pair.placed))

  def swapRoles: GameState =
    this
      .withOffense(_ => defense)
      .withDefense(_ => offense)

  def noCardsLeftToPlay: Boolean = cardsOnTable.isEmpty && (offense.cards.value.isEmpty || defense.cards.value.isEmpty)

  def tryGetReflectCard(rankToCheckForReflect: Rank): Option[Card] = {
    if (canTryToReflect(rankToCheckForReflect)) this.defense.cards.cardToReflect(rankToCheckForReflect)
    else None
  }

  def getCardToDefeat: Option[Card] = this.getCardsToDefeat.map(c => c.value.head)

  def determineWinner: Option[Player] = {
    val offenseCards = offense.cards.value.size
    val defenseCards = defense.cards.value.size
    if (offenseCards > defenseCards) offense.some else if (offenseCards == defenseCards) None else defense.some
  }

  def attack(pickedCardForAttack: Card): GameState =
    this
      .withOffense(offense => offense.withCards(c => c.removeCard(pickedCardForAttack)))
      .addCardsToTable(Set.empty + pickedCardForAttack)

  def reflect(cardUsedToReflect: Card): GameState =
    this
      .withDefense(defense => defense.withCards(c => c.removeCard(cardUsedToReflect)))
      .addCardsToTable(Set.empty + cardUsedToReflect)
      .swapRoles

  def defend(cardToDefeat: Card, defendingCard: Card): Validated[ErrorMsg, GameState] =
    cardsOnTable
      .find(pair => pair.placed == cardToDefeat)
      .map(pair => cardsOnTable - pair + CardPair(pair.placed, defendingCard.some))
      .fold[Validated[ErrorMsg, GameState]](
        ifEmpty = Invalid(ErrorMsg(s"$cardToDefeat wasn't found on table"))
      )(newCardsOnTable =>
        Valid(withCardsOnTable(_ => newCardsOnTable).withDefense(d => d.withCards(c => c.removeCard(defendingCard))))
      )

  def putExtraCardsOnTable: GameState = getCardsToDefeat match {
    case Some(cards) =>
      val cardsCanBeAdded = defense.cards.value.size - cards.value.size
      if (cardsCanBeAdded > 0) {
        val ranksOnTable = getAllCardsOnTable.map(c => c.rank)
        addCardsToTable(offense.cards.value.filter(card => ranksOnTable.contains(card.rank)).take(cardsCanBeAdded))
      }
      this
    case None => this
  }

  def removeCardsFromTable: GameState =
    this.withCardsOnTable(_ => Set.empty)

  def takeCardsFromTable: GameState =
    this
      .withDefense(d => d.withCards(cards => cards.withCards(c => c ++ getAllCardsOnTable)))
      .withCardsOnTable(_ => Set.empty)
}

case class ErrorMsg(error: String)

object Main {
  def main(args: Array[String]): Unit = {
    implicit val trumpSuite: Suite = Suite.unsafeApply('D')
    val players = parsePlayers("D2 S2 C7 H5 ST | H2 C5 SK S7 SJ")
    println(players.andThen(tpl => play(GameState(tpl._1, tpl._2, Set.empty))).fold(
      e => e.error,
      state => state.determineWinner.fold(ifEmpty = "0")(p => p.id.toString)
    ))
  }

  def play(state: GameState) : Validated[ErrorMsg, GameState] = {
    if (state.noCardsLeftToPlay) return Valid(state)

    state.cardsOnTable.headOption match {
      case Some(firstCardPair) =>
        val rankToCheckForReflect = firstCardPair.placed.rank
        state.tryGetReflectCard(rankToCheckForReflect) match {
          case Some(value) => play(state.reflect(value))
          case None =>
            state.getCardToDefeat match {
              case Some(cardToDefeat) =>
                state.defense.cards.cardToDefend(cardToDefeat) match {
                  case Some(defendingCard) => state.defend(cardToDefeat, defendingCard).andThen(state => play(state))
                  case None => play(state.putExtraCardsOnTable.takeCardsFromTable.swapRoles)
                }
              case None => play(state.removeCardsFromTable.swapRoles)
            }
        }
      case None => state.offense.cards.cardToAttack match {
        case Some(value) => play(state.attack(value))
        case None => Valid(state)
      }
    }
  }

  def parseCard(s: String): Validated[ErrorMsg, Card] = {
    if (s.length != 2) Invalid(ErrorMsg(s"Incorrect number of symbols in $s"))
    else {
      val suitePart = s.charAt(0)
      val rankPart = s.charAt(1)

      Suite(suitePart).andThen(suite => Rank(rankPart).map(rank => Card(suite, rank)))
    }
  }

  // Oh boy, I have to learn Scala. There should be a simpler way to write this.
  def parseCards(s: String, separator: Char = ' '): Validated[ErrorMsg, Cards] = s.trim.split(separator).foldLeft(
    Valid(Cards(SortedSet.empty[Card])): Validated[ErrorMsg, Cards]
  )(
    (cards, s) => parseCard(s).andThen(c => cards.map(cc => cc.withCards(ccc => ccc.union(Set(c)))))
  )

  def parsePlayers(s: String, separatorForPlayers: Char = '|')(implicit trumpSuite: Suite): Validated[ErrorMsg, (Player, Player)] = {
    val playersCards = s.trim.split(separatorForPlayers)
    if (playersCards.length != 2) Invalid(ErrorMsg(s"Couldn't split $s into two pieces"))
    else {
      val firstPlayer = parseCards(playersCards.head).map(cards => Player(1, cards))
      val secondPlayer = parseCards(playersCards.last).map(cards => Player(2, cards))

      firstPlayer.andThen(first => secondPlayer.map(second => (first, second)))
    }
  }
}