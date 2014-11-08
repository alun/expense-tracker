package com.katlex.expenses

import scala.slick.driver.H2Driver.simple._
import java.security.MessageDigest
import java.sql.SQLException
import org.bson.types.ObjectId

package object data {

  def nextId = ObjectId.get()
  def now = new java.util.Date

  private lazy val dbUrl = sys.props.get("expenses.db").getOrElse("jdbc:h2:./db/expenses")
  lazy val db = Database.forURL(dbUrl, driver = "org.h2.Driver")

  implicit val objIdColumnType = MappedColumnType.base[ObjectId, String](
    { id => id.toString },
    { str => new ObjectId(str) }
  )
  
  case class User(id:ObjectId, email:String, password:String)
  case class Session(id:ObjectId, ownerId:ObjectId, timestamp:Long)

  class Users(tag: Tag) extends Table[User](tag, "USERS") {
    def id = column[ObjectId]("USER_ID", O.PrimaryKey)
    def email = column[String]("EMAIL")
    def password =  column[String]("PASSWD")
    def * = (id, email, password) <> (User.tupled, User.unapply)
  }
  val users = TableQuery[Users]

  class Sessions(tag:Tag) extends Table[Session](tag, "SESSIONS") {
    def id = column[ObjectId]("SESSION_ID", O.PrimaryKey)
    def ownerId = column[ObjectId]("USER_ID")
    def lastTouched = column[Long]("LAST_TOUCHED")

    def ownerFk = foreignKey("USER_FK", ownerId, users)(
      _.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

    def ownerIdx = index("USER_IDX", ownerId, unique = true)

    def * = (id, ownerId, lastTouched) <> (Session.tupled, Session.unapply)
  }
  val sessions = TableQuery[Sessions]

  /**
   * Optionaly creates db schema if it hasn't been created yet
   */
  def createSchemaOpt() {
    db withSession { implicit dbSession =>
      try {
        users.ddl.create
        sessions.ddl.create

        // test data
        users += User(nextId, "alun@katlex.com", "5236e55bf84d77acf0f9c2aef5751903")
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
   * Creates an MD5 hash of the password using the email as a salt
   * @param email
   * @param password
   * @return MD5 hash string
   */
  def password(email:String, password:String) = {
    def hex(v:Byte) = {
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
