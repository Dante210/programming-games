using System;
using JetBrains.Annotations;

namespace Stupid {
  public struct Exceptional<A> {
    Exception ex { get; }
    readonly A value;

    public bool success => ex == null;
    public bool exception => ex != null;

    public Exceptional(Exception ex) {
      this.ex = ex;
      value = default(A);
    }

    public Exceptional(A right) {
      value = right;
      ex = null;
    }

    public static implicit operator Exceptional<A>(Exception left) => new Exceptional<A>(left);
    public static implicit operator Exceptional<A>(A right) => new Exceptional<A>(right);

    [PublicAPI]
    public AR fold<AR>(Func<Exception, AR> exception, Func<A, AR> success) => this.exception
      ? exception(ex)
      : success(value);

    [PublicAPI]
    public void voidFold(Action<Exception> exception, Action<A> success) {
      if (this.exception) exception(ex);
      else success(value);
    }
  }
}