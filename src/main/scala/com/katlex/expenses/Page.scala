package com.katlex.expenses

import java.net.URL

import net.liftweb.util.Html5
import net.liftweb.util.BindHelpers._

object Page {

  def assets = new URL(getClass.getResource("/www/robots.txt"), ".")
  
  def htmlResource(name:String*) = {
    Html5.parse(
      getClass.getResource(s"/html/${name.mkString("/")}.html").openStream()
    ).openOrThrowException("Bad source html!")
  }
  
  lazy val defaultFrame = htmlResource("default")

  def apply(title:String) = unfiltered.response.Html5 {
    val transform = 
      "title *" #> title
    
    transform(defaultFrame)
  }
}
