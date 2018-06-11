package com.spaceape.hiring.model

/**
 * Represents a completed game in the `completed_games` db
 *
 * Keeps a record of who participated and who won the game
 */
case class CompletedGame(
  player1: String,
  player2: String,
  winner: Option[String]
)
