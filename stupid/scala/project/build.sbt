name := "project"

version := "0.1"

scalaVersion := "2.13.1"

//scalacOptions += "-Ypartial-unification"

libraryDependencies += "org.typelevel" %% "cats-core" % "2.0.0"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % "test"

val monocleVersion = "2.0.0" // depends on cats 2.x

libraryDependencies ++= Seq(
  "com.github.julien-truffaut" %%  "monocle-core"  % monocleVersion,
  "com.github.julien-truffaut" %%  "monocle-macro" % monocleVersion,
  "com.github.julien-truffaut" %%  "monocle-law"   % monocleVersion % "test"
)
