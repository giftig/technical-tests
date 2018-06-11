package com.spaceape.hiring.model

case class Leaderboard(table: Seq[Leaderboard.Entry])

object Leaderboard {
  case class Entry(playerId: String, score: Int)
}
