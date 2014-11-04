organization := "com.katlex"

name := "expenses"

scalaVersion := "2.11.4"

version := "0.1"

seq(coffeeSettings:_*)

seq(sassSettings:_*)

(resourceManaged in (Compile, CoffeeKeys.coffee)) <<= (resourceManaged in Compile)(_ / "www" / "js")

(resourceManaged in (Compile, SassKeys.sass)) <<= (resourceManaged in Compile)(_ / "www" / "css")

resourceGenerators in Compile <+= SassKeys.sass in Compile

libraryDependencies ++= Seq(
   "net.databinder" %% "unfiltered-jetty" % "0.8.2",
   "net.databinder" %% "unfiltered-filter" % "0.8.2"
)
