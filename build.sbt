
name := """parceptionare"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(SbtNativePackager, DockerPlugin)
  .aggregate(parser)
  .dependsOn(parser)


lazy val parser = project

scalaVersion := "2.11.7"

val phantomVersion = "1.12.2"

resolvers ++= Seq(
  "Typesafe repository snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
  "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/",
  "Sonatype repo"                    at "https://oss.sonatype.org/content/groups/scala-tools/",
  "Sonatype releases"                at "https://oss.sonatype.org/content/repositories/releases",
  "Sonatype snapshots"               at "https://oss.sonatype.org/content/repositories/snapshots",
  "Sonatype staging"                 at "http://oss.sonatype.org/content/repositories/staging",
  "Java.net Maven2 Repository"       at "http://download.java.net/maven/2/",
  "Twitter Repository"               at "http://maven.twttr.com",
  "Websudos releases"                at "https://dl.bintray.com/websudos/oss-releases/"
)

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

resolvers += "krasserm at bintray" at "http://dl.bintray.com/krasserm/maven"

libraryDependencies ++= Seq(
  "commons-io" % "commons-io" % "2.4",
  "org.apache.commons" % "commons-compress" % "1.10",
  "org.apache.commons" % "commons-lang3" % "3.1",
  "com.typesafe.akka" %% "akka-actor" % "2.3.9",
  "io.reactivex" %% "rxscala" % "0.24.1",
  "com.google.api-client" % "google-api-client" % "1.20.0",
  "com.google.apis" % "google-api-services-storage" % "v1-rev35-1.20.0",
  "com.typesafe.play" %% "play-slick" % "1.1.1",
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
  "com.github.tototoshi" %% "slick-joda-mapper" % "2.0.0",
  "com.github.tminglei" %% "slick-pg" % "0.10.0",
  "com.github.tminglei" %% "slick-pg_play-json" % "0.10.0", // play json to postgres json
  "com.google.protobuf" % "protobuf-java" % "2.6.1",
  "net.sandrogrzicic" %% "scalabuff-runtime" % "1.4.0",
  "com.spotify" % "async-google-pubsub-client" % "1.17",
  "com.typesafe.play" %% "play-ws" % "2.5.5",
  "com.typesafe.play" %% "play-json" % "2.5.5",
  "com.typesafe" % "config" % "1.3.0",
  "net.codingwell" %% "scala-guice" % "4.1.0"
)

// Docker settings
maintainer in Docker := "tsimoli"

version in Docker := "0.0.1"

packageName in Docker := "parceptionare"

    