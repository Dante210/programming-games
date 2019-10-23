using System;
using System.IO;

namespace Stupid {
  public class File {
    public readonly string path;
    public File(string path) { this.path = path; }

    public IO<Exceptional<StreamReader>> streamReader {
      get {
        try {
          return IO.@return(() => new Exceptional<StreamReader>(System.IO.File.OpenText(path)));
        }
        catch (Exception ex) {
          return IO.@return(() => new Exceptional<StreamReader>(ex));
        }
      }
    }

    public IO<Exceptional<StreamWriter>> streamWriter {
      get {
        try {
          return IO.@return(() => new Exceptional<StreamWriter>(System.IO.File.CreateText(path)));
        }
        catch (Exception ex) {
          return IO.@return(() => new Exceptional<StreamWriter>(ex));
        }
      }
    }
  }
}