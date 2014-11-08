package com.katlex.expenses
package sessions

import com.katlex.expenses.data.User

object SessionManager {

  val COOKIE = "expenses_session"
  val TTL = 10 * 60 * 1000

  def touch(user:User) = data.touchSession(user)

  def touch(sid:String) = data.touchSession(sid)

  def getUser(sid:String) = {
    data.clearSessions(TTL)
    data.getSessionUser(sid)
  }
}
