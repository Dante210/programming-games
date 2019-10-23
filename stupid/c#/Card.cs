using System;
using System.Collections.Generic;
using pzd.lib.functional;

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
        case "C": return new Option<Suit>(Suit.Clubs);
        case "S": return new Option<Suit>(Suit.Spades);
        case "H": return new Option<Suit>(Suit.Hearts);
        case "D": return new Option<Suit>(Suit.Diamonds);
        default: return Option<Suit>.None;
      }
    }

    static Option<Rank> parseRank(string indicator) {
      switch (indicator) {
        case "2": return new Option<Rank>(Rank.Two);
        case "3": return new Option<Rank>(Rank.Three);
        case "4": return new Option<Rank>(Rank.Four);
        case "5": return new Option<Rank>(Rank.Five);
        case "6": return new Option<Rank>(Rank.Six);
        case "7": return new Option<Rank>(Rank.Seven);
        case "8": return new Option<Rank>(Rank.Eight);
        case "9": return new Option<Rank>(Rank.Nine);
        case "T": return new Option<Rank>(Rank.Ten);
        case "J": return new Option<Rank>(Rank.Jack);
        case "Q": return new Option<Rank>(Rank.Queen);
        case "K": return new Option<Rank>(Rank.King);
        case "A": return new Option<Rank>(Rank.Ace);
        default: return Option<Rank>.None;
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