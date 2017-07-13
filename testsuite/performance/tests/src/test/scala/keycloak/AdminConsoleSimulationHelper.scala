package keycloak

import java.time.format.DateTimeFormatter

import io.gatling.core.Predef._
import org.jboss.perf.util.Util
import org.keycloak.performance.TestConfig

/**
  * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
  */
object AdminConsoleSimulationHelper {

  val UI_HEADERS = Map(
    "Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    "Upgrade-Insecure-Requests" -> "1")

  val ACCEPT_JSON = Map("Accept" -> "application/json")
  val ACCEPT_ALL = Map("Accept" -> "*/*")
  val AUTHORIZATION = Map("Authorization" -> "Bearer ${accessToken}")

  val APP_URL = "${keycloakServer}/admin/master/console/"
  val DATE_FMT = DateTimeFormatter.RFC_1123_DATE_TIME


  def getRandomUser() : String = {
    "user_" + (Util.random.nextDouble() * TestConfig.usersPerRealm).toInt
  }

  def needTokenRefresh(sess: Session): Boolean = {
    val lastRefresh = sess("accessTokenRefreshTime").as[Long]

    // 5 seconds before expiry is time to refresh
    lastRefresh + sess("expiresIn").as[String].toInt * 1000 - 5000 < System.currentTimeMillis() ||
      // or if refreshTokenPeriod is set force refresh even if not necessary
      (TestConfig.refreshTokenPeriod > 0 &&
        lastRefresh + TestConfig.refreshTokenPeriod * 1000 < System.currentTimeMillis())
  }
}
