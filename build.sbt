import com.typesafe.sbt.packager.docker._

lazy val scala213 = "2.13.3"

lazy val supportedScalaVersions = Seq(
  crossScalaVersions := List(scala213)
)

val circeVersion = "0.13.0"

inThisBuild(
  List(
    resolvers ++= Seq("Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
                      Resolver.bintrayRepo("metabookmarks", "releases")
      ),
    scalaVersion := scala213,
    organization := "io.metabookmarks.lagom",
    bintrayOrganization := Some("metabookmarks"),
    organizationName := "MetaBookMarks",
    startYear := Some(2019),
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
        Developer(
          "cheleb",
          "Olivier NOUGUIER",
          "olivier.nouguier@gmail.com",
          url("https://github.com/OlivierNouguier")
        )
      ),
    scalafmtOnCompile := true,
    javacOptions ++= Seq("-source", "11", "-target", "11"),
    scalacOptions ++= Seq(
        //      "-Xplugin-require:macroparadise",
        "-unchecked",
        "-deprecation",
        "-language:_",
        "-target:jvm-11",
        "-encoding",
        "UTF-8"
      ) // ++ crossFlags(scalaVersion.value),
    //libraryDependencies// ++= crossPlugins(scalaVersion.value)
  )
)

lazy val commonSettings =
  Seq(
    scalacOptions ++= crossFlags(scalaVersion.value),
    libraryDependencies ++= crossPlugins(scalaVersion.value),
    bintrayRepository := "releases"
  )

def crossFlags(scalaVersion: String) =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, 13)) =>
      Seq("-Ymacro-annotations")
    case Some((2, _)) =>
      Seq("-Ypartial-unification", "-Ywarn-unused-import")
    case _ => Seq.empty
  }

def crossPlugins(scalaVersion: String) =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, 13)) =>
      Nil
    case Some((2, _)) =>
      Seq(compilerPlugin(("org.scalamacros" % "paradise" % "2.1.1").cross(CrossVersion.full)))
    //  compilerPlugin(("org.typelevel" % "kind-projector" % "0.10.1").cross(CrossVersion.binary)))
    case _ => Seq.empty
  }

//lagomCassandraEnabled in ThisBuild := false
//lagomUnmanagedServices in ThisBuild := Map("cas_native" -> "http://localhost:9042")

val monocleVersion = "2.0.0"

val monocle = Seq(
  "com.github.julien-truffaut" %% "monocle-core" % monocleVersion,
  "com.github.julien-truffaut" %% "monocle-macro" % monocleVersion,
  "com.github.julien-truffaut" %% "monocle-law" % monocleVersion % "test"
)

val playJsonDerivedCodecs = "org.julienrf" %% "play-json-derived-codecs" % "7.0.0"
val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.7" % "provided"
val scalaTest = "org.scalatest" %% "scalatest" % "3.2.3" % Test
val cats = Seq("org.typelevel" %% "cats-core" % "2.2.0")
val lagomMacro = "io.metabookmarks" %% "lagom-scalameta" % "0.1.4"
val chimney = "io.scalaland" %% "chimney" % "0.6.0"

val playCirce = Seq("com.dripower" %% "play-circe" % "2812.0",
                    "io.circe" %% "circe-parser" % circeVersion,
                    "io.circe" %% "circe-generic" % circeVersion
)

lazy val `lagom-silhouette` = (project in file("."))
  .settings(publish := {})
  .aggregate(security,
             `session-api`,
             `session-impl`,
             `user-api`,
             `user-impl`,
             `lagom-silhouette-web`,
             `lagom-silhouette-web-ui`
  )

lazy val security = (project in file("security"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
        lagomScaladslApi,
        lagomScaladslServer % Optional,
        scalaTest
      )
  )
  .settings(
    supportedScalaVersions
  )

lazy val `session-api` = (project in file("session-api"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
        lagomScaladslApi,
        playJsonDerivedCodecs,
        lagomMacro
      )
  )
  .settings(
    supportedScalaVersions
  )
  .dependsOn(security)

lazy val `session-impl` = (project in file("session-impl"))
  .enablePlugins(LagomScala)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
        lagomScaladslPersistenceCassandra,
        lagomScaladslKafkaBroker,
        lagomScaladslTestKit,
        macwire,
        chimney,
        scalaTest
      )
  )
  .settings(
    supportedScalaVersions
  )
  .settings(lagomForkedTestSettings: _*)
  .dependsOn(`session-api`)

lazy val `user-api` = (project in file("user-api"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
        lagomScaladslApi,
        playJsonDerivedCodecs,
        lagomMacro
      )
  )
  .settings(
    supportedScalaVersions
  )
  .dependsOn(security)

lazy val `user-impl` = (project in file("user-impl"))
  .enablePlugins(LagomScala)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
        //    "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % "0.17.0+20180718-1128",
        lagomScaladslPersistenceCassandra,
        lagomScaladslKafkaBroker,
        lagomScaladslTestKit,
        macwire,
        scalaTest
      )
  )
  .settings(
    supportedScalaVersions
  )
  .settings(lagomForkedTestSettings: _*)
  .dependsOn(`user-api`)

val silhouetteVersion = "7.0.0"

lazy val `lagom-silhouette-web` = (project in file("lagom-silhouette/web"))
  .enablePlugins(play.sbt.routes.RoutesCompiler, SbtTwirl, WebScalaJSBundlerPlugin)
  .dependsOn(security, `session-api`, `user-api`, `lagom-silhouette-web-shared-js`)
  .settings(
  scalaJSProjects := Seq(`lagom-silhouette-web-ui`),
    pipelineStages in Assets := Seq(scalaJSPipeline),
    // triggers scalaJSPipeline when using compile or continuous compilation
    compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value
  )
  .settings(commonSettings)
  .settings(
    resolvers += "Atlasian" at "https://maven.atlassian.com/content/repositories/atlassian-public",
    libraryDependencies ++= cats ++ playCirce ++ Seq(
        lagomScaladslServer,
        macwire,
        chimney,
        scalaTest,
        "com.mohiva" %% "play-silhouette" % silhouetteVersion,
        "com.mohiva" %% "play-silhouette-password-bcrypt" % silhouetteVersion,
        "com.mohiva" %% "play-silhouette-persistence" % silhouetteVersion,
        "com.mohiva" %% "play-silhouette-crypto-jca" % silhouetteVersion,
        jdbc,
        ehcache,
        openId,
        "net.codingwell" %% "scala-guice" % "4.2.11",
        "com.typesafe.play" %% "play-mailer" % "8.0.1",
        "org.webjars" %% "webjars-play" % "2.8.0-1",
//      "com.typesafe.play" %% "play-slick" % "4.0.0",
        "com.adrianhurt" %% "play-bootstrap" % "1.5.1-P27-B4",
        "com.iheart" %% "ficus" % "1.5.0",
        "org.webjars" % "bootstrap" % "4.4.1-1",
        "org.ocpsoft.prettytime" % "prettytime" % "4.0.6.Final",
        "org.webjars" % "foundation" % "6.4.3-1",
        "org.webjars" % "foundation-icon-fonts" % "d596a3cfb3"
      ),
    //   EclipseKeys.preTasks := Seq(compile in Compile),
    TwirlKeys.templateImports ++= Seq("controllers._",
                                      "play.api.data._",
                                      "play.api.i18n._",
                                      "play.api.mvc._",
                                      "views.html._"
      ),
    sources in (Compile, play.sbt.routes.RoutesKeys.routes) ++= ((unmanagedResourceDirectories in Compile).value * "silhouette.routes").get
  )
  .settings(
    supportedScalaVersions
  )
  .enablePlugins(SbtWeb)

import sbtrelease.ReleaseStateTransformations._
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  releaseStepCommandAndRemaining("+ test"),
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("+ publish"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)

def nexusNpmSettings =
  sys.env
    .get("NEXUS")
    .map(url =>
      npmExtraArgs ++= Seq(
          s"--registry=$url/repository/npm-public/"
        )
    )
    .toSeq

val slinkyVersion = "0.6.6"

lazy val `lagom-silhouette-web-ui` = (project in file("lagom-silhouette/web-ui"))
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .settings(crossScalaVersions := Seq(scala213))
  .settings(nexusNpmSettings)
  .settings(commonSettings)
  .settings(
    scalaJSUseMainModuleInitializer := true,
     bintrayRepository := "releases",
    libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-dom" % "1.1.0",
        "me.shadaj" %%% "slinky-core" % slinkyVersion, // core React functionality, no React DOM
        "me.shadaj" %%% "slinky-web" % slinkyVersion, // React DOM, HTML and SVG tags
//      "me.shadaj" %%% "slinky-hot" % slinkyVersion // Hot loading, requires react-proxy package
        //"me.shadaj" %%% "slinky-scalajsreact-interop" % "0.6.4" // Interop with japgolly/scalajs-react,
        "io.metabookmarks" %%% "slinky-material-ui" % "0.0.7",
        "io.circe" %%% "circe-parser" % circeVersion,
        "io.circe" %%% "circe-generic" % circeVersion,
        "com.softwaremill.sttp.client" %%% "core" % "2.2.9"
      ),
    libraryDependencies += "com.lihaoyi" %%% "utest" % "0.7.5" % "test",
    testFrameworks += new TestFramework("utest.runner.Framework")
  )
  .settings(commonSettings)
  .dependsOn(`lagom-silhouette-web-shared-js`)
  .aggregate(`lagom-silhouette-web-shared-js`)

lazy val `lagom-silhouette-web-shared` = (crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("lagom-silhouette/web-shared")))
  .jsSettings(name := "lagom-silhouette-web-shared-js")
  .jvmSettings(name := "lagom-silhouette-web-shared-jvm")
  .settings(
     bintrayRepository := "releases",
    libraryDependencies ++= Seq(
        "io.circe" %%% "circe-core",
        "io.circe" %%% "circe-generic",
        "io.circe" %%% "circe-parser"
      ).map(_ % circeVersion),
    libraryDependencies += "com.lihaoyi" %%% "utest" % "0.7.5" % "test",
    testFrameworks += new TestFramework("utest.runner.Framework")
  )

lazy val `lagom-silhouette-web-shared-jvm` = `lagom-silhouette-web-shared`.jvm
lazy val `lagom-silhouette-web-shared-js` = `lagom-silhouette-web-shared`.js
