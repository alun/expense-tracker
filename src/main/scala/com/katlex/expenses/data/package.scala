package com.katlex.expenses

import scala.slick.driver.H2Driver.simple._
import java.security.MessageDigest
import java.sql.SQLException
import org.bson.types.ObjectId

package object data {

  import Model._

  def nextId = ObjectId.get()
  def now = new java.util.Date

  private lazy val dbUrl = sys.props.get("expenses.db").getOrElse("jdbc:h2:./db/expenses")
  lazy val db = Database.forURL(dbUrl, driver = "org.h2.Driver")

  implicit val objIdColumnType = MappedColumnType.base[ObjectId, String](
    { id => id.toString },
    { str => new ObjectId(str) }
  )
  
  /**
   * Optionally creates db schema if it hasn't been created yet
   */
  def createSchemaOpt() {
    db withSession { implicit dbSession =>
      try {
        users.ddl.create
        sessions.ddl.create
        expenses.ddl.create
      } catch {
        case e:SQLException => // probably database was already created
      }
    }
  }

  /**
   * Finds the user by given email adress
   * @param email
   * @return optional user object
   */
  def getUser(email:String):Option[User] = db withSession { implicit dbSession =>
    users.filter(_.email === email).list.headOption
  }

  /**
   * Adds new user with given email and password
   * @param email
   * @param passwordHash
   * @return db insertion result
   */
  def addUser(email:String, passwordHash:String) = db withSession { implicit dbSession =>
    val user = User(nextId, email, passwordHash)
    users += user
    user
  }

  /**
   * Creates new session or refreshes exising
   * @param user a user of a session
   * @return the touched session id
   */
  def touchSession(user:User) = db.withSession { implicit dbSession =>
    try {
      val sid = nextId
      sessions += Session(sid, user.id, now.getTime)
      sid.toString
    }
    catch {
      case e:SQLException =>
        val q = for {
          s <- sessions
          if s.ownerId === user.id
        } yield s.lastTouched
        q.update(now.getTime)
        sessions.filter(_.ownerId === user.id).map(_.id).list.headOption.get.toString
    }
  }

  /**
   * Touches a session by id, doesn't create a new session if such a session isn't exist
   * @param id
   * @return a session id
   */
  def touchSession(id:String) = db.withSession { implicit dbSession =>
    (for {
      s <- sessions if s.id === new ObjectId(id)
    } yield s.lastTouched).update(now.getTime)
    id
  }

  /**
   * Clears old session
   * @param ttl milliseconds from now to be considered old
   * @return number of removed sessions
   */
  def clearSessions(ttl:Long) = db.withSession { implicit dbSession =>
    (for {
      s <- sessions if s.lastTouched < now.getTime - ttl
    } yield s).delete
  }

  /**
   * Returns session user
   * @param id id of a session
   * @return a user of the session
   */
  def getSessionUser(id:String) = db.withSession { implicit dbSession =>
    (for {
      s <- sessions if s.id === new ObjectId(id)
      u <- users if u.id === s.ownerId
    } yield u).list.headOption
  }

  /**
   * Produces the list of expenses for the given user using given skip/limit values
   * and filter for comments and description
   * @param user
   * @param skip
   * @param limit
   * @param filter
   * @return
   */
  def getExpenses(user:User, skip:Int = 0, limit:Int = 10, filter:Option[String] = None) =
    db.withSession { implicit dbSession =>
      val query = filter.map(v => s"%$v%").map { v =>
        (for {
          e <- expenses
            if (e.description.toLowerCase like v.toLowerCase) ||
               (e.comment.toLowerCase like v.toLowerCase)
        } yield e)
      } .getOrElse(expenses)
      query.filter(_.ownerId === user.id).sortBy(_.timestamp.desc).drop(skip).take(limit).list
    }

  /**
   * Finds a single expense by id
   * @param id
   * @return
   */
  def getExpense(id:ObjectId) = db.withSession { implicit dbSesssion =>
    (for {
      e <- expenses if e.id === id
    } yield e).list.headOption
  }

  /**
   * Saves new expense for given user to database
   * @param user
   * @param expense
   */
  def saveExpense(user:User, expense:Expense) = db.withSession { implicit dbSession =>
    val e =
      if (expense.id == null) expense.copy(id = nextId, ownerId = user.id)
      else expense
    if ((expenses += e) == 1) Some(e)
    else None
  }

  /**
   * Updates exising expense in database
   * @param expense
   * @return
   */
  def updateExpense(expense:Expense) = db.withSession { implicit dbSession =>
    val res = (for {
      e <- expenses if e.id === expense.id && e.ownerId === expense.ownerId
    } yield e).update(expense)

    if (res == 1) Some(expense)
    else None
  }

  /**
   * Removes expense from db
   * @param expense
   * @return 1 if ok, 0 otherwise
   */
  def removeExpense(expense:Expense) = db.withSession { implicit dbSession =>
    (for {
      e <- expenses if e.id === expense.id
    } yield e).delete
  }

  /**
   * Creates an MD5 hash of the password using the email as a salt
   * @param email
   * @param password
   * @return MD5 hash string
   */
  def password(email: String, password: String) = {
    def hex(v: Byte) = {
      val s = Integer.toHexString(v.toInt & 0xFF)
      if (s.length == 1)
        "0" + s
      else
        s
    }
    val digest = MessageDigest.getInstance("MD5")
    digest.update(email.getBytes())
    digest.update(password.getBytes())
    val digits = Seq(digest.digest(): _*)
    digits.map(hex).mkString("")
  }
}
