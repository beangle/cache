import Dependencies._
import BuildSettings._
import sbt.url

ThisBuild / organization := "org.beangle.cache"
ThisBuild / version := "0.0.23"

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/beangle/cache"),
    "scm:git@github.com:beangle/cache.git"
  )
)

ThisBuild / developers := List(
  Developer(
    id    = "chaostone",
    name  = "Tihua Duan",
    email = "duantihua@gmail.com",
    url   = url("http://github.com/duantihua")
  )
)

ThisBuild / description := "The Beangle Data Library"
ThisBuild / homepage := Some(url("https://beangle.github.io/cache/index.html"))
ThisBuild / resolvers += Resolver.mavenLocal

lazy val root = (project in file("."))
  .settings()
  .aggregate(api,caffeine,ehcache,jgroups,redis)

lazy val api = (project in file("api"))
  .settings(
    name := "beangle-cache-api",
    commonSettings,
    libraryDependencies ++= commonDeps
  )

lazy val caffeine = (project in file("caffeine"))
  .settings(
    name := "beangle-cache-caffeine",
    commonSettings,
    libraryDependencies ++= (commonDeps ++ Seq(Dependencies.caffeine))
  ).dependsOn(api)

lazy val ehcache = (project in file("ehcache"))
  .settings(
    name := "beangle-cache-ehcache",
    commonSettings,
    libraryDependencies ++= (commonDeps ++Seq(Dependencies.ehcache))
  ).dependsOn(api)

lazy val jgroups = (project in file("jgroups"))
  .settings(
    name := "beangle-cache-jgroups",
    commonSettings,
    libraryDependencies ++= (commonDeps ++ Seq(Dependencies.jgroups)),
  ).dependsOn(api)

lazy val redis = (project in file("redis"))
  .settings(
    name := "beangle-cache-redis",
    commonSettings,
    libraryDependencies ++= (commonDeps ++ Seq(Dependencies.jedis)),
  ).dependsOn(api)

publish / skip := true
