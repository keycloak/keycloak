package keycloak

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import io.gatling.core.validation.Validation
import org.keycloak.performance.TestConfig


/**
  * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
  */
class AdminConsoleSimulation extends Simulation {

  def rampDownPeriodNotReached(): Validation[Boolean] = {
    System.currentTimeMillis < TestConfig.rampDownPeriodStartTime
  }



  println()
  println("Target server: " + TestConfig.serverUrisList.get(0))
  println()
  println("Using test parameters:\n" + TestConfig.toStringCommonTestParameters);
  println()
  println("Using dataset properties:\n" + TestConfig.toStringDatasetProperties)


  val httpProtocol = http
    .baseURL("http://localhost:8080")
    .disableFollowRedirect
    .inferHtmlResources()
    .acceptHeader("application/json, text/plain, */*")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.12; rv:54.0) Gecko/20100101 Firefox/54.0")




  val adminSession = new AdminConsoleScenarioBuilder()
    .openAdminConsoleHome()
    .thinkPause()
    .loginThroughLoginForm()

    .openRealmSettings()

    .thinkPause()
    .openClients()

    .thinkPause()
    .openCreateNewClient()

    .thinkPause()
    .submitNewClient()

    .thinkPause()
    .updateClient()

    .thinkPause()
    .openClients()

    .thinkPause()
    .openClientDetails()

    .thinkPause()
    .openUsers()

    .thinkPause()
    .viewAllUsers()

    .thinkPause()
    .viewTenPagesOfUsers()

    .thinkPause()
    .find20Users()

    .thinkPause()
    .findUnlimitedUsers()

    .thinkPause()
    .findRandomUser()
    .openUser()

    .thinkPause()
    .openUserCredentials()

    .thinkPause()
    .setTemporaryPassword()

    .thinkPause()
    .logout()

    .thinkPause()


  val adminScenario = scenario("AdminConsole")
    .asLongAs(s => rampDownPeriodNotReached(), null, TestConfig.rampDownASAP) {
      pace(TestConfig.pace)
      adminSession.chainBuilder
    }

  setUp(adminScenario
    .inject(rampUsers(TestConfig.runUsers) over TestConfig.rampUpPeriod)
    .protocols(httpProtocol))
}
