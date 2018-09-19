package keycloak

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import org.keycloak.gatling.log.LogProcessor
import io.gatling.core.validation.Validation

import io.gatling.core.controller.inject.InjectionStep

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
//  println("Using dataset properties:\n" + TestConfig.toStringDatasetProperties)
//  println()
  println("Using assertion properties:\n" + TestConfig.toStringAssertionProperties)
  println()
  println("Timestamps: \n" + TestConfig.toStringTimestamps)
  println()
  
  var defaultInjectionProfile = Array[InjectionStep] (
      rampUsersPerSec(0.001) to TestConfig.usersPerSec during(TestConfig.rampUpPeriod),
      constantUsersPerSec(TestConfig.usersPerSec) during(TestConfig.warmUpPeriod + TestConfig.measurementPeriod)   
  )
  
  def printSpecificTestParameters {
    // override in subclass
  }
  
  def rampDownNotStarted(): Validation[Boolean] = {
    System.currentTimeMillis < TestConfig.measurementEndTime
  }

  before {
    TestConfig.validateConfiguration
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
