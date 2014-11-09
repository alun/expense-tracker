package com.katlex.expenses.data

import org.bson.types.ObjectId

object Id {
  def unapply(s:String):Option[ObjectId] =  {
    if (ObjectId.isValid(s)) Some(new ObjectId(s))
    else None
  }
}
