package com.katlex.expenses
package data

import org.json4s._
import org.json4s.native.Serialization
import org.json4s.JsonAST.{JString, JNothing, JValue}
import scala.util.control.Exception
import org.bson.types.ObjectId
import java.sql.Timestamp
import org.json4s.MappingException

/**
 * Serializer domain object to and from JSON
 */
object Serializer {
  import Model._
  import Serialization._
  implicit val formats = DefaultFormats + Formats.id + Formats.timestamp

  def toJsonString(user:User) = write(user)
  def toJsonString(expense:Expense) = write(expense)
  def toJsonString(expensesOrStats:List[_]) = write(expensesOrStats)

  def fromJson[T : Manifest](expense:JValue) = {
    Exception.catching(classOf[Exception]).opt {
      read[T](write(expense))
    }
  }
}

object Formats {

  lazy val id = new CustomSerializer[ObjectId](implicit f => ({
    case JString(id) if ObjectId.isValid(id) => new ObjectId(id)
    case JNothing => null
    case x => throw new MappingException("Can't convert " + x + " to ObjectId")
  }, {
    case x: ObjectId => JString(x.toString)
  })
  )

  lazy val timestamp = new CustomSerializer[Timestamp](implicit f => ({
      case JInt(ts) => new Timestamp(ts.toLong)
      case x => throw new MappingException("Can't convert " + x + " to Timestamp")
    }, {
      case ts:Timestamp => JInt(ts.getTime)
    })
  )

}
