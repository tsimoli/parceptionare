logLevel := Level.Warn

resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

// Sbt native packer
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.0.3")