package com.katlex.expenses.data

import org.json4s.native.{JsonMethods, Serialization}
import org.json4s.DefaultFormats

/**
 * Serializer domain object to and from JSON
 */
object Serializer {
  import JsonMethods._
  import Serialization._
  implicit val formats = DefaultFormats + IdSerializer

  def toJsonString(user:User) = write(user)
}
