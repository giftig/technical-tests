package com.spaceape.hiring.health

import scala.concurrent.{Await, ExecutionContext, Future}

import com.codahale.metrics.health.HealthCheck
import com.ibm.couchdb.CouchDb

/**
 * Make sure couchdb is reachable
 */
class DatabaseHealthCheck(conn: CouchDb) extends HealthCheck {
  override protected def check(): HealthCheck.Result = {
    conn.server.mkUuid.run
    HealthCheck.Result.healthy()
  }
}
