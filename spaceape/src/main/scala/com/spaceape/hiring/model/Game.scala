package com.spaceape.hiring.model

/**
 * Represents the internal state of a game
 */
case class Game(
  id: String,
  player1: String,
  player2: String,
  currentTurn: Player.Value,
  board: Array[Array[Option[Player.Value]]]
)

object Game {
  /**
   * Convenience value representing an empty board
   */
  def emptyBoard: Array[Array[Option[Player.Value]]] = {
    Array.fill(3) {
      Array.fill(3)(None)
    }
  }
}
