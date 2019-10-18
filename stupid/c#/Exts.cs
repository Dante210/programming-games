using System.Collections.Immutable;
using funkylib;
using Cards = System.Collections.Immutable.ImmutableList<Stupid.Card>;
using CardPairs = System.Collections.Immutable.ImmutableList<Stupid.CardPair>;


namespace Stupid {
  public static class Exts {
    public static Option<A> get<A>(this A[] arr, int idx) =>
      idx >= 0 && idx < arr.Length ? arr[idx].some() : Option.None;

    public static Option<A> get<A>(this ImmutableList<A> @this, int idx) =>
      idx >= 0 && idx < @this.Count ? @this[idx].some() : Option.None;
  }
}
