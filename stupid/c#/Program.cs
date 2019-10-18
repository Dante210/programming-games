using System;
using System.Collections.Generic;
using System.Collections.Immutable;
using System.IO;
using System.Linq;
using funkylib;
using Cards = System.Collections.Immutable.ImmutableList<Stupid.Card>;
using CardPairs = System.Collections.Immutable.ImmutableList<Stupid.CardPair>;
using File = funkylib.File;

namespace Stupid {
  enum Result {
    Tie = 0,
    Player1_Won = 1,
    Player2_Won = 2
  }

  public struct GameState {
    public readonly Player attacker, defender;
    public readonly CardPairs onTable;
    public readonly Suit trumpSuit;
    public readonly IComparer<Card> trumpComparer;

    public GameState(Player attacker, Player defender, CardPairs onTable, Suit trumpSuit, IComparer<Card> trumpComparer = null) {
      this.attacker = attacker;
      this.defender = defender;
      this.onTable = onTable;
      this.trumpSuit = trumpSuit;
      this.trumpComparer = trumpComparer ?? Card.comparerForTrumpSuit(trumpSuit);
    }

    public GameState withAttacker(Player attacker) => new GameState(attacker, defender, onTable, trumpSuit, trumpComparer);
    public GameState withDefender(Player defender) => new GameState(attacker, defender, onTable, trumpSuit, trumpComparer);
    public GameState withOnTable(CardPairs onTable) => new GameState(attacker, defender, onTable, trumpSuit, trumpComparer);

    public static Option<GameState> parse(string playersCards, Suit trumpSuit) {
      var parts = playersCards.Split('|');
      var firstPlayerCards = parts.get(0).map(s => s.Trim().Split(' '));
      var secondPlayerCards = parts.get(1).map(s => s.Trim().Split(' '));

      var firstPlayer = firstPlayerCards.flatMap(cards => Player.parse(cards, new PlayerNumber(1)));
      var secondPlayer = secondPlayerCards.flatMap(cards => Player.parse(cards, new PlayerNumber(2)));

      return firstPlayer.zip(secondPlayer, (first, second) => new GameState(first, second, CardPairs.Empty, trumpSuit));
    } 
  }

  public class DataReader {
    public IO<Either<string,ImmutableList<GameState>>> readData(File file) => 
      file.readAllLines().map(exceptional =>
        exceptional.fold(
          exception => exception.ToString(),
          dataLines => dataLines.get(0).fold(
            () => $"Trump card not found",
            trumpStr => Card.parseSuite(trumpStr).fold(
              () => $"Trump card couldn't be parsed!",
              trumpSuite => parseStates(dataLines.Skip(1), trumpSuite)
            )
          )
        )
      );

    Either<string,ImmutableList<GameState>> parseStates(IEnumerable<string> data, Suit trumpSuit) {
      var parsedGameStates = new List<GameState>();
      foreach (var playersCards in data) {
        var gameStateOpt = GameState.parse(playersCards, trumpSuit);
        if (!gameStateOpt.isSome)
          return $"Some rounds couldn't be parsed";
        else
          parsedGameStates.Add(gameStateOpt._unsafe);
      }
      return parsedGameStates.ToImmutableList();
    }
}
  
  public static class Program {
    public static IO<Unit> printLn(string s) => IO.@return(() => Console.WriteLine(s));
    public static IO<Unit> print(string s) => IO.@return(() => Console.Write(s));

    public static void Main(string[] args) => mainIO(args.ToImmutableList()).unsafeRun();

    static IO<Unit> mainIO(ImmutableList<string> args) {
      var file = new File(Path.GetFullPath(Path.Combine(Environment.CurrentDirectory, @"..\..\Data.txt")));
      var dataReader = new DataReader();
      var data = dataReader.readData(file);

      var correctAnswer = 
        "1211112121221102212111211012111211111111211211112112111112111111121211111111202222011111011111112111";

      return printLn(correctAnswer).flatMap(_ =>
        data
        .map(e => e.fold(
          err => err,
          states => resultsToString(states.Select(play).Select(wonTheGame))
        ))
        .flatMap(result =>
          printLn(result).andThen(printLn(result == correctAnswer ? "Correct" :"Incorrect"))
        )
      );
    }

    static string resultsToString(IEnumerable<Result> results) =>
      string.Join("", results.Select(_ => (int)_));

    static GameState play (GameState state) {
      if (
        state.onTable.IsEmpty && (state.attacker.cards.IsEmpty || state.defender.cards.IsEmpty)
      ) return state;

      if (state.onTable.IsEmpty)
        return play(attack(
          state,
          state.attacker.cards
            .OrderBy(card => card, state.trumpComparer)
            .First()
        ));
 
      return reflectCard(state).fold(
        onSome: reflectCard => play(reflectToOpponent(state, reflectCard)),

        onNone: () => cardToDefeatPair(state.onTable, state.trumpComparer).fold(
          () => play(
            pickForAttack(state).fold(
              () => cannotAttack(state),
              attackCard => attack(state, attackCard)
            )
          ),
          toDefeatPair => play(
            pickForDefense(state.defender, toDefeatPair, state.trumpSuit, state.trumpComparer).fold(
              () => cannotDefend(state),
              defenseCard => defended(state, defenseCard, toDefeatPair)
            )
          )
        )
      );
    }

    static Result wonTheGame(GameState state) {
      if (state.defender.cards.IsEmpty && state.attacker.cards.IsEmpty)
        return Result.Tie;
      return determinePlayer(
        state.attacker.cards.IsEmpty 
        ? state.attacker.number 
        : state.defender.number
      );
    }

    static Result determinePlayer(PlayerNumber number) => 
      number.number > 1 ? Result.Player2_Won : Result.Player1_Won;

    static GameState attack(GameState state, Card cardToAttack) {
      var newAttacker = state.attacker.withCards(_ => _.Remove(cardToAttack));
      var newOnTable = state.onTable.Add(new CardPair(cardToAttack, Option.None));
      return state.withAttacker(newAttacker).withOnTable(newOnTable);
    }

    static GameState reflectToOpponent(GameState state, Card reflectCard) {
      var newAttacker = state.defender.withCards(_ => _.Remove(reflectCard));
      var newDefender = new Player(state.attacker.cards, state.attacker.number);
      var newCardsOnTable = state.onTable.Add(new CardPair(reflectCard, Option.None));
      return state.withAttacker(newAttacker).withDefender(newDefender).withOnTable(newCardsOnTable);
    }

    static GameState addExtraCardsToTake(GameState state) =>
      pickForAttack(state).fold(
        () => state,
        attackCard => addExtraCardsToTake(attack(state, attackCard))
      );

    static Cards allCardsOnTable(CardPairs onTable) {
      var allPlacedCards = onTable.map(pair => pair.placed);
      var allCoveredCards = onTable.map(pair => pair.cover).asEnumerable();
      return allPlacedCards.Concat(allCoveredCards).ToImmutableList();
    }

    static GameState takeFromTableToHand (GameState state) {
      var newDefendersCards = 
        state.defender.cards
        .Concat(allCardsOnTable(state.onTable))
        .ToImmutableList();
      var newDefender = state.defender.withCards(newDefendersCards);
      return state.withDefender(newDefender).withOnTable(CardPairs.Empty);
    }

    static GameState cannotDefend (GameState state) => 
      takeFromTableToHand(addExtraCardsToTake(state));

    static GameState defended (GameState state, Card defenseCard, CardPair toDefeat) {
      var newDefender = state.defender.withCards(_ => _.Remove(defenseCard));

      var newOnTable = state.onTable.Replace(
        toDefeat,
        new CardPair(toDefeat.placed, defenseCard.some())
      );

      return state.withDefender(newDefender).withOnTable(newOnTable);
    }

    static GameState cannotAttack(GameState state) =>
      state.withDefender(state.attacker).withAttacker(state.defender).withOnTable(CardPairs.Empty);

    public static Option<Card> reflectCard(GameState state) {
      var reflectCards = 
        state.defender.cards
        .Where(card => state.onTable.TrueForAll(pair => pair.placed.rank == card.rank));

      return 
        state.onTable.Count + 1 > state.attacker.cards.Count 
        ? Option.None 
        : reflectCards.head();
    }

    public static Option<CardPair> cardToDefeatPair(CardPairs onTable, IComparer<Card> trumpComparer) {
      var uncoveredCards = onTable.Where(pair => !pair.cover.isSome);
      return uncoveredCards.OrderBy(pair => pair.placed, trumpComparer).head();
    }

    public static Option<Card> pickForDefense(
      Player defender, CardPair toDefeatPair,
      Suit trumpSuit, IComparer<Card> trumpComparer
    ) {
      var sameSuitePicks =
        defender.cards
        .Where(card => card.suit == toDefeatPair.placed.suit && card.CompareTo(toDefeatPair.placed) > 0)
        .OrderBy(_ => _, trumpComparer);

      var trumpCards =
        defender.cards
        .Where(card => 
          card.suit == trumpSuit && (
            toDefeatPair.placed.suit != trumpSuit 
            || card.CompareTo(toDefeatPair.placed) > 0
          )
        )
        .OrderBy(_ => _, trumpComparer);

      var possiblePicks = sameSuitePicks.Concat(trumpCards);
      return possiblePicks.head();
    }

    public static Option<Card> pickForAttack (GameState state) {
      if (state.defender.cards.IsEmpty) return Option.None;
      var allOnTable = allCardsOnTable(state.onTable);

      var possiblePicks =
        allOnTable
        .flatmap(cardOnTable => state.attacker.cards.Where(attackersCard => cardOnTable.rank == attackersCard.rank))
        .OrderBy(_ => _, state.trumpComparer);

      return possiblePicks.head(); 
    }
  }

  public static class IEnumerableExts {
    public static Option<A> head<A>(this IEnumerable<A> enumerable) {
      foreach (var a in enumerable) return a.some();
      return Option.None;
    }
  }
}
