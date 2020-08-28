resolvers += Resolver.bintrayIvyRepo("metabookmarks", "sbt-plugin-releases")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.2")

addSbtPlugin("com.lightbend.lagom" % "lagom-sbt-plugin" % "1.6.4")

addSbtPlugin("com.lightbend.rp" % "sbt-reactive-app" % "1.7.3")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.13")
addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.6")
addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "1.4.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.6.3")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.1.1")
addSbtPlugin("org.scala-js" % "sbt-jsdependencies" % "1.0.2")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.0.0")
addSbtPlugin("ch.epfl.scala" % "sbt-web-scalajs-bundler" % "0.18.0")

//addSbtPlugin("io.chrisdavenport" % "sbt-mima-version-check" % "0.1.0")
