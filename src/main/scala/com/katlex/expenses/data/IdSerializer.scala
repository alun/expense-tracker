package com.katlex.expenses.data

import org.json4s._
import org.bson.types.ObjectId
import org.json4s.JsonAST.JString

object IdSerializer extends Serializer[ObjectId] {
  private val ObjectIdClass = classOf[ObjectId]
  def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), ObjectId] = {
    case (TypeInfo(ObjectIdClass, _), json) => json match {
      case JString(id) if ObjectId.isValid(id) => new ObjectId(id)
      case x => throw new MappingException("Can't convert " + x + " to ObjectId")
    }
  }
  def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case x: ObjectId => JString(x.toString)
  }
}
