package keycloak

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import org.keycloak.gatling.Predef._
import keycloak.BasicOIDCScenarioBuilder._

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


/**
  * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
  */
object BasicOIDCScenarioBuilder {

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

  def rampDownPeriodNotReached(): Validation[Boolean] = {
    System.currentTimeMillis < TestConfig.rampDownPeriodStartTime
  }
}


class BasicOIDCScenarioBuilder {

  var chainBuilder = exec(s => {

      // initialize session with host, user, client app, login failure ratio ...
      val realm = TestConfig.randomRealmsIterator().next()
      val userInfo = TestConfig.getUsersIterator(realm).next()
      val clientInfo = TestConfig.getConfidentialClientsIterator(realm).next()

      AuthorizeAction.init(s)
        .setAll("keycloakServer" -> TestConfig.serverUrisIterator.next(),
          "state" -> randomUUID(),
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

  def thinkPause() : BasicOIDCScenarioBuilder = {
    chainBuilder = chainBuilder.pause(TestConfig.userThinkTime, Normal(TestConfig.userThinkTime * 0.2))
    this
  }

  def thinkPause(builder: ChainBuilder) : ChainBuilder = {
    builder.pause(TestConfig.userThinkTime, Normal(TestConfig.userThinkTime * 0.2))
  }

  def newThinkPause() : ChainBuilder = {
    pause(TestConfig.userThinkTime, Normal(TestConfig.userThinkTime * 0.2))
  }

  def browserOpensLoginPage() : BasicOIDCScenarioBuilder = {
    chainBuilder = chainBuilder
      .exec(http("Browser to Log In Endpoint")
        .get(LOGIN_ENDPOINT)
        .headers(UI_HEADERS)
        .queryParam("login", "true")
        .queryParam("response_type", "code")
        .queryParam("client_id", "${clientId}")
        .queryParam("state", "${state}")
        .queryParam("redirect_uri", "${appUrl}")
        .check(status.is(200), regex("action=\"([^\"]*)\"").find.transform(_.replaceAll("&amp;", "&")).saveAs("login-form-uri")))
        // if already logged in the check will fail with:
        // status.find.is(200), but actually found 302
        // The reason is that instead of returning the login page we are immediately redirected to the app that requested authentication
      .exitHereIfFailed
    this
  }

  def browserPostsWrongCredentials() : BasicOIDCScenarioBuilder = {
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

  def browserPostsCorrectCredentials() : BasicOIDCScenarioBuilder = {
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

  def adapterExchangesCodeForTokens() : BasicOIDCScenarioBuilder = {
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

  def refreshTokenSeveralTimes() : BasicOIDCScenarioBuilder = {
    chainBuilder = chainBuilder
      .asLongAs(s => downCounterAboveZero(s, "refreshTokenCount")) {
        // make sure to call newThinkPause rather than thinkPause
        newThinkPause()
          .exec(oauth("Adapter refreshes token").refresh())
      }
    this
  }

  def logout() : BasicOIDCScenarioBuilder = {
    chainBuilder = chainBuilder
      .exec(http("Browser logout")
        .get(LOGOUT_ENDPOINT)
        .headers(UI_HEADERS)
        .queryParam("redirect_uri", "${appUrl}")
        .check(status.is(302), header("Location").is("${appUrl}")))
    this
  }
}

