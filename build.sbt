organization := "com.katlex"

name := "expenses"

scalaVersion := "2.11.4"

version := "0.1"

seq(coffeeSettings:_*)

seq(sassSettings:_*)

Revolver.settings

(resourceManaged in (Compile, CoffeeKeys.coffee)) <<= (resourceManaged in Compile)(_ / "www" / "js")

(resourceManaged in (Compile, SassKeys.sass)) <<= (resourceManaged in Compile)(_ / "www" / "css")

resourceGenerators in Compile <+= SassKeys.sass in Compile

CoffeeKeys.bare in (Compile, CoffeeKeys.coffee) := true // development purposes only

libraryDependencies ++= {
  val unfilteredVersion = "0.8.2"
  val liftVersion = "2.6-RC1"
  Seq(
    "ch.qos.logback" % "logback-classic"     % "1.0.6",
    "net.liftweb"    %% "lift-util"          % liftVersion,
    "net.databinder" %% "unfiltered-jetty" % unfilteredVersion,
    "net.databinder" %% "unfiltered-filter" % unfilteredVersion,
    "org.json4s" %% "json4s-native" % "3.2.11",
    "com.typesafe.slick" %% "slick" % "2.1.0",
    "com.h2database" % "h2" % "1.4.182" % "runtime",
    "org.mongodb" % "mongo-java-driver" % "2.12.4"
  )
}

