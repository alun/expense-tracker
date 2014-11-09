package com.katlex.expenses
package data

import org.json4s.native.{JsonMethods, Serialization}
import org.json4s.DefaultFormats
import org.json4s.JsonAST.JValue
import scala.util.control.Exception

/**
 * Serializer domain object to and from JSON
 */
object Serializer {
  import Model._
  import JsonMethods._
  import Serialization._
  implicit val formats = DefaultFormats + IdSerializer

  def toJsonString(user:User) = write(user)
  def toJsonString(expense:Expense) = write(expense)
  def toJsonString(expenses:List[Expense]) = write(expenses)

  def fromJson[T : Manifest](expense:JValue) = {
    Exception.catching(classOf[Exception]).opt {
      read[T](write(expense))
    }
  }
}
