import org.beangle.parent.Dependencies.*
import org.beangle.parent.Settings.*
import sbt.Keys.*

ThisBuild / organization := "org.beangle.cache"
ThisBuild / version := "0.1.9-SNAPSHOT"

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/beangle/cache"),
    "scm:git@github.com:beangle/cache.git"
  )
)

ThisBuild / developers := List(
  Developer(
    id = "chaostone",
    name = "Tihua Duan",
    email = "duantihua@gmail.com",
    url = url("http://github.com/duantihua")
  )
)

ThisBuild / description := "The Beangle Cache Library"
ThisBuild / homepage := Some(url("https://beangle.github.io/cache/index.html"))
ThisBuild / resolvers += Resolver.mavenLocal

val beangle_commons = "org.beangle.commons" % "beangle-commons" % "5.6.15"

lazy val root = (project in file("."))
  .settings(
    name := "beangle-cache",
    common,
    libraryDependencies ++= Seq(beangle_commons),
    libraryDependencies ++= Seq(logback_classic % "test", scalatest),
    libraryDependencies ++= Seq(caffeine % "optional"),
    libraryDependencies ++= Seq(ehcache % "optional"),
    libraryDependencies ++= Seq(jgroups % "optional"),
    libraryDependencies ++= Seq(jedis % "optional")
  )

