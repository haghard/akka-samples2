package com.example.serialization

import com.example.serialization2.BidiPatch

import java.time.Instant
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

final case class UserId(value: Long) extends AnyVal

sealed trait Event { self =>

  def tag(): ETag[Event] =
    self match {
      case _: Event.Created           => ETag[Event.Created]
      case _: Event.Created2          => ETag[Event.Created2]
      case _: Event.Deleted           => ETag[Event.Deleted]
      case _: Event.Updated           => ETag[Event.Updated]
      case _: Event.OwnershipChanged  => ETag[Event.OwnershipChanged]
      case _: Event.OwnershipChanged2 => ETag[Event.OwnershipChanged2]
      case _: Event.OwnershipChanged3 => ETag[Event.OwnershipChanged3]
    }
}

object Event {

  final case class Created(id: Long, userId: UserId, when: Instant) extends Event
  object Created {
    def _2 = new BidiPatch[Event.Created, Event.Created2] {
      override def forward: Created => Created2  = ???
      override def backward: Created2 => Created = ???
    }
  }

  final case class Created2(id: Long, creator: UserId, purpose: String, when: Instant) extends Event

  final case class Updated(id: Long, userId: UserId, when: Instant) extends Event

  final case class Deleted(id: Long, userId: UserId, when: Instant) extends Event

  final case class OwnershipChanged(
    id: Long,
    firstName: String,
    lastName: String,
    newOwner: UserId,
    userId: UserId,
    when: Instant
  ) extends Event

  object OwnershipChanged {
    def _2 = new BidiPatch[Event.OwnershipChanged, Event.OwnershipChanged2] {
      override def forward: Event.OwnershipChanged => Event.OwnershipChanged2  = ???
      override def backward: Event.OwnershipChanged2 => Event.OwnershipChanged = ???
    }

    def _3 = new BidiPatch[Event.OwnershipChanged2, Event.OwnershipChanged3] {
      override def forward: Event.OwnershipChanged2 => Event.OwnershipChanged3  = ???
      override def backward: Event.OwnershipChanged3 => Event.OwnershipChanged2 = ???
    }

    def _ff3 = new BidiPatch[Event.OwnershipChanged, Event.OwnershipChanged3] {
      override def forward: Event.OwnershipChanged => Event.OwnershipChanged3  = ???
      override def backward: Event.OwnershipChanged3 => Event.OwnershipChanged = ???
    }
  }

  final case class OwnershipChanged2(
    id: Long,
    fullName: String, // firstName + lastName
    newOwner: UserId,
    userId: UserId,
    when: Instant
  ) extends Event

  final case class OwnershipChanged3(
    id: Long,
    fullName: String,
    newOwner: UserId,
    userId: UserId,
    label: String, // new field
    when: Instant
  ) extends Event

  implicit class ByteOps(val bytes: Array[Byte]) extends AnyVal {

    def deserialize[T <: Event](implicit tag: ETag[T]): T = {
      val bb = ByteBuffer.wrap(bytes)
      tag match {
        case ETag.Created =>
          // Read it back from bb
          Event.Created(0, UserId(1), Instant.now())
        case ETag.Created2 =>
          Event.Created2(0, UserId(1), "bla", Instant.now())
        case ETag.Deleted =>
          Event.Deleted(0, UserId(1), Instant.now())
        case ETag.Updated =>
          Event.Updated(0, UserId(1), Instant.now())
        case ETag.OwnershipCh =>
          Event.OwnershipChanged(0, "John", "Doe", UserId(11), UserId(1), Instant.now())
        case ETag.OwnershipCh2 =>
          Event.OwnershipChanged2(0, "John Doe", UserId(11), UserId(1), Instant.now())
        case ETag.OwnershipCh3 =>
          Event.OwnershipChanged3(0, "John Doe", UserId(11), UserId(1), "label", Instant.now())
      }
    }
  }

  implicit class EventOps[T <: Event](val event: T) extends AnyVal {

    def serialize(implicit tag: ETag[T]): Array[Byte] =
      serialize0[T](event, tag.manifest())

    private def serialize0[E <: Event](ev: E, manifest: String)(implicit tag: ETag[E]): Array[Byte] = {
      // not necessary
      val manifestBts = manifest.getBytes(StandardCharsets.UTF_8)
      val bb =
        ByteBuffer
          .allocate(1 * 1024)
          .putInt(manifestBts.length)
          .put(manifestBts)

      tag match {
        case ETag.Created =>
          val e = ev.asInstanceOf[Event.Created]
          bb
            .putLong(e.id)
            .putLong(e.userId.value)
            .array()
        case ETag.Created2 =>
          val e = ev.asInstanceOf[Event.Created2]
          bb
            .putLong(e.id)
            .putLong(e.creator.value)
            .put(e.purpose.getBytes(StandardCharsets.UTF_8))
            .array()
        case ETag.Deleted =>
          val e = ev.asInstanceOf[Event.Deleted]
          bb
            .putLong(e.id)
            .putLong(e.userId.value)
            //
            .array()
        case ETag.Updated =>
          val e = ev.asInstanceOf[Event.Updated]
          bb
            .putLong(e.id)
            .putLong(e.userId.value)
            .array()
        case ETag.OwnershipCh =>
          val e = ev.asInstanceOf[Event.OwnershipChanged]
          bb
            .putLong(e.id)
            .putLong(e.userId.value)
            .array()
        case ETag.OwnershipCh2 =>
          val e = ev.asInstanceOf[Event.OwnershipChanged2]
          bb
            .putLong(e.id)
            .putLong(e.userId.value)
            .array()
        case ETag.OwnershipCh3 =>
          val e = ev.asInstanceOf[Event.OwnershipChanged3]
          bb
            .putLong(e.id)
            .putLong(e.userId.value)
            .array()
      }
    }

    def migrate[E](implicit M: Migration[T, E]): E =
      migrate0[T, E](event)

    private def migrate0[From, To](ev: From)(implicit M: Migration[From, To]): To =
      M match {
        case Migration.OwnershipChanged1to2 =>
          Event.OwnershipChanged2(
            ev.id,
            ev.firstName + " " + ev.lastName,
            ev.newOwner,
            ev.userId,
            ev.when
          )
        case Migration.OwnershipChanged2to3 =>
          Event.OwnershipChanged3(
            ev.id,
            ev.fullName,
            ev.newOwner,
            ev.userId,
            "my-label",
            ev.when
          )
        case Migration.OwnershipChanged1to3 =>
          Event.OwnershipChanged3(
            ev.id,
            ev.firstName + " " + ev.lastName,
            ev.newOwner,
            ev.userId,
            "my-label",
            ev.when
          )

        case Migration.Created1to2 =>
          Event.Created2(
            ev.id,
            ev.userId,
            "my-purpose",
            ev.when
          )
      }
  }

}
