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
import com.katlex.expenses.data.{Id, Serializer}
import com.katlex.expenses.data.Model.{Expense, User}
import org.json4s.JsonAST.JString
import unfiltered.response.ResponseString
import unfiltered.Cookie
import com.katlex.expenses.sessions.SessionManager
import java.util.Date
import org.bson.types.ObjectId

object Api {
  import JsonMethods._

  object Parameters {
    val Password  = "password"
    val Skip      = "skip"
    val Limit     = "limit"
    val Filter    = "filter"
  }

  object Codes {
    def MandatoryParameterMissing(param:String) = s"Mandatory parameter missing $param"
    val IncorrectLogin = "Incorrect login or password"
    val UserNameAlreadyTaken = "Such a user already exist"
    val UnauthorizedRequest = "Unauthorized request"
    val BadInput = "Bad input"
    val DatabaseError = "Database error"
  }

  import Codes._
  import Parameters._
  import Box._

  val printer:Document => String = compact _

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

  private def jsonParam[T](param:String, matcher:PartialFunction[JValue, T])(implicit params:JValue) = {
    val p = (params \ param)
    if (matcher.isDefinedAt(p))
      Full(matcher(p))
    else
      Failure(MandatoryParameterMissing(param))
  }

  private def requestParam[T](req:HttpRequest[_], name:String,
                              transform:String => Option[T], defaultValue: Option[T] = None) = {
    req.parameterValues(name).headOption.flatMap(transform) orElse defaultValue
  }

  private def stringParam(req:HttpRequest[_], name:String, defaultValue:Option[String]) =
    requestParam(req, name, Some(_), defaultValue)

  private def intParam(req:HttpRequest[_], name:String, defaultValue:Option[Int]) =
    requestParam(req, name, s => Exception.catching(classOf[NumberFormatException]).opt(s.toInt), defaultValue)

  private def updateSession(user:User) = {
    val sid = SessionManager.touch(user)
    SetCookies(Cookie(SessionManager.COOKIE, sid, None, Some("/"), Some(SessionManager.TTL / 1000)))
  }

  private def loginResponse(user:User) =
    updateSession(user) ~>
    jsonStringResponse(Serializer.toJsonString(user))

  private def authorize(req:HttpRequest[_], userId:ObjectId) =
    Util.sessionUser(req).filter(_.id == userId) ?~ UnauthorizedRequest

  private def processApi(implicit bodyParams:JValue = null):PartialFunction[HttpRequest[_], Box[ResponseFunction[Any]]] = {
    case req @ POST(Path(Seg("api" :: "users" :: login :: "login" :: Nil))) =>
      for {
        user <- Box(data.getUser(login)) ?~ IncorrectLogin
        password <- jsonParam(Password, {
            case JString(p) => data.password(user.email, p)
          }).filter(_ == user.password) ?~ IncorrectLogin
      } yield loginResponse(user)

    case req @ POST(Path(Seg("api" :: "users" :: login :: "register" :: Nil))) =>
      for {
        password <- jsonParam(Password, {
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

    case req @ POST(Path(Seg("api" :: "users" :: Id(id) :: "logout" :: Nil))) =>
      Full(
        SetCookies(Cookie(
          SessionManager.COOKIE, "", None, Some("/"), Some(- (new Date().getTime / 1000).toInt)
        )) ~> jsonResponse(JString("ok"))
      )

    case req @ GET(Path(Seg("api" :: "users" :: Id(userId) :: "expenses" :: Nil))) =>
      for {
        user <- authorize(req, userId)
        skip <- intParam(req, Skip, Some(0))
        limit <- intParam(req, Limit, Some(10))
        filter = stringParam(req, Filter, None)
      } yield updateSession(user) ~> jsonStringResponse(
          Serializer.toJsonString(
            data.getExpenses(
              user,
              skip = skip,
              limit = limit,
              filter = filter))
        )

    case req @ POST(Path(Seg("api" :: "users" :: Id(userId) :: "expenses" :: Nil))) =>
      for {
        user <- authorize(req, userId)
        expense <- (Serializer.fromJson[Expense](bodyParams) ?~ BadInput).filter { e =>
            e.ownerId == user.id && e.id == null
          } ?~ UnauthorizedRequest
        savedExpense <- data.saveExpense(user, expense) ?~ DatabaseError
      } yield {
        updateSession(user) ~> jsonStringResponse(Serializer.toJsonString(savedExpense))
      }

    case req @ POST(Path(Seg("api" :: "users" :: Id(userId) :: "expenses" :: Id(expenseId) :: Nil))) =>
      for {
        user <- authorize(req, userId)
        expense <-
          ((Serializer.fromJson[Expense](bodyParams) ?~ BadInput).filter { e =>
            e.ownerId == user.id
          } ?~ UnauthorizedRequest).filter { e =>
            e.id == expenseId
          } ?~ BadInput
        updatedExpense <- data.updateExpense(expense) ?~ DatabaseError
      } yield {
        updateSession(user) ~> jsonStringResponse(Serializer.toJsonString(updatedExpense))
      }

    case req @ DELETE(Path(Seg("api" :: "users" :: Id(userId) :: "expenses" :: Id(expenseId) :: Nil))) =>
      for {
        user <- authorize(req, userId)
        expense <- (data.getExpense(expenseId) ?~ BadInput).filter { e =>
            e.ownerId == user.id
          } ?~ UnauthorizedRequest
        _ <- Full(()).filter(_ => data.removeExpense(expense) == 1) ?~ DatabaseError
      } yield updateSession(user) ~> jsonStringResponse(Serializer.toJsonString(expense))

    case req @ GET(Path(Seg("api" :: "users" :: Id(userId) :: "expenses" :: "stats" :: Nil))) =>
      for {
        user <- authorize(req, userId)
        stats = data.getWeekStats(user)
      } yield updateSession(user) ~> jsonStringResponse(Serializer.toJsonString(stats))

    case Path(Seg(path @ "api" :: _)) =>
      Failure(s"Bad api call ${path.mkString("/")}")
  }
}
