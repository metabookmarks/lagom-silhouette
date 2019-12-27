resolvers += Resolver.bintrayIvyRepo("metabookmarks", "sbt-plugin-releases")

//addSbtPlugin("com.typesafe.sbt" % "sbt-twirl" % "1.5.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.3.0")

// The Lagom plugin
addSbtPlugin("com.lightbend.lagom" % "lagom-sbt-plugin" % "1.6.0")

addSbtPlugin("com.lightbend.rp" % "sbt-reactive-app" % "1.7.3")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.12")
addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.6")
addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "1.4.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.6.3")

//addSbtPlugin("io.chrisdavenport" % "sbt-mima-version-check" % "0.1.0")
