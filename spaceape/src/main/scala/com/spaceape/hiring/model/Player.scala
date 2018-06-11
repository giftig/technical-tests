package com.spaceape.hiring.model

/**
 * Represents a player
 *
 * Useful as a concrete representation given the game has a constant number of players. This
 * can then be used to represent both the player whose turn is currently active, and the
 */
object Player extends Enumeration {
  val P1 = Value
  val P2 = Value
}
