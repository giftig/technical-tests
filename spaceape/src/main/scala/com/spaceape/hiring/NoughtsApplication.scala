package com.spaceape.hiring

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global  // TODO: configurable pool would be nice
import scala.concurrent.duration._

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.typesafe.scalalogging.StrictLogging
import com.ibm.couchdb.CouchDb
import io.dropwizard.Application
import io.dropwizard.jersey.jackson.JacksonMessageBodyProvider
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment

import com.spaceape.hiring.dao._
import com.spaceape.hiring.engine.GameEngine
import com.spaceape.hiring.health.DatabaseHealthCheck

object NoughtsApplication {
  def main(args: Array[String]) {
    new NoughtsApplication().run(args: _*)
  }
}

class NoughtsApplication extends Application[NoughtsConfiguration] with StrictLogging {
  override def getName() = "noughts"

  override def initialize(bootstrap: Bootstrap[NoughtsConfiguration]) {
  }

  /**
   * Ensure the DAOs are ready to operate
   */
  private def bootstrapDb(dbs: Seq[CouchDao]): Unit = {
    logger.info(s"Bootstrapping ${dbs.length} databases...")

    // Bootstrap DAOs in parallel but then block until they're all finished
    val bootstrapResults = Future.traverse(dbs) {
      _.bootstrap()
    }
    Await.result(bootstrapResults, 30.seconds)
  }

  override def run(config: NoughtsConfiguration, environment: Environment) {
    val db = CouchDb(config.database.hostname, config.database.port)
    val activeDao = new ActiveGameDao(db)
    val completedDao = new CompletedGameDao(db)

    bootstrapDb(Seq(activeDao, completedDao))

    val resource = new NoughtsResource(
      activeDao,
      completedDao,
      new GameCompleter(activeDao, completedDao),
      new GameEngine,
      config.endpointTimeout
    )

    val objectMapper = new ObjectMapper()
    objectMapper.registerModule(DefaultScalaModule)

    environment.healthChecks.register("database", new DatabaseHealthCheck(db))
    environment.jersey().register(new JacksonMessageBodyProvider(objectMapper))
    environment.jersey().register(resource)
  }

}
