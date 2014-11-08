package com.katlex.expenses

import scala.slick.driver.H2Driver.simple._
import java.security.MessageDigest
import java.sql.SQLException
import org.bson.types.ObjectId

package object data {

  lazy val base = Database.forURL("jdbc:h2:./db/expenses", driver = "org.h2.Driver")

  implicit val objIdColumnType = MappedColumnType.base[ObjectId, String](
    { id => id.toString },
    { str => new ObjectId(str) }
  )
  
  case class User(id:ObjectId, email:String, password:String)

  class Users(tag: Tag) extends Table[(ObjectId, String, String)](tag, "USERS") {
    def id = column[ObjectId]("USER_ID", O.PrimaryKey)
    def email = column[String]("EMAIL")
    def password =  column[String]("PASSWD")
    def * = (id, email, password)
  }
  val users = TableQuery[Users]

  def createSchemaOpt() {
    base withSession { implicit session =>
      try {
        users.ddl.create
        users += (ObjectId.get(), "alun@katlex.com", "5236e55bf84d77acf0f9c2aef5751903")
      } catch {
        case e:SQLException => // probably database was already created
      }
    }
  }

  def user(email:String):Option[User] = base withSession { implicit  session =>
    (users.filter(_.email === email).list match {
      case user :: _ => Some(user)
      case _ => None
    }).map(User.tupled)
  }

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
