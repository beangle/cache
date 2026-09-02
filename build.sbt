import org.beangle.parent.Dependencies.*
import org.beangle.parent.Settings.*
import sbt.Keys.*

organization := "org.beangle.cache"
version := "0.1.22-SNAPSHOT"

scmInfo := Some(
  ScmInfo(
    uri("https://github.com/beangle/cache"),
    "scm:git@github.com:beangle/cache.git"
  )
)

developers := List(
  Developer(
    id = "chaostone",
    name = "Tihua Duan",
    email = "duantihua@gmail.com",
    url = uri("http://github.com/duantihua")
  )
)

description := "The Beangle Cache Library"
homepage := Some(uri("https://beangle.github.io/cache/index.html"))
resolvers += Resolver.mavenLocal

val beangle_commons = "org.beangle.commons" % "beangle-commons" % "6.3.2"

lazy val root = (project in file("."))
  .settings(
    name := "beangle-cache",
    common,
    libraryDependencies ++= Seq(beangle_commons, slf4j),
    libraryDependencies ++= Seq(logback_classic % "test", scalatest),
    libraryDependencies ++= Seq(caffeine % "optional", caffeine_jcache % "optional"),
    libraryDependencies ++= Seq(ehcache % "optional"),
    libraryDependencies ++= Seq(jgroups % "optional"),
    libraryDependencies ++= Seq(jedis % "optional")
  )
