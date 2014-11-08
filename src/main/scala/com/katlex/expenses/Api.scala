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

  object Parameters {
    val Password = "password"
  }

  object Codes {
    def MandatoryParameterMissing(param:String) = s"Mandatory parameter missing $param"
    val IncorrectLogin = "Incorrect login or password"
    val UserNameAlreadyTaken = "Such a user already exist"
  }

  import Codes._
  import Parameters._

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

  private def getParam[T](params:JValue, param:String, matcher:PartialFunction[JValue, T]) = {
    val p = (params \ param)
    if (matcher.isDefinedAt(p))
      Full(matcher(p))
    else
      Failure(MandatoryParameterMissing(param))
  }

  private def processApi:PartialFunction[HttpRequest[_], Box[JValue]] = {
    case req @ POST(Path(Seg("api" :: "users" :: login :: "login" :: Nil))) =>
      val params = parse(req.reader)
      for {
        user <- Box(data.user(login)) ?~ IncorrectLogin
        password <- getParam(params, Password, {
            case JString(p) => data.password(user.email, p)
          }).filter(_ == user.password) ?~ IncorrectLogin
      } yield {
        JString("ok")
      }

    case req @ POST(Path(Seg("api" :: "users" :: login :: "register" :: Nil))) =>
      val params = parse(req.reader)
      for {
        password <- getParam(params,  Password, {
            case JString(p) => data.password(login, p)
          })
        _ <- data.user(login) match {
            case Some(user) =>
              if (user.password == password)
                Full(())
              else
                Failure(UserNameAlreadyTaken)
            case None =>
              Full(data.addUser(login, password))
          }
      } yield {
        JString("ok")
      }

    case Path(Seg(path @ "api" :: _)) =>
      Failure(s"Bad api call ${path.mkString("/")}")
  }
}
