package com.example.serialization2

@scala.annotation.implicitNotFound("The latest version is ${Out} but found ${In}")
sealed trait VersionConstraint[-In, +Out]

object VersionConstraint {
  implicit def nsub[A]: VersionConstraint[A, A] = new VersionConstraint[A, A] {}
}
