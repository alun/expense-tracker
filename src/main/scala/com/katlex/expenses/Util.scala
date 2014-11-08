package com.katlex.expenses

import unfiltered.request.{Cookies, HttpRequest}
import com.katlex.expenses.sessions.SessionManager

object Util {
  def sessionId(req:HttpRequest[_]) = {
    val cookies = Cookies.unapply(req).get
    cookies.get(SessionManager.COOKIE).flatMap(x => x).map(_.value)
  }

  def sessionUser(req:HttpRequest[_]) = sessionId(req).flatMap(SessionManager.getUser)
}
