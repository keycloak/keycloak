package keycloak

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import keycloak.OIDCScenarioBuilder._

import org.keycloak.performance.TestConfig


class OIDCLoginAndLogoutSimulation extends CommonSimulation {

  override def printSpecificTestParameters {
    println("  refreshTokenCount: " + TestConfig.refreshTokenCount)
    println("  badLoginAttempts: " + TestConfig.badLoginAttempts)
  }
  
  val usersScenario = scenario("Logging-in Users").exec(loginAndLogoutScenario.chainBuilder)

  setUp(usersScenario.inject(defaultInjectionProfile).protocols(httpDefault))

  .assertions(
    global.failedRequests.count.lessThan(TestConfig.maxFailedRequests + 1),
    global.responseTime.mean.lessThan(TestConfig.maxMeanReponseTime)
  )
    
}
