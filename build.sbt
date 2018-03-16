name := "core"
organization := "org.jj"

version := "0.1"

scalaVersion := "2.12.4"

mainClass in Compile := Some("org.jj.core.Server")

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
enablePlugins(AshScriptPlugin)

dockerBaseImage := "openjdk:jre-alpine"
