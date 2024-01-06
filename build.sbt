import org.beangle.parent.Dependencies._
import org.beangle.parent.Settings._

ThisBuild / organization := "org.beangle.cache"
ThisBuild / version := "0.1.7-SNAPSHOT"

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

val beangle_commons_core = "org.beangle.commons" %% "beangle-commons-core" % "5.6.8"

val commonDeps = Seq(beangle_commons_core,  logback_classic, logback_core, scalatest)

lazy val root = (project in file("."))
  .settings()
  .aggregate(api,p_caffeine,p_ehcache,p_jgroups,p_redis)

lazy val api = (project in file("api"))
  .settings(
    name := "beangle-cache-api",
    common,
    libraryDependencies ++= commonDeps
  )

lazy val p_caffeine = (project in file("caffeine"))
  .settings(
    name := "beangle-cache-caffeine",
    common,
    libraryDependencies ++= (commonDeps ++ Seq(caffeine))
  ).dependsOn(api)

lazy val p_ehcache = (project in file("ehcache"))
  .settings(
    name := "beangle-cache-ehcache",
    common,
    libraryDependencies ++= (commonDeps ++Seq(ehcache))
  ).dependsOn(api)

lazy val p_jgroups = (project in file("jgroups"))
  .settings(
    name := "beangle-cache-jgroups",
    common,
    libraryDependencies ++= (commonDeps ++ Seq(jgroups)),
  ).dependsOn(api)

lazy val p_redis = (project in file("redis"))
  .settings(
    name := "beangle-cache-redis",
    common,
    libraryDependencies ++= (commonDeps ++ Seq(jedis)),
  ).dependsOn(api)

publish / skip := true
