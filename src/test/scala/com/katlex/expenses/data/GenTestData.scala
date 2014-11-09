package com.katlex.expenses.data

import scala.slick.driver.H2Driver.simple._
import scala.util.Random
import scala.slick.driver.JdbcProfile
import org.bson.types.ObjectId
import java.sql.{Timestamp, Date}

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
    def randTs = new java.sql.Timestamp(now.getTime - (Math.random() * yearMilliseconds).toLong)
    def words = "gift kauf expense shop auto house appartment transport taxi wife".split(" ").toList
    def randWord = words(randInt(words.size))
    def randSentence = (0 to randInt(3)).map(_ => randWord).mkString(" ")

    for (_ <- 1 to 100) {
      expenses += Expense(nextId, uid, randTs, randSentence, BigDecimal(randInt(100000), 2), randSentence)
    }
  }

  def main(args:Array[String]) {

    withMemDb { implicit dbSession =>
      val uid = createTestUser
      genRandExpenses(uid)

      val week = SimpleFunction.unary[Timestamp, Int]("week")
      val year = SimpleFunction.unary[Timestamp, Int]("year")

      val q1 = (for {
        e <- expenses if year(e.timestamp) === 2014 && e.ownerId === uid
      } yield (week(e.timestamp), e.amount)).groupBy(_._1)

      val q2 = (for {
        (week, amount) <- q1
      } yield (week, amount.map(_._2).avg, amount.map(_._2).sum)).sortBy(_._1.desc)

      println {
        q2.list
      }
    }

  }
}
