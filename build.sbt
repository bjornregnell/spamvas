lazy val spamvasVersion = "0.1.0"

ThisBuild / organization := "se.bjornregnell"
ThisBuild / version      := spamvasVersion
ThisBuild / scalaVersion := "3.6.4"

fork := true
run / javaOptions += "-Xmx8G"
outputStrategy := Some(StdoutOutput)
run / connectInput := true

lazy val root = (project in file("."))
  .aggregate(common, client, server)
  .settings(
    //update / aggregate := false
  )

lazy val common = (project in file("common"))
  .settings(
    // other settings
  )

lazy val client = (project in file("client"))
  .settings(
    name := "SpamvasClient",
    scalacOptions := Seq("-encoding", "utf8", "-deprecation", "-unchecked", "-Werror", "-feature"),

    assembly / assemblyJarName := s"SpamvasClient-assembly-$spamvasVersion.jar",
    assembly / mainClass := Some("spamvas.ClientMain"),
  )
  .dependsOn(common)

lazy val server = (project in file("server"))
  .settings(
    name := "SpamvasServer",
    scalacOptions := Seq("-encoding", "utf8", "-deprecation", "-unchecked", "-Werror", "-feature"),

    assembly / assemblyJarName := s"SpamvasServer-assembly-$spamvasVersion.jar",
    assembly / mainClass := Some("spamvas.ServerMain"),
  )
  .dependsOn(common)

