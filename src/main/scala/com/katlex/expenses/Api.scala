package com.katlex.expenses

import unfiltered.filter.Plan.Intent
import unfiltered.request.{HttpRequest, Path, POST, Seg}
import org.json4s.native.{Serialization, JsonMethods}
import unfiltered.response._
import org.json4s.JsonAST.{JValue, JObject, JString}
import org.json4s.ParserUtil.ParseException
import scala.text.Document
import org.json4s.{DefaultFormats}
import scala.util.control.Exception
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import net.liftweb.common.{Empty, Box, Failure, Full}
import net.liftweb.common.Full
import unfiltered.response.ResponseString

object Api {
  import JsonMethods._
  import Serialization._

  val printer:Document => String = pretty _
  //val printer:Document => String = compact _

  implicit val formats = DefaultFormats

  def jsonResponse(response:JValue) = JsonContent ~> ResponseString(printer(render(response)))

  def intent:Intent = new Intent {
    def isDefinedAt(x: HttpRequest[HttpServletRequest]): Boolean =
      processApi.isDefinedAt(x)

    def apply(x: HttpRequest[HttpServletRequest]): ResponseFunction[HttpServletResponse] = {
      val result = Exception.catching(classOf[ParseException]).either(processApi(x)) .left.map {
        case e:ParseException => new Failure("Bad JSON request format", Full(e), Empty)
      }
      (result.right.toOption orElse result.left.toOption).get match {
        case Full(v) => jsonResponse(v)
        case Failure(msg, _, _) => BadRequest ~> jsonResponse(error(msg))
        case Empty => InternalServerError ~> jsonResponse(error("Unknown error"))
      }
    }
  }

  private def error(code:String) =
    JObject(
      ("status", JString("error")),
      ("code", JString(code)))

  private def processApi:PartialFunction[HttpRequest[_], Box[JValue]] = {
    case req @ POST(Path(Seg("api" :: "users" :: login :: "login" :: Nil))) =>
      val badLogin = "Incorrect login or password"
      val params = parse(req.reader)
      (for {
        user <- Box(data.user(login)) ?~ badLogin
        password <- (params \ "password") match {
            case JString(p) => Full(data.password(user.email, p))
            case _ => Failure("Password parameter should be passed")
          }
        if user.password == password
      } yield {
        JString("ok")
      }) ?~ badLogin
    case Path(Seg(path @ "api" :: _)) =>
      Failure(s"Bad api call ${path.mkString("/")}")
  }
}
