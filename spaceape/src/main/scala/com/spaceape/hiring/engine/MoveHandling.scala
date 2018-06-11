package com.spaceape.hiring.engine

import scala.util.{Either, Left, Right}

import com.spaceape.hiring.model._

/**
 * Provides an API to update a `Game` with `Move`s made by a player
 *
 * Input validation errors are treated as exceptional and will result in a thrown subclass of
 * InvalidArgumentException which should be transformed into a 400 by the API
 *
 * Errors related to moving out of turn or in an invalid place will be treated as a rejected move
 * and will instead result in a rejection type being passed back to be transformed into another
 * HTTP code, eg. 403.
 */
trait MoveHandling {
  import MoveHandling._

  /**
   * Simply check if the `turn` enum corresponds to the specified player
   *
   * @returns true if it's the player's turn, false otherwise
   * @throws InvalidPlayerException if that player isn't even a participant in the game
   */
  private def isPlayerTurn(g: Game, playerId: String): Boolean = {
    if (playerId != g.player1 && playerId != g.player2) {
      throw new InvalidPlayerException(playerId)
    }

    playerId == g.player1 && g.currentTurn == Player.P1 ||
    playerId == g.player2 && g.currentTurn == Player.P2
  }

  /**
   * Check if the given cell is occupied by a player's piece already
   *
   * @returns true if occupied, false otherwise
   * @throws InvalidPositionException if the cell is out of bounds of the board
   */
  private def isCellOccupied(g: Game, x: Int, y: Int): Boolean = try {
    g.board(x)(y).isDefined
  } catch {
    case _: ArrayIndexOutOfBoundsException => throw new InvalidPositionException(x, y)
  }

  /**
   * Just make the desired move; all validation must be done before this is called
   */
  private def makeMove(g: Game, m: Move): Game = {
    val moveTaker = if (m.playerId == g.player1) Player.P1 else Player.P2
    val nextTurn = if (g.currentTurn == Player.P1) Player.P2 else Player.P1

    val clonedBoard = g.board.clone
    clonedBoard(m.x)(m.y) = Some(moveTaker)

    g.copy(currentTurn = nextTurn, board = clonedBoard)
  }

  /**
   * Make the move and return an updated copy of the game board
   */
  def attemptMove(g: Game, m: Move): Either[Rejection, Game] = {
    (isPlayerTurn(g, m.playerId), isCellOccupied(g, m.x, m.y)) match {
      case (false, _) => Left(MovedOutOfTurnRejection)
      case (_, true) => Left(OccupiedCellRejection)
      case _ => Right(makeMove(g, m))
    }
  }
}

object MoveHandling {
  class InvalidPlayerException(player: String) extends IllegalArgumentException(
    s"The player ID '$player' is not a participant in this game"
  )

  class InvalidPositionException(x: Int, y: Int) extends IllegalArgumentException(
    s"The position [$x, $y] is not a valid set of coordinates on a noughts and crosses board"
  )

  /**
   * Reject moves made in the wrong place or by the wrong player
   */
  sealed trait Rejection
  case object MovedOutOfTurnRejection extends Rejection
  case object OccupiedCellRejection extends Rejection
}
