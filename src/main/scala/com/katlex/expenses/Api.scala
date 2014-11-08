package com.katlex.expenses

import unfiltered.filter.Plan.Intent
import unfiltered.request._
import org.json4s.native.{Serialization, JsonMethods}
import unfiltered.response._
import org.json4s.JsonAST.{JObject}
import org.json4s.ParserUtil.ParseException
import scala.text.Document
import org.json4s._
import scala.util.control.Exception
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import net.liftweb.common.{Empty, Box, Failure}
import net.liftweb.common.Full
import scala.Some
import com.katlex.expenses.data.{Serializer, User}
import org.json4s.JsonAST.JString
import unfiltered.response.ResponseString
import unfiltered.Cookie
import com.katlex.expenses.sessions.SessionManager
import java.util.Date

object Api {
  import JsonMethods._

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

  def jsonResponse(response:JValue) = JsonContent ~> ResponseString(printer(render(response)))
  def jsonStringResponse(response:String) = JsonContent ~> ResponseString(response)

  implicit def boxedJValueAsJsonResponse(response:Box[JValue]) = response.map(jsonResponse)

  def intent:Intent = new Intent {
    def isDefinedAt(x: HttpRequest[HttpServletRequest]): Boolean =
      processApi.isDefinedAt(x)

    def apply(x: HttpRequest[HttpServletRequest]): ResponseFunction[HttpServletResponse] = {
      val result = Exception.catching(classOf[ParseException]).either(processApi(x)) .left.map {
        case e:ParseException => new Failure("Bad JSON request format", Full(e), Empty)
      }
      (result.right.toOption orElse result.left.toOption).get match {
        case Full(v) => v
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

  private def setSession(user:User) = {
    val sid = SessionManager.touch(user)
    SetCookies(Cookie(SessionManager.COOKIE, sid, None, Some("/"), Some(SessionManager.TTL / 1000)))
  }

  private def loginResponse(user:User) =
    setSession(user) ~>
    jsonStringResponse(Serializer.toJsonString(user))

  private def processApi:PartialFunction[HttpRequest[_], Box[ResponseFunction[Any]]] = {
    case req @ POST(Path(Seg("api" :: "users" :: login :: "login" :: Nil))) =>
      val params = parse(req.reader)
      for {
        user <- Box(data.getUser(login)) ?~ IncorrectLogin
        password <- getParam(params, Password, {
            case JString(p) => data.password(user.email, p)
          }).filter(_ == user.password) ?~ IncorrectLogin
      } yield loginResponse(user)

    case req @ POST(Path(Seg("api" :: "users" :: login :: "register" :: Nil))) =>
      val params = parse(req.reader)
      for {
        password <- getParam(params,  Password, {
            case JString(p) => data.password(login, p)
          })
        user <- data.getUser(login) match {
            case Some(user) =>
              if (user.password == password)
                Full(user)
              else
                Failure(UserNameAlreadyTaken)
            case None =>
              Full(data.addUser(login, password))
          }
      } yield loginResponse(user)

    case req @ POST(Path(Seg("api" :: "users" :: id :: "logout" :: Nil))) =>
      Full(
        SetCookies(Cookie(
          SessionManager.COOKIE, "", None, Some("/"), Some(- (new Date().getTime / 1000).toInt)
        )) ~> jsonResponse(JString("ok"))
      )

    case Path(Seg(path @ "api" :: _)) =>
      Failure(s"Bad api call ${path.mkString("/")}")
  }
}
