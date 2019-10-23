using System.Collections.Immutable;
using pzd.lib.functional;
using Cards = System.Collections.Immutable.ImmutableList<Stupid.Card>;
using CardPairs = System.Collections.Immutable.ImmutableList<Stupid.CardPair>;


namespace Stupid {
  public static class Exts {
    public static Option<A> get<A>(this A[] arr, int idx) =>
      idx >= 0 && idx < arr.Length ? new Option<A>(arr[idx]) : Option<A>.None;

    public static Option<A> get<A>(this ImmutableList<A> @this, int idx) =>
      idx >= 0 && idx < @this.Count ? new Option<A>(@this[idx]) : Option<A>.None;
  }
}
