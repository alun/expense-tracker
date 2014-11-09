package com.katlex.expenses
import org.specs2.mutable._
import org.json4s.native.JsonMethods
import org.json4s.JsonAST.JString
import com.ning.http.client.Response
import org.json4s.{JArray, JValue}
import com.ning.http.client.cookie.Cookie

class ApiSpec extends Specification {

  import dispatch._
  import Defaults._
  import scala.concurrent.duration._
  import JsonMethods._
  import scala.collection.JavaConversions._

  def asJson: Response => JValue = r => parse(r.getResponseBody)
  def asJsonWithCookes: Response => (JValue, List[Cookie]) = r => (asJson(r), r.getCookies.toList)
  def toString(jv:JValue) = pretty(render(jv))

  implicit class WithCookies(req:Req) {
    def withCookies(cookies:List[Cookie]) =
      cookies.foldLeft(req) { (req, c) =>
        req.addOrReplaceCookie(c)
      }
  }

  lazy val testHost = host("localhost", 8080)
  lazy val apiPrefix = testHost / "api"
  def createLogin(user:String) = apiPrefix / "users" / user / "login"
  def expenses(userId:String) = apiPrefix / "users" / userId / "expenses"

  lazy val login = createLogin("alun@katlex.com")

  sys.props ++ Map(
    "http.proxyHost" -> "localhost",
    "http.proxyPort" -> "8888",
    "com.ning.http.client.AsyncHttpClientConfig.useProxyProperties" -> "true"
  )

  val client = Http

  lazy val loginWithCredentials = login <<
    """
      |{
      | "email": "alun@katlex.com"
      | "password": "qwerqwer"
      |}
    """.stripMargin

  "The Api" should {
    "should't handle login GET request" in {
      (for {
        json <- client(login > asJson)
      } yield json \\ "status" === JString("error")).await
    }

    "but should handle same login with with POST" in {
      (for {
        json <- client(loginWithCredentials OK asJson)
      } yield json \\ "email" === JString("alun@katlex.com")).await
    }
    
    "should be able to list expenses when logged in" in {
      (for {
        (json, cookies) <- client(loginWithCredentials OK asJsonWithCookes)
        JString(userId) = json \\ "id"
        json <- client(expenses(userId).withCookies(cookies) OK asJson)
      } yield json.isInstanceOf[JArray] === true).await
    }
  }

}
