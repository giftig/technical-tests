package com.spaceape.hiring.engine

import com.spaceape.hiring.model._

/**
 * Provides an API to determine a game's state
 */
trait GameStateCalculation {
  /**
   * Lazily generate segments which may contain valid winning lines from this board
   *
   * With a static 3x3 board this is fairly straightforward but see my commentary in the readme
   * for approaches with a larger board
   */
  private def dissectBoard(
    board: Array[Array[Option[Player.Value]]]
  ): Stream[Array[Option[Player.Value]]] = {
    // This could be generalised but with such a small board size it's easier just to write it out
    board(0) #:: board(1) #:: board(2) #::  // horizontal lines
    Array(board(0)(0), board(1)(0), board(2)(0)) #::
    Array(board(0)(1), board(1)(1), board(2)(1)) #::
    Array(board(0)(2), board(1)(2), board(2)(2)) #::  // vertical lines
    Array(board(0)(0), board(1)(1), board(2)(2)) #::
    Array(board(0)(2), board(1)(1), board(2)(0)) #:: // horizontal lines
    Stream.empty
  }

  /**
   * Return the player who has three tokens in this line
   *
   * @param line Must be a three-element array
   */
  private def findWinInLine(line: Array[Option[Player.Value]]): Option[Player.Value] = {
    // Reduce the line to None unless it finds all elements match and are not None
    // This only does what we want because the entire line must be taken to "win" the line
    line reduce { (a, b) => (a, b) match {
      case (None, _) | (_, None) => None
      case (Some(a: Player.Value), Some(b: Player.Value)) if a == b => Some(a)
      case _ => None
    }}
  }

  /**
   * Figure out the state of the game from the internal game data
   *
   * Simply means checking if someone's won, and if it's a draw
   */
  def calculateGameState(g: Game): GameState = {
    val lines = dissectBoard(g.board)
    val winner: Option[Player.Value] = {
      lines.map { l => findWinInLine(l) }.find { _.isDefined }.flatten
    }
    val winnerId = winner map {
      case Player.P1 => g.player1
      case _ => g.player2
    }

    lazy val boardHasSpace = g.board.flatten exists { _.isEmpty }

    GameState(winnerId = winnerId, gameOver = winnerId.isDefined || !boardHasSpace)
  }
}
