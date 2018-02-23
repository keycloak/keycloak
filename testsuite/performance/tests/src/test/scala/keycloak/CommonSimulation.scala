package keycloak

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import keycloak.CommonScenarioBuilder._
import org.keycloak.performance.log.LogProcessor
import io.gatling.core.validation.Validation

import org.keycloak.performance.TestConfig


/**
  * @author tomas Kyjovsky &lt;tkyjovsk@redhat.com&gt;
  */
abstract class CommonSimulation extends Simulation {

  println()
  println("Target servers: " + TestConfig.serverUrisList)
  println()
  println("Using test parameters:\n" + TestConfig.toStringCommonTestParameters);
  printSpecificTestParameters
  println()
  println("Using dataset properties:\n" + TestConfig.toStringDatasetProperties)
  println()
  println("Timestamps: \n" + TestConfig.toStringTimestamps)
  println()

  def printSpecificTestParameters {
    // override in subclass
  }
  
  def rampDownNotStarted(): Validation[Boolean] = {
    System.currentTimeMillis < TestConfig.measurementEndTime
  }

  after {
    if (TestConfig.filterResults) {
      new LogProcessor(getClass).filterLog(
        TestConfig.measurementStartTime, 
        TestConfig.measurementEndTime,
        false, false)
    }
  }

}
