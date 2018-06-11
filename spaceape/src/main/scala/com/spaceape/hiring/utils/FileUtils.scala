package com.spaceape.hiring.utils

import scala.io.Source

object FileUtils {
  /**
   * Read the complete content of a resource file as a String
   */
  def readResource(filename: String): String = {
    Source.fromInputStream(getClass.getResourceAsStream("/" + filename)).mkString
  }
}
