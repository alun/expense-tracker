package com.katlex.expenses

import java.io.OutputStreamWriter
import java.net.URL

import net.liftweb.util.Html5
import net.liftweb.util.BindHelpers._
import unfiltered.response.{ResponseWriter, HtmlContent, ComposeResponse}
import unfiltered.filter.Plan.Intent
import unfiltered.request.Path
import com.katlex.expenses.data.Serializer
import com.katlex.expenses.data.Model.User

object Page {
  
  case class Response(nodes: scala.xml.NodeSeq) extends ComposeResponse(HtmlContent ~> new ResponseWriter {
    def write(w: OutputStreamWriter) {
      val html = nodes.head match {
        case <html>{_*}</html> => nodes.head
        case _ => <html>{nodes}</html>
      }
      
      Html5.write(html, w, true, false)
    }
  })

  def intent:Intent = {
    case req @ (Path("/") | Path("/index") | Path("/index.html")) =>
      Page("Expenses tracker", Util.sessionUser(req))
  }

  def assets = new URL(getClass.getResource("/www/robots.txt"), ".")
  
  def htmlResource(name:String*) = {
    Html5.parse(
      getClass.getResource(s"/html/${name.mkString("/")}.html").openStream()
    ).openOrThrowException("Bad source html!")
  }
  
  //lazy val defaultFrame = htmlResource("default")
  def defaultFrame = htmlResource("default")

  def apply(title:String, user:Option[User]) = Response {
    val transform = 
      "title *" #> title &
      "#user [ng-init]" #> user.map { user =>
        "user = " + Serializer.toJsonString(user)
      }
    
    transform(defaultFrame)
  }
}
