lazy val commonSettings = Seq(
  organization := "org.jj",
  version := "0.1",
  scalaVersion := "2.12.4"
)


name := "jj"

lazy val core = (project in file("core")).settings(commonSettings)

lazy val root = (project in file(".")).aggregate(core)