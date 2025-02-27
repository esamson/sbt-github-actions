/*
 * Copyright 2020-2021 Daniel Spiewak
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

name := "sbt-github-actions"

lazy val scala212 = "2.12.20"
ThisBuild / organization := "com.github.sbt"
ThisBuild / crossScalaVersions := Seq(scala212)
ThisBuild / scalaVersion := scala212

ThisBuild / githubWorkflowOSes := Seq("ubuntu-latest", "macos-latest", "windows-latest")
ThisBuild / githubWorkflowBuild := Seq(WorkflowStep.Sbt(List("test", "scripted")))
ThisBuild / githubWorkflowJavaVersions ++= Seq(
  JavaSpec.graalvm(Graalvm.Distribution("graalvm"), "17"),
  JavaSpec.corretto("17")
)

ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowPublishTargetBranches :=
  Seq(
    RefPredicate.StartsWith(Ref.Tag("v")),
    RefPredicate.Equals(Ref.Branch("main"))
  )
ThisBuild / githubWorkflowPublish := Seq(
  WorkflowStep.Sbt(
    commands = List("ci-release"),
    name = Some("Publish project"),
    env = Map(
      "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}",
      "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}",
      "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
      "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
    )
  )
)

// So that publishLocal doesn't continuously create new versions
def versionFmt(out: sbtdynver.GitDescribeOutput): String = {
  val snapshotSuffix = if
    (out.isSnapshot()) "-SNAPSHOT"
  else ""
    out.ref.dropPrefix + snapshotSuffix
}

def fallbackVersion(d: java.util.Date): String = s"HEAD-${sbtdynver.DynVer timestamp d}"

ThisBuild / version := dynverGitDescribeOutput.value.mkVersion(versionFmt, fallbackVersion(dynverCurrentDate.value))
ThisBuild / dynver := {
  val d = new java.util.Date
  sbtdynver.DynVer.getGitDescribeOutput(d).mkVersion(versionFmt, fallbackVersion(d))
}

sbtPlugin := true
pluginCrossBuild / sbtVersion := "1.5.5"

publishMavenStyle := true

scalacOptions +=
  "-Xlint:_,-missing-interpolator"

libraryDependencies += "org.specs2" %% "specs2-core" % "4.20.8" % Test

enablePlugins(SbtPlugin)

scriptedLaunchOpts ++= Seq("-Dplugin.version=" + version.value)
scriptedBufferLog := true
// This sbt version is necessary for CI to work on windows with
// scripted tests, see https://github.com/sbt/sbt/pull/7087
scriptedSbt := "1.10.2"

ThisBuild / homepage := Some(url("https://github.com/sbt/sbt-github-actions"))
ThisBuild / startYear := Some(2020)
ThisBuild / dynverSonatypeSnapshots := true
ThisBuild / developers := List(
  Developer(
    id = "armanbilge",
    name = "Arman Bilge",
    email = "@armanbilge",
    url = url("https://github.com/armanbilge")
  ),
  Developer(
    id = "djspiewak",
    name = "Daniel Spiewak",
    email = "@djspiewak",
    url = url("https://github.com/djspiewak")
  ),
  Developer(
    id = "eed3si9n",
    name = "Eugene Yokota",
    email = "@eed3si9n",
    url = url("https://github.com/eed3si9n")
  ),
  Developer(
    id = "mdedetrich",
    name = "Matthew de Detrich",
    email = "mdedetrich@gmail.com",
    url = url("https://github.com/mdedetrich")
  )
)
ThisBuild / description := "An sbt plugin which makes it easier to build with GitHub Actions"
ThisBuild / licenses := List("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild / pomIncludeRepository := { _ =>
  false
}
ThisBuild / publishTo := sonatypePublishToBundle.value
ThisBuild / publishMavenStyle := true
Global / excludeLintKeys ++= Set(pomIncludeRepository, publishMavenStyle)
