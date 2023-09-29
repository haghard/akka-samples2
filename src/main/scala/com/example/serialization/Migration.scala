package com.example.serialization

@scala.annotation.implicitNotFound(
  "Not found migration from ${From} to ${To}. Consider adding it to the companion object."
)
sealed trait Migration[From, To] extends enumeratum.EnumEntry {
  def desc: String
}

object Migration extends enumeratum.Enum[Migration[_, _]] {

  implicit object OwnershipChanged1to2 extends Migration[Event.OwnershipChanged, Event.OwnershipChanged2] {
    val desc = "OwnershipChanged1~>2"
  }

  implicit object OwnershipChanged2to3 extends Migration[Event.OwnershipChanged2, Event.OwnershipChanged3] {
    val desc = "OwnershipChanged2~>3"
  }

  implicit object OwnershipChanged1to3 extends Migration[Event.OwnershipChanged, Event.OwnershipChanged3] {
    val desc = "OwnershipChanged1~>3"
  }

  implicit object Created1to2 extends Migration[Event.Created, Event.Created2] {
    val desc = "Created1~>2"
  }

  override val values: scala.collection.immutable.IndexedSeq[Migration[_, _]] = findValues

  def apply[From, To](implicit M: Migration[From, To]): Migration[From, To] = M
}
