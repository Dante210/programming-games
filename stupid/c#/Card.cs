using System;
using System.Collections.Generic;
using funkylib;

namespace Stupid {
  public enum Suit {
    Hearts,
    Diamonds,
    Clubs,
    Spades
  }

  public enum Rank {
    Two,
    Three,
    Four,
    Five,
    Six,
    Seven,
    Eight,
    Nine,
    Ten,
    Jack,
    Queen,
    King,
    Ace
  }

  public struct CardPair {
    public readonly Card placed;
    public readonly Option<Card> cover;

    public CardPair(Card placed, Option<Card> cover) {
      this.placed = placed;
      this.cover = cover;
    }
  }

  public struct Card : IComparable<Card> {
    public readonly Suit suit;
    public readonly Rank rank;

    public Card(Suit suit, Rank rank) {
      this.suit = suit;
      this.rank = rank;
    }

    public static Option<Card> parse(string s) {
      var parts = s.Trim();
      var suitePart = parts.Substring(0,1);
      var rankPart = parts.Substring(1, parts.Length - 1);
      var suiteOpt = parseSuite(suitePart);
      var rankOpt = parseRank(rankPart);
      return suiteOpt.zip(rankOpt, (suite, rank) => new Card(suite, rank));
    }

    public static Option<Suit> parseSuite(string indicator) {
      switch (indicator) {
        case "C": return Suit.Clubs.some();
        case "S": return Suit.Spades.some();
        case "H": return Suit.Hearts.some();
        case "D": return Suit.Diamonds.some();
        default: return Option.None;
      }
    }

    static Option<Rank> parseRank(string indicator) {
      switch (indicator) {
        case "2": return Rank.Two.some();
        case "3": return Rank.Three.some();
        case "4": return Rank.Four.some();
        case "5": return Rank.Five.some();
        case "6": return Rank.Six.some();
        case "7": return Rank.Seven.some();
        case "8": return Rank.Eight.some();
        case "9": return Rank.Nine.some();
        case "T": return Rank.Ten.some();
        case "J": return Rank.Jack.some();
        case "Q": return Rank.Queen.some();
        case "K": return Rank.King.some();
        case "A": return Rank.Ace.some();
        default: return Option.None;
      }
    }

    sealed class RankSuiteRelationalComparer : Comparer<Card> {
      public override int Compare(Card x, Card y) {
        var rankComparison = x.rank.CompareTo(y.rank);
        if (rankComparison != 0) return rankComparison;
        return x.suit.CompareTo(y.suit);
      }
    }

    sealed class TrumpRankSuiteRelationalComparer : Comparer<Card> {
      readonly Suit trumpSuit;

      public TrumpRankSuiteRelationalComparer(Suit trumpSuit) { this.trumpSuit = trumpSuit; }

      public override int Compare(Card x, Card y) {
        if (x.suit == trumpSuit && y.suit != trumpSuit)
          return 1;
        return 
          x.suit != trumpSuit && y.suit == trumpSuit 
          ? -1 
          : rankSuiteComparer.Compare(x, y);
      }
    }

    public static Comparer<Card> comparerForTrumpSuit(Suit trumpSuit) => new TrumpRankSuiteRelationalComparer(trumpSuit);
    static Comparer<Card> rankSuiteComparer { get; } = new RankSuiteRelationalComparer();

    public int CompareTo(Card other) => rankSuiteComparer.Compare(this, other);
  }
}