using System;
using System.Collections.Generic;
using System.Collections.Immutable;
using pzd.lib.functional;

namespace Stupid {
  public struct Player {
    public readonly PlayerNumber number;
    public readonly ImmutableList<Card> cards;

    public Player(ImmutableList<Card> cards, PlayerNumber number) {
      this.cards = cards;
      this.number = number;
    }

    public Player withCards(Func<ImmutableList<Card>, ImmutableList<Card>> f) => new Player(f(cards), number);
    public Player withCards (ImmutableList<Card> cards) => new Player(cards, number);

    public static Option<Player> parse(IEnumerable<string> playerCards, PlayerNumber number) {
      var parsedCards = new List<Card>();

      foreach (var card in playerCards) {
        var parsedCardOpt = Card.parse(card);
        if (!parsedCardOpt.isSome) return Option<Player>.None;
        parsedCards.Add(parsedCardOpt.__unsafeGet);
      }
      return new Option<Player>(new Player(parsedCards.ToImmutableList(), number));
    }
  }

  public struct PlayerNumber {
    public readonly int number;
    public PlayerNumber(int number) { this.number = number; }
  }
}