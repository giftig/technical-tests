package com.spaceape.hiring.engine

import org.junit.Test
import org.scalatest.Matchers
import org.scalatest.junit.JUnitSuite

import com.spaceape.hiring.model._

class GameStateCalculationTest extends JUnitSuite with Matchers {
  private val p1Name = "sandor_clegane"
  private val p2Name = "gregor_clegane"

  private val p1 = Some(Player.P1)
  private val p2 = Some(Player.P2)

  private val p1Win = GameState(winnerId = Some(p1Name), gameOver = true)
  private val p2Win = GameState(winnerId = Some(p2Name), gameOver = true)
  private val draw = GameState(winnerId = None, gameOver = true)
  private val notOver = GameState(winnerId = None, gameOver = false)

  private val calc = new GameStateCalculation {}

  private def createGame(b: Array[Array[Option[Player.Value]]]): Game = Game(
    id = "123",
    player1 = p1Name,
    player2 = p2Name,
    currentTurn = Player.P1,
    board = b
  )

  @Test
  def testHorizontalWinP1(): Unit = {
    val g = createGame(Array(
      Array(None, None, None),
      Array(p1, p1, p1),
      Array(None, None, None)
    ))
    calc.calculateGameState(g) shouldBe p1Win
  }

  @Test
  def testHorizontalWinP2(): Unit = {
    val g = createGame(Array(
      Array(None, None, None),
      Array(p2, p2, p2),
      Array(None, None, None)
    ))
    calc.calculateGameState(g) shouldBe p2Win
  }

  @Test
  def testVerticalWinP1(): Unit = {
    val g = createGame(Array(
      Array(None, p1, None),
      Array(None, p1, None),
      Array(None, p1, None)
    ))
    calc.calculateGameState(g) shouldBe p1Win
  }

  @Test
  def testVerticalWinP2(): Unit = {
    val g = createGame(Array(
      Array(None, None, None),
      Array(p2, p2, p2),
      Array(None, None, None)
    ))
    calc.calculateGameState(g) shouldBe p2Win
  }

  @Test
  def testDiagonalWinP1(): Unit = {
    val g = createGame(Array(
      Array(p1, None, None),
      Array(None, p1, None),
      Array(None, None, p1)
    ))
    calc.calculateGameState(g) shouldBe p1Win
  }

  @Test
  def testDiagonalWinP2(): Unit = {
    val g = createGame(Array(
      Array(None, None, p2),
      Array(None, p2, None),
      Array(p2, None, None)
    ))
    calc.calculateGameState(g) shouldBe p2Win
  }

  @Test
  def testDraw(): Unit = {
    val g = createGame(Array(
      Array(p1, p2, p2),
      Array(p2, p1, p1),
      Array(p2, p1, p2)
    ))
    calc.calculateGameState(g) shouldBe draw
  }

  @Test
  def testSpaceRemaining(): Unit = {
    val g = createGame(Array(
      Array(p1, p2, None),
      Array(p2, p1, p1),
      Array(p2, p1, p2)
    ))
    calc.calculateGameState(g) shouldBe notOver
  }
}
