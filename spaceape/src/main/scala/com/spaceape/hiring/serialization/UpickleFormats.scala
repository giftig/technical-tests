package com.spaceape.hiring.serialization

import upickle._
import upickle.default.{Reader, Writer}

import com.spaceape.hiring.model.Player

object UpickleFormats {
  implicit val playerWriter: Writer[Player.Value] = Writer[Player.Value] {
    case p: Player.Value => Js.Str(p.toString)
  }
  implicit val playerReader: Reader[Player.Value] = Reader[Player.Value] {
    case Js.Str(s) => Player.withName(s)
  }
}
