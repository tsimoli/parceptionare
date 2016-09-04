name := "parser"

version := "1.0"

scalaVersion := "2.11.7"

val phantomVersion = "1.12.2"


resolvers ++= Seq(
  "Typesafe repository snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
  "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/",
  "Sonatype repo" at "https://oss.sonatype.org/content/groups/scala-tools/",
  "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases",
  "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  "Sonatype staging" at "http://oss.sonatype.org/content/repositories/staging",
  "Java.net Maven2 Repository" at "http://download.java.net/maven/2/",
  "Twitter Repository" at "http://maven.twttr.com",
  Resolver.bintrayRepo("websudos", "oss-releases")
)

libraryDependencies ++= Seq(
  "commons-io" % "commons-io" % "2.4",
  "org.apache.commons" % "commons-lang3" % "3.1",
  "com.google.protobuf" % "protobuf-java" % "2.6.1",
  "com.typesafe.akka" %% "akka-actor" % "2.3.9",
  "io.reactivex" %% "rxscala" % "0.24.1",
  "com.google.api-client" % "google-api-client" % "1.20.0",
  "com.google.apis" % "google-api-services-storage" % "v1-rev35-1.20.0",
  "com.typesafe.akka" % "akka-contrib_2.11" % "2.3.12",
  "com.websudos" %% "phantom-dsl" % phantomVersion,
  "com.websudos" %% "phantom-testkit" % phantomVersion % "test, provided",
  "com.typesafe.slick" %% "slick" % "3.0.2",
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
  "com.github.tototoshi" %% "slick-joda-mapper" % "2.0.0",
  "com.typesafe.play" %% "play-slick" % "1.0.1",
  "com.typesafe.play" %% "play-slick-evolutions" % "1.0.1",
  "net.sandrogrzicic" %% "scalabuff-runtime" % "1.4.0"
)
