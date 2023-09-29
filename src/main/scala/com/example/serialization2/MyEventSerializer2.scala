package com.example.serialization2

import akka.actor.ExtendedActorSystem
import akka.serialization.SerializerWithStringManifest
import com.example.serialization.Event.{ByteOps, EventOps}
import com.example.serialization.{ETag, Event}

import java.io.NotSerializableException

object MyEventSerializer2 {

  def notSerializable(msg: String) = throw new NotSerializableException(msg)

  def migrate[E](event: E)(f: PartialFunction[E, E]): E =
    f.lift(event).getOrElse(event)
}

final class MyEventSerializer2(
  system: ExtendedActorSystem
) extends SerializerWithStringManifest
    with BidiPatches { self =>
  import MyEventSerializer2.*

  override val identifier = 999

  override def manifest(event: AnyRef): String =
    event match {
      case ev: Event =>
        ev.tag().manifest()
      case _ =>
        notSerializable(s"Unknown $event in ${self.getClass().getSimpleName()}")
    }

  override def toBinary(ev: AnyRef): Array[Byte] =
    ev match {
      case ev: Event =>
        ev.serialize(ev.tag())
      case _ =>
        notSerializable(s"Unknown $ev in ${self.getClass().getSimpleName()}")
    }

  override def fromBinary(evBytes: Array[Byte], manifest: String): AnyRef =
    ETag
      .fromStr(manifest)
      .map { tag =>
        val evFromJournal: Event = evBytes.deserialize(tag)
        migrate(evFromJournal) {
          case e: Event.Created =>
            applyPatch(e, Event.Created._2)

          case e: Event.OwnershipChanged =>
            import Event.OwnershipChanged.*
            // applyPatch(e, _2)
            // applyPatch(e, _2 ~> _3)
            applyPatch(e, _ff3)

        }
      }
      .getOrElse(notSerializable(s"Unknown $manifest. Check ${self.getClass().getSimpleName()}"))
}
