package com.katlex.expenses.data

import scala.slick.driver.H2Driver.simple._
import scala.util.Random
import scala.slick.driver.JdbcProfile
import org.bson.types.ObjectId

object GenTestData {
  import Model._

  def withRealDb[T](body: JdbcProfile#Backend#Session => T) = {
    db.withSession(body)
  }

  def withMemDb[T](body: JdbcProfile#Backend#Session => T) = {
    sys.props += "expenses.db" -> "jdbc:h2:mem:test1;TRACE_LEVEL_FILE=3"

    db.withSession { implicit dbSession =>
      users.ddl.create
      sessions.ddl.create
      expenses.ddl.create

      body(dbSession)
    }
  }

  def createTestUser(implicit session: JdbcProfile#Backend#Session) = {
    users.filter(_.email === "test@data.com").delete

    val uid = nextId
    users += User(uid, "test@data.com", password("test@data.com", "pwd"))
    uid
  }

  def genRandExpenses(uid:ObjectId)(implicit dbSession: JdbcProfile#Backend#Session) {
    val yearMilliseconds = 52L * 7 * 24 * 60 * 60 * 1000

    def randInt(max:Int) = Random.nextInt(max)
    def randTs = now.getTime - (Math.random() * yearMilliseconds).toLong
    def words = "gift kauf expense shop auto house appartment transport taxi wife".split(" ").toList
    def randWord = words(randInt(words.size))
    def randSentence = (0 to randInt(3)).map(_ => randWord).mkString(" ")

    for (_ <- 1 to 100) {
      expenses += Expense(nextId, uid, randTs, randSentence, BigDecimal(randInt(100000), 2), randSentence)
    }
  }

  def main(args:Array[String]) {

    withRealDb { implicit dbSession =>
      val uid = createTestUser
      genRandExpenses(uid)

      println {
        expenses.list
      }
    }

  }
}
