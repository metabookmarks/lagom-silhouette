resolvers += Resolver.bintrayIvyRepo("metabookmarks", "sbt-plugin-releases")

addSbtPlugin("com.typesafe.sbt" % "sbt-twirl" % "1.3.15")

// The Lagom plugin
addSbtPlugin("com.lightbend.lagom" % "lagom-sbt-plugin" % "1.4.6")
addSbtPlugin("io.metabookmarks" % "sbt-plantuml-plugin" % "0.0.26")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.22")
//addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.0.0-M3")
//addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "0.3.1")
addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % "1.0.8")

//addSbtPlugin("com.lightbend.conductr" % "sbt-conductr" % "2.7.2")
addSbtPlugin("com.lightbend.rp" % "sbt-reactive-app" % "1.1.0")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.8")
addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.4")
addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "1.3.2")
addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.6.2")
