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

libraryDependencies ++= Seq(
  "net.databinder" %% "unfiltered-jetty" % "0.8.2",
  "net.databinder" %% "unfiltered-filter" % "0.8.2",
  "com.typesafe.slick" %% "slick" % "2.1.0",
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "com.h2database" % "h2" % "1.4.182" % "runtime",
  "org.mongodb" % "mongo-java-driver" % "2.12.4"
)

