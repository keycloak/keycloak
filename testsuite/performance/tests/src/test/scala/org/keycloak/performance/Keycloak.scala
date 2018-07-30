package org.keycloak.performance

import java.time.format.DateTimeFormatter
import org.keycloak.performance.templates.DatasetTemplate

trait Keycloak {
  
  val datasetTemplate = new DatasetTemplate()
  datasetTemplate.validateConfiguration
  val dataset = datasetTemplate.produce
  
  val DATE_FMT_RFC1123 = DateTimeFormatter.RFC_1123_DATE_TIME
  
  val ACCEPT_ALL = Map("Accept" -> "*/*")
  val AUTHORIZATION = Map("Authorization" -> "Bearer ${accessToken}")
  
}