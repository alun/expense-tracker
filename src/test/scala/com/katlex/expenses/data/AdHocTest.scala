package com.katlex.expenses
package data

import scala.slick.driver.H2Driver.simple._

object AdHocTest {
  import Model._

  def main(args:Array[String]) {

    sys.props += "expenses.db" -> "jdbc:h2:mem:test1;TRACE_LEVEL_FILE=3"

    db.withSession { implicit dbSession =>
      users.ddl.create
      sessions.ddl.create
      expenses.ddl.create

      val uid = nextId
      users += User(uid, "some", password("some", "some"))
      val sid = nextId
      sessions += Session(sid, uid, now.getTime)

      expenses += Expense(nextId, uid, now, "my first expense", 10, "hello")
      expenses += Expense(nextId, uid, now, "my second expense", 20.02d, "")
      expenses += Expense(nextId, uid, now, "unexpected", 100.100d, "")

      println {
        (for {
          e <- expenses if (e.description like "%my%") || (e.comment like "%my%")
        } yield e).size.run
      }
    }

  }

  def getAlun = {
    db.withSession { implicit dbSession =>
      (for {
        u <- users if u.email === "alun@katlex.com"
      } yield u).list.headOption.get
    }
  }

}
