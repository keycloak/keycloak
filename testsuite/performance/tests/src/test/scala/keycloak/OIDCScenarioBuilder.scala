package keycloak

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import org.keycloak.gatling.Predef._
import keycloak.OIDCScenarioBuilder._

import java.util.concurrent.atomic.AtomicInteger

import io.gatling.core.pause.Normal
import io.gatling.core.session.Session
import io.gatling.core.structure.ChainBuilder
import io.gatling.core.validation.Validation
import org.jboss.perf.util.Util
import org.jboss.perf.util.Util.randomUUID
import org.keycloak.adapters.spi.HttpFacade.Cookie
import org.keycloak.gatling.AuthorizeAction
import org.keycloak.performance.TestConfig
import org.keycloak.performance.templates.DatasetTemplate


/**
  * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
  */
object OIDCScenarioBuilder {

  val BASE_URL = "${keycloakServer}/realms/${realm}"
  val LOGIN_ENDPOINT = BASE_URL + "/protocol/openid-connect/auth"
  val LOGOUT_ENDPOINT = BASE_URL + "/protocol/openid-connect/logout"

  // Specify defaults for http requests
  val UI_HEADERS = Map(
    "Accept" -> "text/html,application/xhtml+xml,application/xml",
    "Accept-Encoding" -> "gzip, deflate",
    "Accept-Language" -> "en-US,en;q=0.5",
    "User-Agent" -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

  val ACCEPT_JSON = Map("Accept" -> "application/json")
  val ACCEPT_ALL = Map("Accept" -> "*/*")

  def downCounterAboveZero(session: Session, attrName: String): Validation[Boolean] = {
    val missCounter = session.attributes.get(attrName) match {
      case Some(result) => result.asInstanceOf[AtomicInteger]
      case None => new AtomicInteger(0)
    }
    missCounter.getAndDecrement() > 0
  }
  
  val httpDefault = http
    .acceptHeader("application/json")
    .disableFollowRedirect
    .inferHtmlResources

  val loginAndLogoutScenario = new OIDCScenarioBuilder()
      .browserOpensLoginPage()
      .thinkPause()
      .browserPostsWrongCredentials()
      .browserPostsCorrectCredentials()

      // Act as client adapter - exchange code for keys
      .adapterExchangesCodeForTokens()

      .refreshTokenSeveralTimes()

      .thinkPause()
      .randomLogout()
  
  val registerAndLogoutScenario = new OIDCScenarioBuilder()
      .browserOpensLoginPage()
      .thinkPause()
      .browserOpensRegistrationPage()
      .thinkPause()
      .browserPostsRegistrationDetails()
      .adapterExchangesCodeForTokens()
      .thinkPause()
      .randomLogout()
      
  val datasetTemplate = new DatasetTemplate()
  datasetTemplate.validateConfiguration
  val dataset = datasetTemplate.produce
  val usersIterator = dataset.randomUsersIterator

}


class OIDCScenarioBuilder {

  var chainBuilder = exec(s => {

      val user = usersIterator.next
      val client = user.randomConfidentialClientIterator.next

      AuthorizeAction.init(s)
        .setAll("keycloakServer" -> TestConfig.serverUrisIterator.next(),
          "state" -> randomUUID(),
          "wrongPasswordCount" -> new AtomicInteger(TestConfig.badLoginAttempts),
          "refreshTokenCount" -> new AtomicInteger(TestConfig.refreshTokenCount),
          "realm" -> user.getRealm.toString,
          "firstName" -> user.getRepresentation.getFirstName,
          "lastName" -> user.getRepresentation.getLastName,
          "email" -> user.getRepresentation.getEmail,
          "username" -> user.getRepresentation.getUsername,
          "password" -> user.getCredentials.get(0).getRepresentation.getValue,
          "clientId" -> client.getRepresentation.getClientId,
          "secret" -> client.getRepresentation.getSecret,
          "appUrl" -> client.getRepresentation.getBaseUrl
        )
    })
    .exitHereIfFailed

  def thinkPause() : OIDCScenarioBuilder = {
    chainBuilder = chainBuilder.pause(TestConfig.userThinkTime, Normal(TestConfig.userThinkTime * 0.2))
    this
  }

  def thinkPause(builder: ChainBuilder) : ChainBuilder = {
    builder.pause(TestConfig.userThinkTime, Normal(TestConfig.userThinkTime * 0.2))
  }

  def newThinkPause() : ChainBuilder = {
    pause(TestConfig.userThinkTime, Normal(TestConfig.userThinkTime * 0.2))
  }
  
  def browserOpensLoginPage() : OIDCScenarioBuilder = {
    chainBuilder = chainBuilder
      .exec(http("Browser to Log In Endpoint")
        .get(LOGIN_ENDPOINT)
        .headers(UI_HEADERS)
        .queryParam("login", "true")
        .queryParam("response_type", "code")
        .queryParam("client_id", "${clientId}")
        .queryParam("state", "${state}")
        .queryParam("redirect_uri", "${appUrl}")
        .check(status.is(200), 
          regex("action=\"([^\"]*)\"").find.transform(_.replaceAll("&amp;", "&")).saveAs("login-form-uri"),
          regex("href=\"/auth(/realms/[^\"]*/login-actions/registration[^\"]*)\"").find.transform(_.replaceAll("&amp;", "&")).saveAs("registration-link")))
        // if already logged in the check will fail with:
        // status.find.is(200), but actually found 302
        // The reason is that instead of returning the login page we are immediately redirected to the app that requested authentication
      .exitHereIfFailed
    this
  }

  def browserPostsWrongCredentials() : OIDCScenarioBuilder = {
    chainBuilder = chainBuilder
      .asLongAs(s => downCounterAboveZero(s, "wrongPasswordCount")) {
        var c = exec(http("Browser posts wrong credentials")
          .post("${login-form-uri}")
          .headers(UI_HEADERS)
          .formParam("username", "${username}")
          .formParam("password", _ => Util.randomString(10))
          .formParam("login", "Log in")
          .check(status.is(200), regex("action=\"([^\"]*)\"").find.transform(_.replaceAll("&amp;", "&")).saveAs("login-form-uri")))
          .exitHereIfFailed

        // make sure to call the right version of thinkPause - one that takes chainBuilder as argument
        // - because this is a nested chainBuilder - not the same as chainBuilder field
        thinkPause(c)
      }
    this
  }

  def browserPostsCorrectCredentials() : OIDCScenarioBuilder = {
    chainBuilder = chainBuilder
      .exec(http("Browser posts correct credentials")
        .post("${login-form-uri}")
        .headers(UI_HEADERS)
        .formParam("username", "${username}")
        .formParam("password", "${password}")
        .formParam("login", "Log in")
        .check(status.is(302), header("Location").saveAs("login-redirect")))
      .exitHereIfFailed
    this
  }

  def browserOpensRegistrationPage() : OIDCScenarioBuilder = {
    chainBuilder = chainBuilder
      .exec(http("Browser to Registration Endpoint")
        .get("${keycloakServer}${registration-link}")
        .headers(UI_HEADERS)
        .check(
          status.is(200), 
          regex("action=\"([^\"]*)\"").find.transform(_.replaceAll("&amp;", "&")).saveAs("registration-form-uri"))
        )
      .exitHereIfFailed
    this
  }

  def browserPostsRegistrationDetails() : OIDCScenarioBuilder = {
    chainBuilder = chainBuilder
      .exec(http("Browser posts registration details")
        .post("${registration-form-uri}")
        .headers(UI_HEADERS)
        .formParam("firstName", "${firstName}")
        .formParam("lastName", "${lastName}")
        .formParam("email", "${email}")
        .formParam("username", "${username}")
        .formParam("password", "${password}")
        .formParam("password-confirm", "${password}")
        .check(status.is(302), header("Location").saveAs("login-redirect")))
      .exitHereIfFailed
    this
  }

  def adapterExchangesCodeForTokens() : OIDCScenarioBuilder = {
    chainBuilder = chainBuilder
      .exec(oauth("Adapter exchanges code for tokens")
        .authorize("${login-redirect}",
          session => List(new Cookie("OAuth_Token_Request_State", session("state").as[String], 0, null, null)))
        .authServerUrl("${keycloakServer}")
        .resource("${clientId}")
        .clientCredentials("${secret}")
        .realm("${realm}")
        //.realmKey(Loader.realmRepresentation.getPublicKey)
      )
    this
  }

  def refreshTokenSeveralTimes() : OIDCScenarioBuilder = {
    chainBuilder = chainBuilder
      .asLongAs(s => downCounterAboveZero(s, "refreshTokenCount")) {
        // make sure to call newThinkPause rather than thinkPause
        newThinkPause()
          .exec(oauth("Adapter refreshes token").refresh())
      }
    this
  }

  def logoutChain() : ChainBuilder = {
    exec(http("Browser logout")
        .get(LOGOUT_ENDPOINT)
        .headers(UI_HEADERS)
        .queryParam("redirect_uri", "${appUrl}")
        .check(status.is(302), header("Location").is("${appUrl}")))
  }
  
  def logout() : OIDCScenarioBuilder = {
    chainBuilder = chainBuilder.exec(logoutChain)
    this
  }
  
  def randomLogout() : OIDCScenarioBuilder = {
    chainBuilder = chainBuilder
    .randomSwitch(
      // logout randomly based on logoutPct param
      TestConfig.logoutPct -> exec(logoutChain)
    )
    this
  }
  
}

