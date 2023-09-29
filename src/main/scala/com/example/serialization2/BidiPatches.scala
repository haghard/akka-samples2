package com.example.serialization2

trait BidiPatches {

  def applyPatch[From, To](event: From, patch: BidiPatch[From, To])
  // (implicit ev: To VersionConstraint com.example.serialization.Event.OwnershipChanged3)
    : To =
    patch match {
      case b: BidiPatch.Both[_, _, _] =>
        b.forward(event)
      case p: BidiPatch[_, _] =>
        p.forward(event)
    }
}
