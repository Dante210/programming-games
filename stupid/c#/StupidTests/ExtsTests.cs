using NUnit.Framework;
using System.Collections.Immutable;
using funkylib;

namespace Stupid.Tests {
  [TestFixture()]
  public class ExtsTests {
    [Test()]
    public void getImmutableListTest() {
      Option<int> expected = Option.None;
      var target = ImmutableList.Create(1, 2, 3);
      var actual = target.get(10);
      Assert.True(expected.Equals(actual));
    }

    [Test()]
    public void getArrayTest() {
      Option<int> expected = 1.some();
      var target = new[] {1,2,3};
      var actual = target.get(0);
      Assert.True(expected.Equals(actual));
    }
  }
}