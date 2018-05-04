name := "jj-core"


mainClass in Compile := Some("org.jj.core.MainServer")

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
enablePlugins(AshScriptPlugin)

dockerBaseImage := "openjdk:jre-alpine"
