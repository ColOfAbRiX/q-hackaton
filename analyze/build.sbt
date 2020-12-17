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
      "org.eclipse.jgit"        % "org.eclipse.jgit"        % "5.10.0.202012080955-r",
      "org.scalatest"          %% "scalatest"               % "3.2.2" % Test,
      "org.typelevel"          %% "cats-core"               % "2.3.0",
      "org.typelevel"          %% "cats-effect"             % "2.3.0",
      "io.monix"               %% "monix"                   % "3.3.0",
      "org.typelevel"          %% "cats-kernel"             % "2.3.0",
      "com.sksamuel.elastic4s" %% "elastic4s-client-esjava" % "7.9.2",
      "com.sksamuel.elastic4s" %% "elastic4s-json-circe"    % "7.9.2",
      "com.sksamuel.elastic4s" %% "elastic4s-core"          % "7.9.2",
      "io.scalaland"           %% "chimney"                 % "0.6.1",
      "io.circe"               %% "circe-core"              % "0.12.3",
      "io.circe"               %% "circe-generic"           % "0.12.3",
      "io.circe"               %% "circe-parser"            % "0.12.3",
    ),
  )
