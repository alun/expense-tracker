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
import com.katlex.expenses.data.{Serializer}
import com.katlex.expenses.data.Model.User
import org.json4s.JsonAST.JString
import unfiltered.response.ResponseString
import unfiltered.Cookie
import com.katlex.expenses.sessions.SessionManager
import java.util.Date

object Api {
  import JsonMethods._

  object Parameters {
    val Password  = "password"
    val Skip      = "Skip"
    val Limit     = "Limit"
    val Filter    = "Filter"
  }

  object Codes {
    def MandatoryParameterMissing(param:String) = s"Mandatory parameter missing $param"
    val IncorrectLogin = "Incorrect login or password"
    val UserNameAlreadyTaken = "Such a user already exist"
    val UnauthorizedRequest = "Unauthorized request"
  }

  import Codes._
  import Parameters._
  import Box._

  val printer:Document => String = pretty _
  //val printer:Document => String = compact _

  def jsonResponse(response:JValue) = JsonContent ~> ResponseString(printer(render(response)))
  def jsonStringResponse(response:String) = JsonContent ~> ResponseString(response)

  implicit def boxedJValueAsJsonResponse(response:Box[JValue]) = response.map(jsonResponse)

  def intent:Intent = new Intent {
    def isDefinedAt(x: HttpRequest[HttpServletRequest]): Boolean =
      processApi.isDefinedAt(x)

    def apply(x: HttpRequest[HttpServletRequest]): ResponseFunction[HttpServletResponse] = {
      val result = Exception.catching(classOf[ParseException]).either(
        processApi(parse(x.reader))(x)).left.map {
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

  private def getParam[T](param:String, matcher:PartialFunction[JValue, T])(implicit params:JValue) = {
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

  private def processApi(implicit params:JValue = null):PartialFunction[HttpRequest[_], Box[ResponseFunction[Any]]] = {
    case req @ POST(Path(Seg("api" :: "users" :: login :: "login" :: Nil))) =>
      for {
        user <- Box(data.getUser(login)) ?~ IncorrectLogin
        password <- getParam(Password, {
            case JString(p) => data.password(user.email, p)
          }).filter(_ == user.password) ?~ IncorrectLogin
      } yield loginResponse(user)

    case req @ POST(Path(Seg("api" :: "users" :: login :: "register" :: Nil))) =>
      for {
        password <- getParam(Password, {
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

    case req @ GET(Path(Seg("api" :: "users" :: userId :: "expenses" :: Nil))) =>
      for {
        user <- Util.sessionUser(req).filter(_.id == userId) ?~ UnauthorizedRequest
        skip <- getParam(Skip, {
            case JDecimal(p) => p.toInt
            case _ => 0
          })
        limit <- getParam(Limit, {
            case JDecimal(p) => p.toInt
            case _ => 10
          })
        filter <- getParam(Filter, {
          case JString(filter) => Some(filter)
          case _ => None
        })
      } yield jsonStringResponse(
        Serializer.toJsonString(
          data.getExpenses(
            user,
            skip = skip,
            limit = limit,
            filter = filter))
      )

    case Path(Seg(path @ "api" :: _)) =>
      Failure(s"Bad api call ${path.mkString("/")}")
  }
}
