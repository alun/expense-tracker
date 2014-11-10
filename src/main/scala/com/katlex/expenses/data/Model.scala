package com.katlex.expenses.data

import scala.slick.driver.H2Driver.simple._
import org.bson.types.ObjectId
import scala.slick.lifted.Tag
import java.sql.Timestamp

/**
 * Domain model definition
 */
object Model {

  case class User(id:ObjectId, email:String, password:String)
  class Users(tag: Tag) extends Table[User](tag, "USERS") {
    def id = column[ObjectId]("USER_ID", O.PrimaryKey)
    def email = column[String]("EMAIL")
    def password =  column[String]("PASSWD")
    def * = (id, email, password) <> (User.tupled, User.unapply)
  }
  val users = TableQuery[Users]

  case class Session(id:ObjectId, ownerId:ObjectId, timestamp:Long)
  class Sessions(tag:Tag) extends Table[Session](tag, "SESSIONS") {
    def id = column[ObjectId]("SESSION_ID", O.PrimaryKey)
    def ownerId = column[ObjectId]("USER_ID")
    def lastTouched = column[Long]("LAST_TOUCHED")

    def ownerFk = foreignKey("SESSION_USER_FK", ownerId, users)(
      _.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

    def ownerIdx = index("SESSION_USER_IDX", ownerId, unique = true)

    def * = (id, ownerId, lastTouched) <> (Session.tupled, Session.unapply)
  }
  val sessions = TableQuery[Sessions]

  case class Expense(id:ObjectId, ownerId:ObjectId, timestamp:Timestamp,
                     description:String, amount:BigDecimal, comment:String)
  class Expenses(tag:Tag) extends Table[Expense](tag, "EXPENSES") {
    def id = column[ObjectId]("EXPENSE_ID", O.PrimaryKey)
    def ownerId = column[ObjectId]("USER_ID")
    def timestamp = column[Timestamp]("TIMESTAMP")
    def description = column[String]("DESCRIPTION")
    def amount = column[BigDecimal]("AMOUNT")
    def comment = column[String]("COMMENT")

    def ownerFk = foreignKey("EXPENSE_USER_FK", ownerId, users)(
      _.id, onUpdate=ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)

    def ownerIdx = index("EXPENSE_USER_IDX", ownerId)
    def descriptionIdx = index("EXPENSE_DESCRIPTION_IDX", description)
    def commentIdx = index("EXPENSE_COMMENT_IDX", comment)

    def * = (id, ownerId, timestamp, description, amount, comment) <> (Expense.tupled, Expense.unapply)
  }
  val expenses = TableQuery[Expenses]

  /**
   * Helper DTO class for week expenses statistics
   * @param year
   * @param week
   * @param totalExpenses
   * @param averageExpense
   * @param total
   */
  case class WeekStat(
                       year:Int,
                       week:Int,
                       totalExpenses:Int,
                       averageExpense:BigDecimal,
                       total:BigDecimal
                       )

}
