import cats.data.Validated
import cats.data.Validated._
import cats.implicits._

import monocle.Lens
import monocle.macros.GenLens

import scala.collection.SortedSet

abstract class DefinedValue extends Ordered[DefinedValue] {
  def value : Int
  def asString : String

  override def toString: String = asString
  override def hashCode(): Int = value
  override def compare(that: DefinedValue): Int = value - that.value
}

case class Card(suite: Suite, rank: Rank) extends Ordered[Card] {
  override def compare(that: Card): Int = {
    val rank = this.rank.compare(that.rank)
    if (rank == 0) this.suite.compare(that.suite) else rank
  }

  override def toString: String = s"$suite$rank"
}
object Card {
  def parse(s: String): Validated[ErrorMsg, Card] = {
    if (s.length != 2) Invalid(ErrorMsg(s"Incorrect number of symbols in $s"))
    else {
      val suitePart = s.charAt(0)
      val rankPart = s.charAt(1)

      Suite(suitePart).andThen(suite => Rank(rankPart).map(rank => Card(suite, rank)))
    }
  }
}

case class Cards private(value: SortedSet[Card]) {
  def cardToAttack : Option[Card] = this.value.headOption
  def cardToReflect(rankToReflect: Rank): Option[Card] = this.value.find(card => card.rank == rankToReflect)

  def removeCards(cardsToRemove: scala.collection.Set[Card]): Cards = this.copy(value = value.diff(cardsToRemove))
}
object Cards {
  def apply(cards: Set[Card])(implicit trumpSuite: Suite): Cards = {
    implicit val orderByTrumpSuite: Ordering[Card] =
      Ordering
        .by[Card, Boolean](card => card.suite == trumpSuite)
        .orElse(Ordering.by[Card, Rank](c => c.rank))
        .orElse(Ordering.by[Card, Suite](c => c.suite))

    new Cards(SortedSet.empty[Card] ++ cards)
  }

  def parse(s: String, separator: Char = ' ')(implicit trumpSuite: Suite): Validated[ErrorMsg, Cards] =
    s.trim.split(separator)
      .foldLeft(Valid(Set.empty[Card]): Validated[ErrorMsg, Set[Card]]
      )((cards, s) => Card.parse(s).andThen(c => cards.map(cc => cc + c)))
      .map(cards => Cards(cards))
}

case class CardPair(placed: Card, covering: Option[Card]) {
  override def toString: String = s"$placed/${covering.fold(ifEmpty = "-")(c => c.toString)}"
}
case class Player(id: Int, cards: Cards) {
  override def toString: String = s"P$id[$cards]"
}
object Players {
  def parse(s: String, separatorForPlayers: Char = '|')(implicit trumpSuite: Suite)
  : Validated[ErrorMsg, (Player, Player)] = {
    val playersCards = s.trim.split(separatorForPlayers)
    if (playersCards.length != 2) Invalid(ErrorMsg(s"Couldn't split $s into two pieces"))
    else {
      val firstPlayer = Cards.parse(playersCards.head).map(cards => Player(1, cards))
      val secondPlayer = Cards.parse(playersCards.last).map(cards => Player(2, cards))

      firstPlayer.andThen(first => secondPlayer.map(second => (first, second)))
    }
  }
}

case class GameState(offense: Player, defense: Player, cardsOnTable: Set[CardPair])(implicit trumpSuite: Suite) {
  override def toString: String =
  s"""State[
    trump: $trumpSuite
     o: $offense
     d: $defense
     t: $cardsOnTable
  ]"""

  private val withOffense : Lens[GameState, Player] = GenLens[GameState](_.offense)
  private val withDefense : Lens[GameState, Player] = GenLens[GameState](_.defense)

  private val withCards : Lens[Player, Cards] = GenLens[Player](_.cards)
  private val withCardsValue : Lens[Cards, SortedSet[Card]] = GenLens[Cards](_.value)

  private def withCardsOnTable(fn: Set[CardPair] => Set[CardPair]) = this.copy(cardsOnTable = fn(cardsOnTable))

  private def addCardsToTable(cardsToAdd: Iterable[Card]) =
    withCardsOnTable(onTable => onTable ++ cardsToAdd.map(c => CardPair(c, None)))

  private def getCardsToDefeat: Option[Cards] = {
    val cards = Cards(this.cardsOnTable.filter(pair => pair.covering.isEmpty).map(pair => pair.placed))
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
    cardsOnTable.flatMap(pair => pair.covering.map(c => c).toList :+ pair.placed)

  def swapRoles: GameState = withOffense.modify(_ => defense).andThen(withDefense.modify(_ => offense))(this)

  def noCardsLeftToPlay: Boolean = cardsOnTable.isEmpty && (offense.cards.value.isEmpty || defense.cards.value.isEmpty)

  def tryGetReflectCard(rankToCheckForReflect: Rank): Option[Card] = {
    if (canTryToReflect(rankToCheckForReflect)) this.defense.cards.cardToReflect(rankToCheckForReflect)
    else None
  }

  def getCardToDefeat: Option[Card] = this.getCardsToDefeat.map(c => c.value.head)

  def cardToDefend(cardNeededToDefeat: Card): Option[Card] =
    this.defense.cards.value.find(card => {
      if (cardNeededToDefeat.suite == trumpSuite) card.suite == trumpSuite && card > cardNeededToDefeat
      else card.suite == cardNeededToDefeat.suite && card > cardNeededToDefeat || card.suite == trumpSuite
    })

  def determineWinner: Option[Player] = {
    val offenseCards = offense.cards.value.size
    val defenseCards = defense.cards.value.size
    if (offenseCards < defenseCards) offense.some else if (offenseCards == defenseCards) None else defense.some
  }

  def attack(pickedCardForAttack: Card): GameState =
    (withOffense composeLens withCards).modify(c => c.removeCards(Set(pickedCardForAttack)))(this)
    .addCardsToTable(Set.empty + pickedCardForAttack)

  def reflect(cardUsedToReflect: Card): GameState =
    (withDefense composeLens withCards).modify(c => c.removeCards(Set(cardUsedToReflect)))(this)
    .addCardsToTable(Set.empty + cardUsedToReflect)
    .swapRoles

  def defend(cardToDefeat: Card, defendingCard: Card): Validated[ErrorMsg, GameState] =
    cardsOnTable
      .find(pair => pair.placed == cardToDefeat)
      .map(pair => cardsOnTable - pair + CardPair(pair.placed, defendingCard.some))
      .fold[Validated[ErrorMsg, GameState]](
        ifEmpty = Invalid(ErrorMsg(s"$cardToDefeat wasn't found on table"))
      )(newCardsOnTable =>
        Valid(
          (withDefense composeLens withCards).modify(c => c.removeCards(Set(defendingCard)))(this)
          .withCardsOnTable(_ => newCardsOnTable)
        )
      )

  def reinforce(extraCards: SortedSet[Card]): GameState =
    (withOffense composeLens withCards).modify(c => c.removeCards(extraCards))(this)
    .addCardsToTable(extraCards)

  def offenseReinforce: Option[SortedSet[Card]] = {
    val cardsCanBeAdded = defense.cards.value.size - getCardsToDefeat.fold(ifEmpty = 0)(c => c.value.size)
    Option.when(cardsCanBeAdded > 0)(
      offense.cards.value
      .filter(card => getAllCardsOnTable.map(c => c.rank).contains(card.rank)      )
      .take(cardsCanBeAdded)
    ).flatMap(cards => if (cards.isEmpty) None else cards.some)
  }

  def removeCardsFromTable: GameState = this.withCardsOnTable(_ => Set.empty)

  def defensePicksCardsFromTable: GameState =
    (withDefense composeLens withCards composeLens withCardsValue).modify(c => c ++ getAllCardsOnTable)(this)
    .withCardsOnTable(_ => Set.empty)
}

case class ErrorMsg(error: String)
case class GameStateLog(value: String)

object Main {
  def main(args: Array[String]): Unit = {
    val debugLogOn : Boolean = true
    println(runGame("data.txt")(Option.when(debugLogOn)(GameStateLog(""))))
  }

  def runGame(gameResourceFilename: String)(implicit log: Option[GameStateLog]): String =
    parseResource(readResource(gameResourceFilename))
    .map(states => states.map(s => play(s)(log)))
    .fold(
      e => s"!!! ERRORS WHILE PARSING $gameResourceFilename !!! \n$e",
      statesV => statesV.map(v => v.fold(
        e => s"!!! ERRORS WHILE PROCESSING GAME STATE !!! \n$e",
        tpl => {
          val state = tpl._1
          val logOpt = tpl._2
          logOpt.fold(ifEmpty = "")(log => log.value) + state.determineWinner.fold(ifEmpty = "0")(p => p.id.toString)
        }
      )).reduceLeft(_ + _)
    )

  def parseResource(r: Validated[ErrorMsg, (Char, List[String])]): Validated[ErrorMsg, List[GameState]] =
    r.andThen(tpl => {
      val trumpSuitePart = tpl._1
      val playersPart = tpl._2
      Suite(trumpSuitePart).andThen(implicit trumpSuite =>
        playersPart.foldLeft(
          Valid(List.empty[GameState]): Validated[ErrorMsg, List[GameState]]
        )(
          (list, s) => Players.parse(s).andThen(tpl => list.map(state => state :+ GameState(tpl._1, tpl._2, Set.empty)))
        )
      )
    })

  def readResource(filename: String): Validated[ErrorMsg, (Char, List[String])] = {
    val source = scala.io.Source.fromResource(filename)
    val l = source.getLines.toList.map(s => s.trim)
    source.close
    if (l.size < 2) Invalid(ErrorMsg(s"File ($filename) should contain at least 2 lines"))
    else Valid((l.head.head, l.tail))
  }

  def play(state: GameState)(log: Option[GameStateLog])(implicit turn : Int = 1)
  : Validated[ErrorMsg, (GameState, Option[GameStateLog])] = {
    def addToLog(s: String) = log.map(l => GameStateLog(l.value + s"$s: $state\n"))
    if (state.noCardsLeftToPlay) return Valid((state, addToLog("End of the game")))

    state.cardsOnTable.headOption match {
      case Some(firstCardPair) =>
        val rankToCheckForReflect = firstCardPair.placed.rank
        state.tryGetReflectCard(rankToCheckForReflect) match {
          case Some(value) => play(state.reflect(value))(addToLog(s"Reflecting with $value"))
          case None =>
            state.getCardToDefeat match {
              case Some(cardToDefeat) =>
                state.cardToDefend(cardToDefeat) match {
                  case Some(defendingCard) => state
                  .defend(cardToDefeat, defendingCard)
                  .andThen(state => play(state)(addToLog(s"Needed to defeat $cardToDefeat defending with $defendingCard")))
                  case None => state.offenseReinforce match {
                    case Some(value) => play(state.reinforce(value))(
                      addToLog(s"Defense couldn't defend & offense reinforce with $value")
                    )
                    case None => play(state.defensePicksCardsFromTable)(addToLog(s"Couldn't defend against $cardToDefeat"))
                  }
                }
              case None => state.offenseReinforce match {
                case Some(value) => play(state.reinforce(value))(addToLog(s"Offense reinforce with $value"))
                case None => play(state.removeCardsFromTable.swapRoles)(addToLog("Turn ended"))
              }
            }
        }
      case None => state.offense.cards.cardToAttack match {
        case Some(value) => play(state.attack(value))(addToLog(s"Turn $turn \nAttacking with $value"))(turn + 1)
        case None => Valid(state, addToLog("There are no cards to attack"))
      }
    }
  }
}