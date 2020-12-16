Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / scalaVersion := "2.13.3"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.quantexa"
ThisBuild / organizationName := "quantexa"

ThisBuild / scalafmtOnCompile := true

lazy val root = (project in file("."))
  .settings(
    name := "analyze",
    libraryDependencies ++= Seq(
      "com.madgag.scala-git"   %% "scala-git"               % "4.3",
      "com.sksamuel.elastic4s" %% "elastic4s-client-esjava" % "7.9.2",
      "org.scalatest"          %% "scalatest"               % "3.2.2" % Test,
      "org.typelevel"          %% "cats-core"               % "2.3.0",
      "org.typelevel"          %% "cats-effect"             % "2.3.0",
      "org.typelevel"          %% "cats-kernel"             % "2.3.0",
    ),
  )
