package keycloak

import java.util.concurrent.atomic.AtomicInteger

import io.gatling.core.Predef._
import io.gatling.core.pause.Normal
import io.gatling.core.session._
import io.gatling.core.validation.Validation
import io.gatling.http.Predef._
import org.jboss.perf.util.Util
import org.keycloak.adapters.spi.HttpFacade.Cookie
import org.keycloak.gatling.AuthorizeAction
import org.keycloak.gatling.Predef._
import org.keycloak.performance.TestConfig

/**
  * @author Radim Vansa &lt;rvansa@redhat.com&gt;
  * @author Marko Strukelj &lt;mstrukel@redhat.com&gt;
  */
class DefaultSimulation extends Simulation {

  val BASE_URL = "${keycloakServer}/realms/${realm}"
  val LOGIN_ENDPOINT = BASE_URL + "/protocol/openid-connect/auth"
  val LOGOUT_ENDPOINT = BASE_URL + "/protocol/openid-connect/logout"



  println()
  println("Target servers: " + TestConfig.serverUrisList)
  println()

  println("Using test parameters:\n" + TestConfig.toStringCommonTestParameters);
  println("  refreshTokenCount: " + TestConfig.refreshTokenCount)
  println("  badLoginAttempts: " + TestConfig.badLoginAttempts)
  println()
  println("Using dataset properties:\n" + TestConfig.toStringDatasetProperties)


  val httpDefault = http
    .acceptHeader("application/json")
    .disableFollowRedirect
    .inferHtmlResources
    //.baseURL(SERVER_URI)

  // Specify defaults for http requests
  val UI_HEADERS = Map(
    "Accept" -> "text/html,application/xhtml+xml,application/xml",
    "Accept-Encoding" -> "gzip, deflate",
    "Accept-Language" -> "en-US,en;q=0.5",
    "User-Agent" -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

  val ACCEPT_JSON = Map("Accept" -> "application/json")
  val ACCEPT_ALL = Map("Accept" -> "*/*")

  val userSession = exec(s => {
    // initialize session with host, user, client app, login failure ratio ...
    val realm = TestConfig.randomRealmsIterator().next()
    val userInfo = TestConfig.getUsersIterator(realm).next()
    val clientInfo = TestConfig.getConfidentialClientsIterator(realm).next()

    AuthorizeAction.init(s)
      .setAll("keycloakServer" -> TestConfig.serverUrisIterator.next(),
        "state" -> Util.randomUUID(),
        "wrongPasswordCount" -> new AtomicInteger(TestConfig.badLoginAttempts),
        "refreshTokenCount" -> new AtomicInteger(TestConfig.refreshTokenCount),
        "realm" -> realm,
        "username" -> userInfo.username,
        "password" -> userInfo.password,
        "clientId" -> clientInfo.clientId,
        "secret" -> clientInfo.secret,
        "appUrl" -> clientInfo.appUrl
      )
    })
    .exitHereIfFailed
    .exec(http("Browser to Log In Endpoint")
      .get(LOGIN_ENDPOINT)
      .headers(UI_HEADERS)
      .queryParam("login", "true")
      .queryParam("response_type", "code")
      .queryParam("client_id", "${clientId}")
      .queryParam("state", "${state}")
      .queryParam("redirect_uri", "${appUrl}")
      .check(status.is(200), regex("action=\"([^\"]*)\"").find.transform(_.replaceAll("&amp;", "&")).saveAs("login-form-uri")))
    .exitHereIfFailed
    .pause(TestConfig.userThinkTime, Normal(TestConfig.userThinkTime * 0.2))

    .asLongAs(s => downCounterAboveZero(s, "wrongPasswordCount")) {
      exec(http("Browser posts wrong credentials")
        .post("${login-form-uri}")
        .headers(UI_HEADERS)
        .formParam("username", "${username}")
        .formParam("password", _ => Util.randomString(10))
        .formParam("login", "Log in")
        .check(status.is(200), regex("action=\"([^\"]*)\"").find.transform(_.replaceAll("&amp;", "&")).saveAs("login-form-uri")))
        .exitHereIfFailed
        .pause(TestConfig.userThinkTime, Normal(TestConfig.userThinkTime * 0.2))
    }

    // Successful login
    .exec(http("Browser posts correct credentials")
      .post("${login-form-uri}")
      .headers(UI_HEADERS)
      .formParam("username", "${username}")
      .formParam("password", "${password}")
      .formParam("login", "Log in")
      .check(status.is(302), header("Location").saveAs("login-redirect")))
    .exitHereIfFailed


    // Now act as client adapter - exchange code for keys
    .exec(oauth("Adapter exchanges code for tokens")
      .authorize("${login-redirect}",
      session => List(new Cookie("OAuth_Token_Request_State", session("state").as[String], 0, null, null)))
      .authServerUrl("${keycloakServer}")
      .resource("${clientId}")
      .clientCredentials("${secret}")
      .realm("${realm}")
    //.realmKey(Loader.realmRepresentation.getPublicKey)
    )

    // Refresh token several times
    .asLongAs(s => downCounterAboveZero(s, "refreshTokenCount")) {
      pause(TestConfig.refreshTokenPeriod, Normal(TestConfig.refreshTokenPeriod * 0.2))
      .exec(oauth("Adapter refreshes token").refresh())
    }

    // Logout
    .pause(TestConfig.userThinkTime, Normal(TestConfig.userThinkTime * 0.2))
    .exec(http("Browser logout")
      .get(LOGOUT_ENDPOINT)
      .headers(UI_HEADERS)
      .queryParam("redirect_uri", "${appUrl}")
      .check(status.is(302), header("Location").is("${appUrl}")))

  val usersScenario = scenario("users")
    .asLongAs(s => rampDownPeriodNotReached(), null, TestConfig.rampDownASAP) {
      pace(TestConfig.pace)
      userSession
    }

  setUp(usersScenario
    .inject(rampUsers(TestConfig.runUsers) over TestConfig.rampUpPeriod)
    .protocols(httpDefault))

  //
  // Function definitions
  //

  def downCounterAboveZero(session: Session, attrName: String): Validation[Boolean] = {
    val missCounter = session.attributes.get(attrName) match {
      case Some(result) => result.asInstanceOf[AtomicInteger]
      case None => new AtomicInteger(0)
    }
    missCounter.getAndDecrement() > 0
  }

  def rampDownPeriodNotReached(): Validation[Boolean] = {
    System.currentTimeMillis < TestConfig.rampDownPeriodStartTime
  }

}
