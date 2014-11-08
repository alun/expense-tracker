package com.katlex.expenses

import java.io.OutputStreamWriter
import java.net.URL

import net.liftweb.util.Html5
import net.liftweb.util.BindHelpers._
import unfiltered.response.{ResponseWriter, HtmlContent, ComposeResponse}
import unfiltered.filter.Plan.Intent
import unfiltered.request.{Cookies, Path}
import com.katlex.expenses.sessions.SessionManager
import unfiltered.Cookie
import com.katlex.expenses.data.Serializer

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
      val cookies = Cookies.unapply(req).get
      Page("Expenses tracker", cookies.get(SessionManager.COOKIE).flatMap(x => x))
  }

  def assets = new URL(getClass.getResource("/www/robots.txt"), ".")
  
  def htmlResource(name:String*) = {
    Html5.parse(
      getClass.getResource(s"/html/${name.mkString("/")}.html").openStream()
    ).openOrThrowException("Bad source html!")
  }
  
  //lazy val defaultFrame = htmlResource("default")
  def defaultFrame = htmlResource("default")

  def apply(title:String, sid:Option[Cookie]) = Response {
    val transform = 
      "title *" #> title &
      "#user [ng-init]" #> sid.flatMap(c => SessionManager.getUser(c.value)).map { user =>
        "user = " + Serializer.toJsonString(user)
      }
    
    transform(defaultFrame)
  }
}
