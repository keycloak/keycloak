package keycloak

import io.gatling.core.Predef._
import io.gatling.core.validation.Validation
import io.gatling.http.Predef._
import org.jboss.perf.util.Util
import org.keycloak.performance.TestConfig
import org.keycloak.gatling.Utils._
import SimulationsHelper._


/**
  * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
  */
class AdminConsoleSimulation extends Simulation {

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

  val adminSession = exec(s => {
      val realm = TestConfig.randomRealmsIterator().next()
      val serverUrl = TestConfig.serverUrisList.get(0)
      s.setAll(
        "keycloakServer" -> serverUrl,
        "keycloakServerUrlEncoded" -> urlencode(serverUrl),
        "keycloakServerRootEncoded" -> urlEncodedRoot(serverUrl),
        "state" -> Util.randomUUID(),
        "nonce" -> Util.randomUUID(),
        "randomClientId" -> ("client_" + Util.randomUUID()),
        "realm" -> realm,
        "username" -> "admin",
        "password" -> "admin",
        "clientId" -> "security-admin-console"
      )
    })

    .exitHereIfFailed
    .openAdminConsoleHome()

    .thinkPause()
    .acsim_loginThroughLoginForm()
    .exitHereIfFailed

    .thinkPause()
    .acsim_openClients()

    .thinkPause()
    .acsim_openCreateNewClient()

    .thinkPause()
    .acsim_submitNewClient()

    .thinkPause()
    .acsim_updateClient()

    .thinkPause()
    .acsim_openClients()

    .thinkPause()
    .acsim_openClientDetails()

    .thinkPause()
    .acsim_openUsers()

    .thinkPause()
    .acsim_viewAllUsers()

    .thinkPause()
    .acsim_viewTenPagesOfUsers()

    .thinkPause()
    .acsim_find20Users()

    .thinkPause()
    .acsim_findUnlimitedUsers()

    .thinkPause()
    .acsim_findRandomUser()

    .acsim_openUser()

    .thinkPause()
    .acsim_openUserCredentials()

    .thinkPause()
    .acsim_setTemporaryPassword()

    .thinkPause()
    .acsim_logOut()


  val adminScenario = scenario("AdminConsole")
    .asLongAs(s => rampDownPeriodNotReached(), null, TestConfig.rampDownASAP) {
      pace(TestConfig.pace)
      adminSession
    }

  setUp(adminScenario
    .inject(rampUsers(TestConfig.runUsers) over TestConfig.rampUpPeriod)
    .protocols(httpProtocol))

  
  def rampDownPeriodNotReached(): Validation[Boolean] = {
    System.currentTimeMillis < TestConfig.rampDownPeriodStartTime
  }
  
}
