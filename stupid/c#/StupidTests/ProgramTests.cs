﻿using NUnit.Framework;
using System.Collections.Immutable;
using funkylib;

namespace Stupid.Tests {
  [TestFixture()]
  public class ProgramTests {
    [Test()]
    public void pickForDefenseTest() {
      var defenderCards = ImmutableList.Create<Card>()
        .Add(new Card(Suit.Clubs, Rank.Ace))
        .Add(new Card(Suit.Hearts, Rank.Four));
      var defender = new Player(defenderCards, new PlayerNumber(1));
      var toDefeatPair = new CardPair(new Card(Suit.Spades, Rank.King), Option.None);
      var expected = new Card(Suit.Hearts, Rank.Four).some();
      var actual = Program.pickForDefense(defender, toDefeatPair, Suit.Hearts, Card.comparerForTrumpSuit(Suit.Hearts));
      Assert.AreEqual(expected, actual);
    }

    [Test()]
    public void pickForAttackTest() {
      const Suit trumpSuite = Suit.Hearts;

      var defenderCards = ImmutableList.Create<Card>()
        .Add(new Card(Suit.Clubs, Rank.Ace))
        .Add(new Card(Suit.Hearts, Rank.Four));
      var defender = new Player(defenderCards, new PlayerNumber(1));
      var onTable = ImmutableList.Create<CardPair>()
        .Add(new CardPair(new Card(Suit.Diamonds, Rank.Five), new Card(Suit.Diamonds, Rank.Jack).some()))
        .Add(new CardPair(new Card(Suit.Hearts, Rank.Eight), new Card(Suit.Spades, Rank.Six).some()));
      var attackerCards = ImmutableList.Create<Card>()
        .Add(new Card(Suit.Hearts, Rank.Five));
      var attacker = new Player(attackerCards, new PlayerNumber(2));
      var expected = new Card(Suit.Hearts, Rank.Five).some();

      var gameState = new GameState(attacker, defender, onTable, trumpSuite);
      var actual = Program.pickForAttack(gameState);
      Assert.AreEqual(expected, actual);
    }

    [Test()]
    public void cardToDefeatPairTest() {
      var onTable = ImmutableList.Create<CardPair>()
        .Add(new CardPair(new Card(Suit.Diamonds, Rank.Five), new Card(Suit.Diamonds, Rank.Jack).some()))
        .Add(new CardPair(new Card(Suit.Hearts, Rank.Eight), Option.None));
      var expected = new CardPair(new Card(Suit.Hearts, Rank.Eight), Option.None).some();
      var actual = Program.cardToDefeatPair(onTable, Card.comparerForTrumpSuit(Suit.Hearts));
      Assert.AreEqual(expected, actual);
    }

    [Test()]
    public void reflectCardTest() {
      const Suit trumpSuite = Suit.Hearts;
      var attackerCards = ImmutableList.Create<Card>()
        .Add(new Card(Suit.Hearts, Rank.Five));
      var attacker = new Player(attackerCards, new PlayerNumber(2));

      var onTable = ImmutableList.Create(new CardPair(new Card(Suit.Clubs, Rank.Ace), Option.None));

      var defenderCards = ImmutableList.Create<Card>()
        .Add(new Card(Suit.Spades, Rank.Ace))
        .Add(new Card(Suit.Hearts, Rank.Four));

      var defender = new Player(defenderCards, new PlayerNumber(1));
      var expected = Option.None;
      var state = new GameState(attacker, defender, onTable, trumpSuite, Card.comparerForTrumpSuit(trumpSuite));
      var actual = Program.reflectCard(state);
      Assert.AreEqual(expected, actual);
    }
  }
}