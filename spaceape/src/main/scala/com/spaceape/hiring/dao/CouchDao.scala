package com.spaceape.hiring.dao

import scala.concurrent.{ExecutionContext, Future}

import com.ibm.couchdb._
import com.typesafe.scalalogging.StrictLogging
import upickle.default.Writer

import com.spaceape.hiring.serialization.UpickleFormats._

/**
 * Wrapper for the couchdb library to allow daos to access couch
 *
 * Everything is handled asynchronously via futures to allow control over when to block
 */
trait CouchDao extends StrictLogging {
  protected val conn: CouchDb
  protected val dbName: String
  protected val designDoc: String
  protected val views: Map[String, CouchView] = Map.empty
  protected val typeMapping: TypeMapping = TypeMapping.empty

  protected lazy val db = conn.db(dbName, typeMapping)

  /**
   * Ensure the database has been created in couchdb
   */
  private def ensureDatabaseExists()(implicit ec: ExecutionContext): Future[Unit] = {
    val result = Future(conn.dbs.create(dbName).run) map { _ => () }

    result recover {
      case CouchException(err: Res.Error) if err.error == "file_exists" => ()
    }
  }

  /**
   * Ensure the design document for this dao has been inserted into couchdb
   */
  private def ensureDesignDocExists()(implicit ec: ExecutionContext): Future[Unit] = {
    val design: Option[CouchDesign] = if (views.isEmpty) {
      None
    } else {
      Some(CouchDesign(name = designDoc, views = views))
    }

    val res = design map {
      d => Future(db.design.create(d).run) map { _ => () }
    } getOrElse Future.successful(())

    res recover {
      case CouchException(err: Res.Error) if err.error == "conflict" => ()
    }
  }

  /**
   * Ensure that any databases and indexes required by the application are present
   */
  def bootstrap()(implicit ec: ExecutionContext): Future[Unit] = {
    ensureDatabaseExists() flatMap { _ => ensureDesignDocExists() }
  }

  /**
   * Insert a document into the database, erroring if it already exists
   */
  def insert[T : Writer](doc: T)(implicit ec: ExecutionContext): Future[Res.DocOk] = {
    Future(db.docs.create(doc).run)
  }

  /**
   * Insert a document into the database, erroring if it already exists
   */
  def insert[T : Writer](doc: T, docId: String)(
    implicit ec: ExecutionContext
  ): Future[Res.DocOk] = Future(db.docs.create(doc, docId).run)

  /**
   * Delete a document
   *
   * @param docId The ID of the coument
   * @param rev The revision we're trying to delete; if it's not the latest revision we'll error
   */
  def delete(docId: String, rev: String)(implicit ec: ExecutionContext): Future[Unit] = {
    // Workaround for https://github.com/beloglazov/couchdb-scala/issues/77
    val doc = CouchDoc[String](_id = docId, _rev = rev, kind = "", doc = "")
    val res = Future(db.docs.delete(doc).run) map { _ => () }

    // It's fine if the document has already been deleted, though we'll warn about it
    res recover {
      case CouchException(err: Res.Error) if err.error == "missing" => {
        logger.warn(s"Tried to delete missing document $docId:$rev")
      }
    }
  }
}
