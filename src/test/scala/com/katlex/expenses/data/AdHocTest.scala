package com.katlex.expenses.data

import scala.slick.driver.H2Driver.simple._

object AdHocTest {

  def main(args:Array[String]) {

    sys.props += "expenses.db" -> "jdbc:h2:mem:test1;TRACE_LEVEL_FILE=3"

    db.withSession { implicit dbSession =>
      users.ddl.create
      sessions.ddl.create

      val uid = nextId
      users += User(uid, "some", password("some", "some"))
      val sid = nextId
      sessions += Session(sid, uid, now.getTime)
      println(sessions.list)
      println(
        touchSession(sid.toString)
      )
      println(sessions.list)
    }

  }

}
