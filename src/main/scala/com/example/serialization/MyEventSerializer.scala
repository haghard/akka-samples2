package com.example.serialization

import akka.actor.ExtendedActorSystem
import com.example.serialization.Event.{ByteOps, EventOps}
import akka.serialization.SerializerWithStringManifest

import java.io.NotSerializableException

/** Each FQCN is unique and the Scala compiler can check its uniqueness during compile time. The question is how can we
  * bring that compile-time guarantee to the Scala type level.
  */
object MyEventSerializer {

  def notSerializable(msg: String) = throw new NotSerializableException(msg)

  def migrate[E](event: E)(f: PartialFunction[E, E]): E =
    f.lift(event).getOrElse(event)
}

final class MyEventSerializer(
  system: ExtendedActorSystem
) extends SerializerWithStringManifest { self =>
  import MyEventSerializer._

  override val identifier = 999

  override def manifest(event: AnyRef): String =
    event match {
      case ev: Event =>
        ev.tag().manifest()
      case _ =>
        notSerializable(s"Unexpected $event in ${self.getClass().getSimpleName()}")
    }

  override def toBinary(ev: AnyRef): Array[Byte] =
    ev match {
      case ev: Event =>
        ev.serialize(ev.tag())
      case _ =>
        notSerializable(s"Unexpected $ev in ${self.getClass().getSimpleName()}")
    }

  override def fromBinary(evBytes: Array[Byte], manifest: String): AnyRef =
    ETag
      .fromStr(manifest)
      // .map { implicit tag => evBytes.deserialize(tag) }
      .map { tag =>
        val evFromJournal: Event = evBytes.deserialize(tag)
        /*
          Promote events to the latest versions during recovery
          such that the business logic doesn't have to consider multiple versions of events.
          It can work with the latest version only.
         */
        migrate(evFromJournal) {
          case e: Event.Created =>
            e.migrate[Event.Created2]

          // fast forward (V1 ~> V3)
          case e: Event.OwnershipChanged =>
            e.migrate[Event.OwnershipChanged3]

          /*
          //OR
          case e: Event.OwnershipChanged =>
            //V1 ~> V2
            e.migrate[Event.OwnershipChanged2]
          case e: Event.OwnershipChanged2 =>
            //V2 ~> V3
            e.migrate[Event.OwnershipChanged3]
           */
        }
      }
      .getOrElse(notSerializable(s"Unexpected $manifest in ${self.getClass().getSimpleName()}"))
}
