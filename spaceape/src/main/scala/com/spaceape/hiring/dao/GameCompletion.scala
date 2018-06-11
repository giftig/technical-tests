package com.spaceape.hiring.dao

import scala.concurrent.{ExecutionContext, Future}

import com.typesafe.scalalogging.StrictLogging

import com.spaceape.hiring.model._

/**
 * Completes finished games by writing an entry to CompletedGameDao and removing from ActiveGameDao
 */
trait GameCompletion extends StrictLogging {
  protected val activeDao: ActiveGameDao
  protected val completedDao: CompletedGameDao

  /**
   * Write the provided game's completed state to the db and clean up the `active` entry on success
   *
   * @param completed An entry representing the completed game info
   * @param active The game and its database revision (so that it can be deleted)
   */
  def completeGame(
    completed: CompletedGame,
    active: (Game, String)
  )(implicit ec: ExecutionContext): Future[Unit] = {
    val (game, rev) = active
    logger.info(s"Completing game ${game.id}")

    completedDao.insert(completed, game.id) flatMap {
      _ => activeDao.delete(ActiveGameDao.deriveKey(game), rev)
    }
  }
}
