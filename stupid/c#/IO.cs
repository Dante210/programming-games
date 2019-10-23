using System;
using System.Collections.Generic;
using System.Collections.Immutable;
using System.IO;
using pzd.lib.functional;

namespace Stupid {
  public static class IO {
    public static readonly IO<Unit> empty = @return(() => { });

    public static IO<A> @return<A>(Func<A> fn) => new IO<A>(fn);
    public static IO<Unit> @return(Action action) => new IO<Unit>(() => {
      action();
      return new Unit();
    });
  }

  // Allows encapsulating side effects and compose them.
  public struct IO<A> {
    readonly Func<A> fn;

    public IO(Func<A> fn) => this.fn = fn;

    public IO<B> map<B>(Func<A, B> mapper) {
      var fn = this.fn;
      return new IO<B>(() => mapper(fn()));
    }

    public IO<B> andThen<B>(IO<B> io2) {
      var fn = this.fn;
      return new IO<B>(() => {
        fn();
        return io2.unsafeRun();
      });
    }

    public IO<B> flatMap<B>(Func<A, IO<B>> mapper) {
      var fn = this.fn;
      return new IO<B>(() => mapper(fn()).unsafeRun());
    }

    // Runs the encapsulated side effects.
    public A unsafeRun() => fn();
  }
  public static class IOExts {
    public static R @using<TDisp, R>(TDisp disposable, Func<TDisp, R> f) where TDisp : IDisposable {
      using (disposable) return f(disposable);
    }

    public static IO<Exceptional<R>> readerWrapper<R>(File file, Func<StreamReader, R> func) {
      return file.streamReader.map(exceptional =>
        exceptional.fold<Exceptional<R>>(
          exception => exception,
          streamReader => @using(streamReader, func)
        )
      );
    }

    public static IO<Exceptional<ImmutableList<string>>> readAllLines(this File @this) {
      return readerWrapper(@this, reader => {
        string line;
        var temp = new List<string>();
        while ((line = reader.ReadLine()) != null) {
          temp.Add(line);
        }
        return temp.ToImmutableList();
      });
    }
  }
}