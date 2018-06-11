package com.spaceape.hiring

import java.util.UUID
import javax.ws.rs.core.Response.Status

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.mashape.unirest.http.{HttpResponse, Unirest}
import io.dropwizard.testing.junit.DropwizardAppRule
import org.junit.ClassRule
import org.junit.Test
import org.scalatest.Matchers
import org.scalatest.junit.JUnitSuite
import org.scalatest.matchers.{Matcher, MatchResult}

import com.spaceape.hiring.model.{GameState, NewGameResponse, Move}


object NoughtsTest {
	@ClassRule def rule = new DropwizardAppRule[NoughtsConfiguration](classOf[NoughtsApplication], "test.yml")
}

class NoughtsTest extends JUnitSuite with CustomMatchers {

  val baseUrl = "http://localhost:8080/game"

  val objectMapper = new ObjectMapper()
  objectMapper.registerModule(DefaultScalaModule)

  private def initGameResponse(player1Id: String, player2Id: String): HttpResponse[String] = {
    Unirest.post(baseUrl)
      .queryString("player1Id", player1Id)
      .queryString("player2Id", player2Id)
      .asString()
  }

  private def initGame(player1Id: String, player2Id: String) = {
    val response = initGameResponse(player1Id, player2Id)
    response should haveStatus(Status.OK)
    objectMapper.readValue(response.getBody, classOf[NewGameResponse])
  }

  private def runMoves(gameId: String, moves: Seq[Move]) = {
    moves.foreach(move => {
      val response = Unirest.put(s"$baseUrl/$gameId")
        .header("Content-Type", "application/json")
        .body(objectMapper.writeValueAsString(move))
        .asString()

      response should haveStatus(Status.ACCEPTED)
    })
  }

  private def getState(gameId: String) = {
    val response = Unirest.get(s"$baseUrl/$gameId").asString()

    response should haveStatus(Status.OK)
    objectMapper.readValue(response.getBody, classOf[GameState])
  }

  /**
   * Finish a fresh game by making player 1 win
   *
   * This is the basis for the `testPlayer1Win` test, and also used as an accessory to end a game,
   * allowing testing features which happen when a game is over in the absence of an API to do that
   * in a more direct way.
   *
   * Note that this means if this process breaks it'll result in several tests failing
   */
  private def finishGame(id: String, p1: String, p2: String): Unit = {
    runMoves(id, Seq(
      Move(p1, 0, 0),
      Move(p2, 1, 0),
      Move(p1, 0, 1),
      Move(p2, 1, 1),
      Move(p1, 0, 2)
    ))
    getState(id) should be (GameState(Some(p1), true))
  }

	@Test
	def testPlayer1Win(): Unit = {
    val gameId = initGame("1", "2").gameId
    finishGame(gameId, "1", "2")
	}

  @Test
  def testNoDuplicateGame(): Unit = {
    // To prevent conflicts with repeated test runs (though ideally we should clean the test db)
    val p1 = UUID.randomUUID.toString
    val p2 = UUID.randomUUID.toString

    initGame(p1, p2)
    initGameResponse(p1, p2) should haveStatus(Status.CONFLICT)
  }

  @Test
  def testDuplicateGamePermittedAfterCompletion(): Unit = {
    // To prevent conflicts with repeated test runs (though ideally we should clean the test db)
    val p1 = UUID.randomUUID.toString
    val p2 = UUID.randomUUID.toString

    val gameId = initGame(p1, p2).gameId
    finishGame(gameId, p1, p2)
    initGame(p1, p2)
  }

  @Test
  def testDisallowBothPlayersWithSameId(): Unit = {
    val player = UUID.randomUUID.toString
    initGameResponse(player, player) should haveStatus(Status.BAD_REQUEST)
  }
}

trait CustomMatchers extends Matchers {
  class StatusMatcher(expected: Status) extends Matcher[HttpResponse[_]] {
    def apply(resp: HttpResponse[_]) = {
      val actual = resp.getStatus

      MatchResult(
        actual == expected.getStatusCode,
        s"""HTTP status $actual was not $expected""",
        s"""HTTP status was $expected, as expected"""
      )
    }
  }

  protected def haveStatus(expected: Status): StatusMatcher = new StatusMatcher(expected)
}
