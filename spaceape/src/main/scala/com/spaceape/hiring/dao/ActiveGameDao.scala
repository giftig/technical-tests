package com.spaceape.hiring.dao

import java.security.MessageDigest
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

import com.ibm.couchdb._

import com.spaceape.hiring.model.{Game, Player}
import com.spaceape.hiring.serialization.UpickleFormats._
import com.spaceape.hiring.utils.FileUtils

/**
 * Read and write to the active_games database
 *
 * TODO: Moving abstract definitions into a trait which is ignorant of CouchDao would make it
 *       easier to swap the db backend if desired
 */
class ActiveGameDao(override protected val conn: CouchDb) extends CouchDao {
  override protected val dbName: String = "active_games"
  override protected val typeMapping: TypeMapping = TypeMapping(classOf[Game] -> "game")
  override protected val designDoc: String = dbName

  override protected val views: Map[String, CouchView] = Map(
    ActiveGameDao.ByGameIdView -> CouchView(
      map = FileUtils.readResource("couchviews/active_games/by_game_id.js"),
      reduce = "_count"
    )
  )

  /**
   * Create the requested game
   *
   * @returns ID if the game was created, or None if it already existed
   */
  def createGame(player1: String, player2: String)(
    implicit ec: ExecutionContext
  ): Future[Option[String]] = {
    val g = Game(
      id = UUID.randomUUID.toString,
      player1 = player1,
      player2 = player2,
      currentTurn = Player.P1,
      board = Game.emptyBoard
    )

    val resp = insert(g, ActiveGameDao.deriveKey(g)) map { resp => Some(g.id) }
    resp recover {
      case CouchException(err: Res.Error) if err.error == "conflict" => None
    }
  }

  /**
   * Retrieve the specified game by looking up the unique ID in the index
   *
   * @returns The game and a String representing the document revision so that it can be updated
   *          if desired
   */
  def getGame(id: String)(implicit ec: ExecutionContext): Future[Option[(Game, String)]] = {
    val view = db.query.view[String, Any](designDoc, ActiveGameDao.ByGameIdView).get
    val res: Future[Seq[CouchDoc[Game]]] = Future(
      view.key(id).noReduce.includeDocs[Game].build.query.run.getDocs
    )
    res map { _.headOption map { doc => (doc.doc, doc._rev) } }
  }

  /**
   * Save the updated state of the game in the db
   *
   * @returns The updated document revision
   */
  def updateGame(g: Game, rev: String)(implicit ec: ExecutionContext): Future[String] = {
    val doc = CouchDoc(
      _id = ActiveGameDao.deriveKey(g),
      _rev = rev,
      kind = "game",
      doc = g
    )
    Future(db.docs.update(doc).run) map { _.rev } recover {
      case CouchException(err: Res.Error) if err.error == "conflict" => {
        throw new ActiveGameDao.UpdateConflictException(g.id)
      }
    }
  }
}

object ActiveGameDao {
  final val ByGameIdView = "by_game_id"

  private lazy val md = MessageDigest.getInstance("sha-1")

  /**
   * Convenience method to provide a hex sha1sum of a string
   */
  private def hash(s: String): String = md.digest(s.getBytes).map { c => f"${c}%02x" }.mkString

  /**
   * Derive a unique doc ID for couch based on the participants of this game
   *
   * We'll use this as the ID so that couch can ensure uniqueness as it doesn't support other
   * types of constraints. To ensure length or special characters aren't an issue, we'll shasum
   * the player names and combine them
   */
  def deriveKey(g: Game): String = hash(g.player1) + hash(g.player2)

  class UpdateConflictException(id: String) extends RuntimeException(
    s"Failed to update game $id due to a database update conflict"
  )
}
