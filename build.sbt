import sbt._
import sbt.Keys._
import sbtrelease.ReleasePlugin.autoImport._
import com.typesafe.sbt.pgp.PgpKeys
import sbtcrossproject.CrossProject
import sbtcrossproject.CrossType

val Org = "org.scalafuzz"
val ProjectName = "scalafuzz-scalac"
val PluginProjectName = "scalafuzz-scalac-plugin"
val RuntimeProjectName = "scalafuzz-scalac-runtime"
val LibProjectName = "scalafuzz-lib"
val MockitoVersion = "2.19.0"
val ScalatestVersion = "3.0.5-M1"

val appSettings = Seq(
    organization := Org,
    scalaVersion := "2.12.4",
    crossScalaVersions := Seq("2.10.7", "2.11.12", "2.12.4", "2.13.0-M3"),
    fork in Test := false,
    publishMavenStyle := true,
    publishArtifact in Test := false,
    parallelExecution in Test := false,
    scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8"),
    concurrentRestrictions in Global += Tags.limit(Tags.Test, 1),
    publishTo := {
      if (isSnapshot.value)
        Some("snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")
      else
        Some("releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2")
    },
    pomExtra := {
      <url>https://github.com/greenhat/scalafuzz-scalac-plugin</url>
        <licenses>
          <license>
            <name>Apache 2</name>
            <url>http://www.apache.org/licenses/LICENSE-2.0</url>
            <distribution>repo</distribution>
          </license>
        </licenses>
        <scm>
          <url>git@github.com:greenhat/scalafuzz-scalac-plugin.git</url>
          <connection>scm:git@github.com:greenhat/scalafuzz-scalac-plugin.git</connection>
        </scm>
        <developers>
          <developer>
            <id>greenhat</id>
            <name>Denys Zadorozhnyi</name>
            <url>http://github.com/greenhat</url>
          </developer>
        </developers>
    },
    pomIncludeRepository := {
      _ => false
    }
  ) ++ Seq(
    releaseCrossBuild := true,
    releasePublishArtifactsAction := PgpKeys.publishSigned.value
  )

lazy val root = Project(ProjectName, file("."))
    .settings(name := ProjectName)
    .settings(appSettings: _*)
    .settings(publishArtifact := false)
    .settings(publishLocal := {})
    .aggregate(plugin, runtime.jvm, runtime.js, lib)

lazy val runtime = CrossProject(RuntimeProjectName, file(RuntimeProjectName))(JVMPlatform, JSPlatform)
    .crossType(CrossType.Full)
    .settings(name := RuntimeProjectName)
    .settings(appSettings: _*)
    .jvmSettings(
      libraryDependencies ++= Seq(
      "org.mockito" % "mockito-core" % MockitoVersion % Test,
      "org.scalatest" %% "scalatest" % ScalatestVersion % Test
      )
    )
    .jsSettings(
      libraryDependencies += "org.scalatest" %%% "scalatest" % ScalatestVersion % Test,
      scalaJSStage := FastOptStage,
      inConfig(Test)(jsEnv := RhinoJSEnv().value)
    )

lazy val `scalafuzz-scalac-runtimeJVM` = runtime.jvm
lazy val `scalafuzz-scalac-runtimeJS` = runtime.js

lazy val plugin = Project(PluginProjectName, file(PluginProjectName))
    .dependsOn(`scalafuzz-scalac-runtimeJVM` % Test)
    .settings(name := PluginProjectName)
    .settings(appSettings: _*)
    .settings(libraryDependencies ++= Seq(
    "org.mockito" % "mockito-core" % MockitoVersion % Test,
    "org.scalatest" %% "scalatest" % ScalatestVersion % Test,
    "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided"
  )).settings(libraryDependencies ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, scalaMajor)) if scalaMajor > 10 => Seq(
        "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
        "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0" % Test
      )
      case _ => Seq(
        "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2" % Test
      )
    }
  })

lazy val lib = Project(LibProjectName, file(LibProjectName))
  .dependsOn(`scalafuzz-scalac-runtimeJVM`)
  .settings(name := LibProjectName)
  .settings(appSettings: _*)
  .settings(libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % ScalatestVersion % Test,
    "ch.qos.logback" % "logback-classic" % "1.2.3" % Test,
  )).settings(libraryDependencies ++= {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, scalaMajor)) if scalaMajor > 10 => Seq(
      "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0"
    )
    case _ => Seq(
      "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2"
    )
  }
})
