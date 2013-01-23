import sbt._
import sbt.Keys._

object ReactiveMongoDemoBuild extends Build {

  lazy val reactivemongodemo = Project(
    id = "reactivemongo-demo",
    base = file("."),
    settings = Project.defaultSettings ++ Seq(
      name := "reactivemongo-demo",
      organization := "tv.yobriefcasts",
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.10.0",
      resolvers ++= Seq(
        "Typesafe repository snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
        "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/"
      ),
      libraryDependencies ++= Seq(
        "org.reactivemongo" %% "reactivemongo" % "0.8"
      )
    )
  )
}
