package com.example.serialization

/** GADTs - Parametrically polymorphic ADT, where you are allowed to specialize a type parameter in the terms of a sum
  * type. Specialization the type T of ETag inside the children together with being able to reconstruct this information
  * in pattern matching is known as GADTs.
  */
@scala.annotation.implicitNotFound("Not found ETag[${T}]. Consider adding it to the companion object.")
sealed trait ETag[+T] extends enumeratum.EnumEntry { self =>
  def manifest(): String = self.getClass().getName()
}

object ETag extends enumeratum.Enum[ETag[_]] {

  implicit object Created      extends ETag[Event.Created]
  implicit object Created2     extends ETag[Event.Created2]
  implicit object Deleted      extends ETag[Event.Deleted]
  implicit object Updated      extends ETag[Event.Updated]
  implicit object OwnershipCh  extends ETag[Event.OwnershipChanged]
  implicit object OwnershipCh2 extends ETag[Event.OwnershipChanged2]
  implicit object OwnershipCh3 extends ETag[Event.OwnershipChanged3]

  override val values: scala.collection.immutable.IndexedSeq[ETag[_]] = findValues

  def apply[T <: Event: ETag]: ETag[T] = implicitly[ETag[T]]

  def fromStr(
    manifest: String
  ): Either[String, ETag[Event]] =
    values.find(_.manifest() == manifest) match {
      case Some(tag) => Right(tag.asInstanceOf[ETag[Event]])
      case None      => Left(manifest)
    }
}
