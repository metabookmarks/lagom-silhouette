resolvers += Resolver.bintrayIvyRepo("metabookmarks", "sbt-plugin-releases")

addSbtPlugin("com.typesafe.sbt" % "sbt-twirl" % "1.3.15")

// The Lagom plugin
addSbtPlugin("com.lightbend.lagom" % "lagom-sbt-plugin" % "1.5.0-M4")

addSbtPlugin("com.lightbend.rp" % "sbt-reactive-app" % "1.3.1")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.9")
addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.4")
addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "1.3.2")
addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.6.2")
