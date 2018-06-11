package com.spaceape.hiring

import javax.ws.rs._
import javax.ws.rs.core.{MediaType, Response}
import javax.ws.rs.core.Response.Status
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.{Either, Left, Right}

import com.typesafe.scalalogging.StrictLogging

import com.spaceape.hiring.dao.{ActiveGameDao, CompletedGameDao, GameCompletion}
import com.spaceape.hiring.engine.{GameEngine, GameHandling, MoveHandling}
import com.spaceape.hiring.model._

@Path("/game")
@Produces(Array(MediaType.APPLICATION_JSON))
@Consumes(Array(MediaType.APPLICATION_JSON))
class NoughtsResource(
  activeGameDao: ActiveGameDao,
  completedGameDao: CompletedGameDao,
  completionHandler: GameCompletion,
  engine: GameHandling,
  awaitTimeout: Duration = Duration.Inf
)(implicit ec: ExecutionContext) extends StrictLogging {
  /**
   * Convenience method to await futures
   */
  private def block[T](f: Future[T]): T = {
    Await.result(f, awaitTimeout)
  }

  @POST
  def createGame(
    @QueryParam("player1Id") player1: String,
    @QueryParam("player2Id") player2: String
  ): NewGameResponse = {
    (Option(player1), Option(player2)) match {
      case (Some(p1), Some(p2)) if p1 == p2 =>
        throw new WebApplicationException("Cannot create a one-player game", Status.BAD_REQUEST)
      case (Some(p1), Some(p2)) =>
        logger.info(s"Attempting to create new game between players $player1 and $player2...")
        val result = activeGameDao.createGame(p1, p2) map {
          case Some(id) => NewGameResponse(id)
          case None => throw new WebApplicationException(
            s"A game is already ongoing between $player1 and $player2",
            Status.CONFLICT
          )
        }
        block(result)
      case _ =>
        throw new WebApplicationException("Missing one or both player IDs", Status.BAD_REQUEST)
    }
  }

  @GET
  @Path("/{gameId}")
  def getGame(@PathParam("gameId") gameId: String): GameState = {
    val completedGame: Future[Option[GameState]] = completedGameDao.getGame(gameId) map {
      _ map {
        cg: CompletedGame => GameState(winnerId = cg.winner, gameOver = true)
      }
    }
    lazy val activeGame: Future[Option[GameState]] = activeGameDao.getGame(gameId) map {
      _ map {
        case (g: Game, _) => GameState(winnerId = None, gameOver = false)
      }
    }

    val gameState = completedGame flatMap {
      case Some(state) => Future.successful(state)
      case None => activeGame map {
        _ getOrElse { throw new WebApplicationException("No such game", Status.NOT_FOUND) }
      }
    }

    block(gameState)
  }


  @PUT
  @Path("/{gameId}")
  def makeMove(@PathParam("gameId") gameId: String, move: Move): Response = {
    logger.info(s"$gameId: ${move.playerId} is placing [${move.x}, ${move.y}]")

    val game: Future[(Game, String)] = activeGameDao.getGame(gameId) map {
      _.getOrElse(throw new WebApplicationException("No such game", Status.NOT_FOUND))
    }
    val updatedGame: Future[(Either[MoveHandling.Rejection, Game], String)] = game map {
      case (g, rev) => engine.attemptMove(g, move) -> rev
    }

    // Firstly handle rejections due to the user doing something fishy
    val rejectionsHandled: Future[(Game, String)] = updatedGame map {
      case (Left(MoveHandling.MovedOutOfTurnRejection), _) => throw new WebApplicationException(
        "It's not your turn",
        Status.FORBIDDEN
      )
      case (Left(MoveHandling.OccupiedCellRejection), _) => throw new WebApplicationException(
        "There's already a token at the specified location",
        Status.FORBIDDEN
      )
      case (Right(game), rev) => game -> rev
    } recover {
      case e: IllegalArgumentException => throw new WebApplicationException(
        e.getMessage,
        Status.BAD_REQUEST
      )
    }

    // Now we try to save the updated game to the db and handle any races
    val savedGame: Future[(Game, String)] = rejectionsHandled flatMap { case (g, rev) =>
      activeGameDao.updateGame(g, rev) map {
        case newRev => (g, newRev)
      } recover {
        case e: ActiveGameDao.UpdateConflictException => {
          throw new WebApplicationException(e.getMessage, Status.CONFLICT)
        }
      }
    }

    // Check if the game is over and move it to the completed db if that's the case
    val handledCompletion: Future[Unit] = savedGame flatMap { case (g, rev) =>
      val state = engine.calculateGameState(g)
      if (state.gameOver) {
        val completed = CompletedGame(g.player1, g.player2, state.winnerId)

        completionHandler.completeGame(completed, (g, rev))
      } else {
        Future.successful(())
      }
    }

    // If everything succeeded, just respond Accepted (since it's sync it should be 200, though)
    val finalResponse = handledCompletion map { _ => Response.accepted.build }
    block(finalResponse)
  }

  @GET
  @Path("/leaderboard")
  def getLeaderboard(
    @QueryParam("lastName") lastName: String,
    @QueryParam("limit") limit: Int
  ): Leaderboard = {
    // To keep me on my toes, the limit will be 0 if not specified as a java int can't be null
    val limitOpt: Option[Int] = if (limit == 0) None else Some(limit)

    block(completedGameDao.getLeaderboard(Option(lastName), limitOpt))
  }
}
