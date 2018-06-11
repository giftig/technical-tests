package com.spaceape.hiring

import scala.concurrent.duration._

import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Configuration

class NoughtsConfiguration extends Configuration {
  import NoughtsConfiguration._

  @JsonProperty
  val database: DatabaseConfig = DatabaseConfig()

  @JsonProperty("endpointTimeout")
  val endpointTimeoutSeconds: Int = 0

  val endpointTimeout: Duration = if (endpointTimeoutSeconds == 0) {
    Duration.Inf
  } else {
    endpointTimeoutSeconds.seconds
  }
}

object NoughtsConfiguration {
  /**
   * Contains config related to connecting to the db
   */
  case class DatabaseConfig(
    @JsonProperty("hostname") hostname: String = "localhost",
    @JsonProperty("port") port: Int = 5984
  )
}
