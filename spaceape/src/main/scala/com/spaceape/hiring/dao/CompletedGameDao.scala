package com.spaceape.hiring.dao

import scala.concurrent.{ExecutionContext, Future}

import com.ibm.couchdb._

import com.spaceape.hiring.model.{CompletedGame, Leaderboard}
import com.spaceape.hiring.utils.FileUtils

/**
 * Read/write to the completed_games database
 *
 * This stores a simple record of past games and who won them, which is useful both to serve the
 * API to look up the winner of a particular game, and to provide a view acting as a leaderboard
 */
class CompletedGameDao(override protected val conn: CouchDb) extends CouchDao {
  override protected val dbName: String = "completed_games"
  override protected val typeMapping: TypeMapping = TypeMapping(
    classOf[CompletedGame] -> "completed_game"
  )
  override protected val designDoc = dbName

  override protected val views: Map[String, CouchView] = Map(
    CompletedGameDao.WinsByPlayerView -> CouchView(
      map = FileUtils.readResource("couchviews/completed_games/wins_by_player.js"),
      reduce = "_count"
    )
  )

  /**
   * Get a completed game by ID
   */
  def getGame(gameId: String)(implicit ec: ExecutionContext): Future[Option[CompletedGame]] = {
    Future(db.docs.get[CompletedGame](gameId).run.doc) map { Some(_) } recover {
      case CouchException(err: Res.Error) if err.error == "not_found" => None
    }
  }

  /**
   * Get the leaderboard in a paginated way
   *
   * @param afterName Optionally, the last player in the previous page of results, to continue
   *                  paginating from there.
   * @param limit Max number of results to return
   */
  def getLeaderboard(afterName: Option[String], limit: Option[Int])(
    implicit ec: ExecutionContext
  ): Future[Leaderboard] = {
    val finalLimit = limit getOrElse 10  // default to a reasonable number

    val view = db.query.view[String, Int](designDoc, CompletedGameDao.WinsByPlayerView).get
    val query = {
      val baseQuery = view.reduce[Int].group(true).descending(true)

      val withStartKey = afterName map {
        baseQuery.startKey(_).skip(1)
      } getOrElse baseQuery

      val withLimit = withStartKey.limit(finalLimit)

      withLimit.build.query
    }

    Future(query.run) map {
      result => Leaderboard(
        result.rows map { row => Leaderboard.Entry(row.key, row.value) }
      )
    }
  }
}

object CompletedGameDao {
  final val WinsByPlayerView = "wins_by_player"
}
