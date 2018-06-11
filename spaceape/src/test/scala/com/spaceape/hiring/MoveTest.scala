package com.spaceape.hiring.engine

import org.junit.Test
import org.scalatest.Matchers
import org.scalatest.junit.JUnitSuite

import com.spaceape.hiring.model._

class MoveTest extends JUnitSuite with Matchers {
  import MoveHandling._

  private val p1Name = "kamala"
  private val p2Name = "colivar"

  private val p1 = Some(Player.P1)
  private val p2 = Some(Player.P2)

  def createGame(): Game = Game(
    id = "321",
    player1 = p1Name,
    player2 = p2Name,
    currentTurn = Player.P1,
    board = Array(
      Array(None, None, p1),
      Array(p1, p2, p2),
      Array(None, None, None)
    )
  )

  val handler = new MoveHandling {}

  @Test
  def testValidMove(): Unit = {
    val g = createGame()
    val m1 = Move(playerId = g.player1, x = 0, y = 0)
    val m2 = Move(playerId = g.player2, x = 2, y = 0)

    val update1 = handler.attemptMove(g, m1).right.get
    update1.currentTurn shouldBe Player.P2
    update1.board(m1.x)(m1.y) shouldBe p1

    val update2 = handler.attemptMove(update1, m2).right.get
    update2.currentTurn shouldBe Player.P1
    update2.board(m2.x)(m2.y) shouldBe p2
  }

  @Test
  def testCollision(): Unit = {
    val g = createGame()
    val moves = Seq(
      Move(playerId = g.player1, x = 0, y = 2),
      Move(playerId = g.player1, x = 1, y = 1)
    )

    moves foreach { m =>
      handler.attemptMove(g, m).left.get shouldBe(OccupiedCellRejection)
    }
  }

  @Test
  def testWrongTurn(): Unit = {
    val g = createGame()
    val moves = Seq(
      Move(playerId = g.player2, x = 0, y = 0),
      Move(playerId = g.player2, x = 2, y = 2)
    )

    moves foreach { m =>
      handler.attemptMove(g, m).left.get shouldBe(MovedOutOfTurnRejection)
    }
  }

  @Test
  def testOutOfBounds(): Unit = {
    val g = createGame()
    val moves = Seq(
      Move(playerId = g.player1, x = -1, y = -1),
      Move(playerId = g.player1, x = -1, y = 0),
      Move(playerId = g.player1, x = 0, y = -1),
      Move(playerId = g.player1, x = 3, y = 3),
      Move(playerId = g.player1, x = 3, y = 0),
      Move(playerId = g.player1, x = 0, y = 3)
    )

    moves foreach { m =>
      an[InvalidPositionException] should be thrownBy handler.attemptMove(g, m)
    }
  }

  @Test
  def testInvalidPlayer(): Unit = {
    val g = createGame()
    val m = Move(playerId = s"$p1Name-$p2Name", x = 0, y = 0)

    an[InvalidPlayerException] should be thrownBy handler.attemptMove(g, m)
  }

}
