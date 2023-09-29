package com.example.serialization2

trait BidiPatch[From, To] {
  def forward: From => To
  def backward: To => From
}

object BidiPatch {

  final case class Both[A, B, C](a: BidiPatch[A, B], b: BidiPatch[B, C]) extends BidiPatch[A, C] {
    def forward: A => C =
      (in: A) => b.forward(a.forward(in))

    def backward: C => A =
      (in: C) => a.backward(b.backward(in))
  }

  implicit class BidiPatchOps[A, B](val self: BidiPatch[A, B]) extends AnyVal {
    def ~>[C](next: BidiPatch[B, C]): BidiPatch[A, C] = Both(self, next)
  }
}
