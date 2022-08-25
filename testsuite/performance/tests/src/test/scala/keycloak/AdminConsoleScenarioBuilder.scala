package keycloak

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import keycloak.AdminConsoleScenarioBuilder._

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import io.gatling.core.pause.Normal
import io.gatling.http.request.StringBody
import org.jboss.perf.util.Util
import org.jboss.perf.util.Util.randomUUID
import org.keycloak.gatling.Utils.{generateCodeChallenge, generateCodeVerifier, urlEncodedRoot, urlencode}
import org.keycloak.performance.TestConfig
import org.keycloak.performance.templates.DatasetTemplate


/**
  * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
  */

object AdminConsoleScenarioBuilder {

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

    val datasetTemplate = new DatasetTemplate()
    datasetTemplate.validateConfiguration
    val dataset = datasetTemplate.produce
    val realmsIterator = dataset.randomRealmIterator
  
}

class AdminConsoleScenarioBuilder {

  var chainBuilder = exec(s => {
    val realm = realmsIterator.next
    val serverUrl = TestConfig.serverUrisIterator.next()
    val codeVerifier = generateCodeVerifier()
    val codeChallenge = generateCodeChallenge(codeVerifier)
    s.setAll(
      "keycloakServer" -> serverUrl,
      "keycloakServerUrlEncoded" -> urlencode(serverUrl),
      "keycloakServerRootEncoded" -> urlEncodedRoot(serverUrl),
      "state" -> randomUUID(),
      "nonce" -> randomUUID(),
      "randomClientId" -> ("client_" + randomUUID()),
      "realm" -> realm.getRepresentation.getRealm,
      "username" -> TestConfig.authUser,
      "password" -> TestConfig.authPassword,
      "clientId" -> "security-admin-console",
      "codeVerifier" -> codeVerifier,
      "codeChallenge" -> codeChallenge
    )
  }).exitHereIfFailed



  def thinkPause() : AdminConsoleScenarioBuilder = {
    chainBuilder = chainBuilder.pause(TestConfig.userThinkTime, Normal(TestConfig.userThinkTime * 0.2))
    this
  }

  def needTokenRefresh(sess: Session): Boolean = {
    val lastRefresh = sess("accessTokenRefreshTime").as[Long]

    // 5 seconds before expiry is time to refresh
    lastRefresh + sess("expiresIn").as[String].toInt * 1000 - 5000 < System.currentTimeMillis() ||
      // or if refreshTokenPeriod is set force refresh even if not necessary
      (TestConfig.refreshTokenPeriod > 0 &&
        lastRefresh + TestConfig.refreshTokenPeriod * 1000 < System.currentTimeMillis())
  }

  def refreshTokenIfExpired() : AdminConsoleScenarioBuilder = {
    chainBuilder = chainBuilder
      .doIf(s => needTokenRefresh(s)) {
        exec(http("JS Adapter Token - Refresh tokens")
          .post("/auth/realms/master/protocol/openid-connect/token")
          .headers(ACCEPT_ALL)
          .formParam("grant_type", "refresh_token")
          .formParam("refresh_token", "${refreshToken}")
          .formParam("client_id", "security-admin-console")
          .check(status.is(200),
            jsonPath("$.access_token").saveAs("accessToken"),
            jsonPath("$.refresh_token").saveAs("refreshToken"),
            jsonPath("$.expires_in").saveAs("expiresIn"),
            header("Date").saveAs("tokenTime")))

          .exec(s => {
            s.set("accessTokenRefreshTime", ZonedDateTime.parse(s("tokenTime").as[String], DATE_FMT).toEpochSecond * 1000)
          })
      }
    this
  }

  def openAdminConsoleHome() : AdminConsoleScenarioBuilder = {
    chainBuilder = chainBuilder
      .exec(http("Console Home")
        .get("/auth/admin/")
        .headers(UI_HEADERS)
        .check(status.is(302))
        .resources(
          http("Console Redirect")
            .get("/auth/admin/master/console/")
            .headers(UI_HEADERS)
            .check(status.is(200), regex("<link.+\\/resources\\/([^\\/]+).+>").saveAs("resourceVersion")),
          http("Console REST - Config")
            .get("/auth/admin/master/console/config")
            .headers(ACCEPT_JSON)
            .check(status.is(200))
        ))
    this
  }

  def loginThroughLoginForm() : AdminConsoleScenarioBuilder = {
    chainBuilder = chainBuilder
      .exec(http("JS Adapter Auth - Login Form Redirect")
        .get("/auth/realms/master/protocol/openid-connect/auth?client_id=security-admin-console&redirect_uri=${keycloakServerUrlEncoded}%2Fadmin%2Fmaster%2Fconsole%2F&state=${state}&nonce=${nonce}&response_mode=fragment&response_type=code&scope=openid&code_challenge=${codeChallenge}&code_challenge_method=S256")
        .headers(UI_HEADERS)
        .check(status.is(200), regex("action=\"([^\"]*)\"").find.transform(_.replaceAll("&amp;", "&")).saveAs("login-form-uri")))
      .exitHereIfFailed

    // thinkPause
    thinkPause()

    // Successful login
    chainBuilder = chainBuilder
      .exec(http("Login Form - Submit Correct Credentials")
        .post("${login-form-uri}")
        .formParam("username", "${username}")
        .formParam("password", "${password}")
        .formParam("login", "Log in")
        .check(status.is(302),
          header("Location").saveAs("login-redirect"),
          headerRegex("Location", "code=([^&]+)").saveAs("code")))
      // TODO store AUTH_SESSION_ID cookie for use with oauth.authorize?
      .exitHereIfFailed

      .exec(http("Console Redirect")
        .get("/auth/admin/master/console/")
        .headers(UI_HEADERS)
        .check(status.is(200))
        .resources(
          http("Console REST - Config")
            .get("/auth/admin/master/console/config")
            .headers(ACCEPT_JSON)
            .check(status.is(200)),

          http("JS Adapter Token - Exchange code for tokens")
            .post("/auth/realms/master/protocol/openid-connect/token")
            .headers(ACCEPT_ALL)
            .formParam("code", "${code}")
            .formParam("code_verifier", "${codeVerifier}")
            .formParam("grant_type", "authorization_code")
            .formParam("client_id", "security-admin-console")
            .formParam("redirect_uri", APP_URL)
            .check(status.is(200),
              jsonPath("$.access_token").saveAs("accessToken"),
              jsonPath("$.refresh_token").saveAs("refreshToken"),
              jsonPath("$.expires_in").saveAs("expiresIn"),
              header("Date").saveAs("tokenTime")),

          http("Console REST - messages.json")
            .get("/auth/admin/master/console/messages.json?lang=en")
            .headers(ACCEPT_JSON)
            .check(status.is(200)),

          // iframe status listener
          // TODO: properly set Referer
          http("IFrame Status Init")
            .get("/auth/realms/master/protocol/openid-connect/login-status-iframe.html/init?client_id=security-admin-console&origin=${keycloakServerRootEncoded}") // ${keycloakServerUrlEncoded}
            .headers(ACCEPT_ALL) //  ++ Map("Referer" -> "/auth/realms/master/protocol/openid-connect/login-status-iframe.html?version=3.3.0.cr1-201708011508") ${resourceVersion}
            .check(status.is(204))
        )
      )
      .exec(s => {
        // How to not have to duplicate this block of code?
        s.set("accessTokenRefreshTime", ZonedDateTime.parse(s("tokenTime").as[String], DATE_FMT).toEpochSecond * 1000)
      })
      .exec(http("Console REST - whoami")
        .get("/auth/admin/master/console/whoami")
        .headers(ACCEPT_JSON ++ AUTHORIZATION)
        .check(status.is(200)))

      .exec(http("Console REST - realms")
        .get("/auth/admin/realms")
        .headers(AUTHORIZATION)
        .check(status.is(200)))

      .exec(http("Console REST - serverinfo")
        .get("/auth/admin/serverinfo")
        .headers(AUTHORIZATION)
        .check(status.is(200)))

      .exec(http("Console REST - realms")
        .get("/auth/admin/realms")
        .headers(AUTHORIZATION)
        .check(status.is(200)))

      .exec(http("Console REST - ${realm}")
        .get("/auth/admin/realms/${realm}")
        .headers(AUTHORIZATION)
        .check(status.is(200)))

      .exec(http("Console REST - realms")
        .get("/auth/admin/realms")
        .headers(AUTHORIZATION)
        .check(status.is(200)))
    this
  }

  def openRealmSettings() : AdminConsoleScenarioBuilder = {
    refreshTokenIfExpired()
    chainBuilder = chainBuilder.exec(http("Console Realm Settings")
      .get("/auth/resources/${resourceVersion}/admin/keycloak/partials/realm-detail.html")
      .headers(UI_HEADERS)
      .check(status.is(200))
      .resources(
        http("Console REST - ${realm}")
          .get("/auth/admin/realms/${realm}")
          .headers(AUTHORIZATION)
          .check(status.is(200)),

        http("Console REST - realms")
          .get("/auth/admin/realms")
          .headers(AUTHORIZATION)
          .check(status.is(200)),

        http("Console REST - realms")
          .get("/auth/admin/realms")
          .headers(AUTHORIZATION)
          .check(status.is(200)),

        http("Console - kc-tabs-realm.html")
          .get("/auth/resources/${resourceVersion}/admin/keycloak/templates/kc-tabs-realm.html")
          //.headers(UI_HEADERS ++ Map("Referer" -> ""))  // TODO fix referer
          .headers(UI_HEADERS)
          .check(status.is(200)),

        http("Console - kc-menu.html")
          .get("/auth/resources/${resourceVersion}/admin/keycloak/templates/kc-menu.html")
          //.headers(UI_HEADERS ++ Map("Referer" -> ""))  // TODO fix referer
          .headers(UI_HEADERS)
          .check(status.is(200))
      )
    )
    .exitHereIfFailed
    this
  }

  def openClients() : AdminConsoleScenarioBuilder = {
    refreshTokenIfExpired()
    chainBuilder = chainBuilder
      .exec(http("Console - client-list.html")
        .get("/auth/resources/${resourceVersion}/admin/keycloak/partials/client-list.html")
        .headers(UI_HEADERS)
        .check(status.is(200))
        .resources(
          http("Console REST - ${realm}")
            .get("/auth/admin/realms/${realm}")
            .headers(AUTHORIZATION)
            .check(status.is(200)),
          http("Console REST - realms")
            .get("/auth/admin/realms")
            .headers(AUTHORIZATION)
            .check(status.is(200)),
          http("Console - kc-paging.html")
            .get("/auth/resources/${resourceVersion}/admin/keycloak/templates/kc-paging.html")
            .headers(UI_HEADERS)
            .check(status.is(200)),
          http("Console REST - ${realm}/clients")
            .get("/auth/admin/realms/${realm}/clients?viewableOnly=true")
            .headers(AUTHORIZATION)
            .check(status.is(200))
        )
      )
    this
  }

  def openCreateNewClient() : AdminConsoleScenarioBuilder = {
    refreshTokenIfExpired()
    chainBuilder = chainBuilder
      .exec(http("Console - create-client.html")
        .get("/auth/resources/${resourceVersion}/admin/keycloak/partials/create-client.html")
        .headers(UI_HEADERS)
        .check(status.is(200))
        .resources(
          http("Console REST - ${realm}/clients")
            .get("/auth/admin/realms/${realm}/clients")
            .headers(AUTHORIZATION)
            .check(status.is(200))
        )
      )
    this
  }

  def submitNewClient() : AdminConsoleScenarioBuilder = {
    refreshTokenIfExpired()
    chainBuilder = chainBuilder
      .exec(http("Console REST - ${realm}/clients POST")
        .post("/auth/admin/realms/${realm}/clients")
        .headers(AUTHORIZATION)
        .header("Content-Type", "application/json")
        .body(StringBody(
          """
               {"enabled":true,"attributes":{},"redirectUris":[],"clientId":"${randomClientId}","rootUrl":"http://localhost:8081/myapp","protocol":"openid-connect"}
            """.stripMargin))
        .check(status.is(201), headerRegex("Location", "\\/([^\\/]+)$").saveAs("idOfClient")))

      .exec(http("Console REST - ${realm}/clients/ID")
        .get("/auth/admin/realms/${realm}/clients/${idOfClient}")
        .headers(AUTHORIZATION)
        .check(status.is(200), bodyString.saveAs("clientJson"))
        .resources(
          http("Console REST - ${realm}/clients")
            .get("/auth/admin/realms/${realm}/clients")
            .headers(AUTHORIZATION)
            .check(status.is(200)),

          http("Console REST - ${realm}/client-templates")
            .get("/auth/admin/realms/${realm}/client-templates")
            .headers(AUTHORIZATION)
            .check(status.is(200)),

          http("Console REST - ${realm}")
            .get("/auth/admin/realms/${realm}")
            .headers(AUTHORIZATION)
            .check(status.is(200)),

          http("Console REST - realms")
            .get("/auth/admin/realms")
            .headers(AUTHORIZATION)
            .check(status.is(200))
        )
      )
    this
  }

  def updateClient() : AdminConsoleScenarioBuilder = {
    refreshTokenIfExpired()
    chainBuilder = chainBuilder.exec(s => {
      s.set("updateClientJson", s("clientJson").as[String].replace("\"publicClient\":false", "\"publicClient\":true"))
    })
      .exec(http("Console REST - ${realm}/clients/ID PUT")
        .put("/auth/admin/realms/${realm}/clients/${idOfClient}")
        .headers(AUTHORIZATION)
        .header("Content-Type", "application/json")
        .body(StringBody("${updateClientJson}"))
        .check(status.is(204)))

      .exec(http("Console REST - ${realm}/clients/ID")
        .get("/auth/admin/realms/${realm}/clients/${idOfClient}")
        .headers(AUTHORIZATION)
        .check(status.is(200), bodyString.saveAs("clientJson"))
        .resources(
          http("Console REST - ${realm}/clients")
            .get("/auth/admin/realms/${realm}/clients")
            .headers(AUTHORIZATION)
            .check(status.is(200)),

          http("Console REST - ${realm}/client-templates")
            .get("/auth/admin/realms/${realm}/client-templates")
            .headers(AUTHORIZATION)
            .check(status.is(200)),

          http("Console REST - ${realm}")
            .get("/auth/admin/realms/${realm}")
            .headers(AUTHORIZATION)
            .check(status.is(200)),

          http("Console REST - realms")
            .get("/auth/admin/realms")
            .headers(AUTHORIZATION)
            .check(status.is(200))
        )
      )
    this
  }

  def openClientDetails() : AdminConsoleScenarioBuilder = {
    refreshTokenIfExpired()
    chainBuilder = chainBuilder
      .exec(http("Console - client-detail.html")
        .get("/auth/resources/${resourceVersion}/admin/keycloak/partials/client-detail.html")
        .headers(UI_HEADERS)
        .check(status.is(200))
        .resources(
          http("Console REST - ${realm}/client-templates")
            .get("/auth/admin/realms/${realm}/client-templates")
            .headers(AUTHORIZATION)
            .check(status.is(200)),

          http("Console REST - ${realm}")
            .get("/auth/admin/realms/${realm}")
            .headers(AUTHORIZATION)
            .check(status.is(200)),

          http("Console REST - realms")
            .get("/auth/admin/realms")
            .headers(AUTHORIZATION)
            .check(status.is(200)),

          http("Console REST - ${realm}/clients")
            .get("/auth/admin/realms/${realm}/clients")
            .headers(AUTHORIZATION)
            .check(status.is(200)),

          http("Console REST - ${realm}/clients/ID")
            .get("/auth/admin/realms/${realm}/clients/${idOfClient}")
            .headers(AUTHORIZATION)
            .check(status.is(200)),

          http("Console - kc-tabs-client.html")
            .get("/auth/resources/${resourceVersion}/admin/keycloak/templates/kc-tabs-client.html")
            .headers(UI_HEADERS)
            .check(status.is(200))
        )
      )
    this
  }

  def openUsers() : AdminConsoleScenarioBuilder = {
    refreshTokenIfExpired()
    chainBuilder = chainBuilder
      .exec(http("Console - user-list.html")
        .get("/auth/resources/${resourceVersion}/admin/keycloak/partials/user-list.html")
        .headers(UI_HEADERS)
        .check(status.is(200))
        .resources(
          http("Console REST - realms")
            .get("/auth/admin/realms")
            .headers(AUTHORIZATION)
            .check(status.is(200)),
          http("Console REST - ${realm}")
            .get("/auth/admin/realms/${realm}")
            .headers(AUTHORIZATION)
            .check(status.is(200)),
          http("Console - kc-tabs-users.html")
            .get("/auth/resources/${resourceVersion}/admin/keycloak/templates/kc-tabs-users.html")
            .headers(UI_HEADERS)
            .check(status.is(200))
        )
      )
    this
  }

  def viewAllUsers() : AdminConsoleScenarioBuilder = {
    refreshTokenIfExpired()
    chainBuilder = chainBuilder
      .exec(http("Console REST - ${realm}/users")
        .get("/auth/admin/realms/${realm}/users?first=0&max=20")
        .headers(AUTHORIZATION)
        .check(status.is(200))
      )
    this
  }

  def viewTenPagesOfUsers() : AdminConsoleScenarioBuilder = {
    refreshTokenIfExpired()
    chainBuilder = chainBuilder
      .repeat(10, "i") {
        exec(s => s.set("offset", s("i").as[Int] * 20))
          .pause(1)
          .exec(http("Console REST - ${realm}/users?first=${offset}")
            .get("/auth/admin/realms/${realm}/users?first=${offset}&max=20")
            .headers(AUTHORIZATION)
            .check(status.is(200))
          )
      }
    this
  }

  def find20Users() : AdminConsoleScenarioBuilder = {
    refreshTokenIfExpired()
    chainBuilder = chainBuilder
      .exec(http("Console REST - ${realm}/users?first=0&max=20&search=user")
        .get("/auth/admin/realms/${realm}/users?first=0&max=20&search=user")
        .headers(AUTHORIZATION)
        .check(status.is(200))
      )
    this
  }

  def findUnlimitedUsers() : AdminConsoleScenarioBuilder = {
    refreshTokenIfExpired()
    chainBuilder = chainBuilder
      .exec(http("Console REST - ${realm}/users?search=user")
        .get("/auth/admin/realms/${realm}/users?search=user")
        .headers(AUTHORIZATION)
        .check(status.is(200))
      )
    this
  }

  def findRandomUser() : AdminConsoleScenarioBuilder = {
    refreshTokenIfExpired()
    chainBuilder = chainBuilder
      .exec(s => s.set("randomUsername", getRandomUser()))
      .exec(http("Console REST - ${realm}/users?first=0&max=20&search=USERNAME")
        .get("/auth/admin/realms/${realm}/users?first=0&max=20&search=${randomUsername}")
        .headers(AUTHORIZATION)
        .check(status.is(200), jsonPath("$[0]['id']").saveAs("userId"))
      )
    this
  }

  def openUser() : AdminConsoleScenarioBuilder = {
    refreshTokenIfExpired()
    chainBuilder = chainBuilder
      .exec(http("Console - user-detail.html")
        .get("/auth/resources/${resourceVersion}/admin/keycloak/partials/user-detail.html")
        .headers(UI_HEADERS)
        .check(status.is(200))
        .resources(

          http("Console REST - realms")
            .get("/auth/admin/realms")
            .headers(AUTHORIZATION)
            .check(status.is(200)),

          http("Console REST - ${realm}")
            .get("/auth/admin/realms/${realm}")
            .headers(AUTHORIZATION)
            .check(status.is(200)),

          http("Console REST - ${realm}/users/ID")
            .get("/auth/admin/realms/${realm}/users/${userId}")
            .headers(AUTHORIZATION)
            .check(status.is(200)),

          http("Console - kc-tabs-user.html")
            .get("/auth/resources/${resourceVersion}/admin/keycloak/templates/kc-tabs-user.html")
            .headers(UI_HEADERS)
            .check(status.is(200)),

          http("Console REST - ${realm}/authentication/required-actions")
            .get("/auth/admin/realms/${realm}/authentication/required-actions")
            .headers(AUTHORIZATION)
            .check(status.is(200)),

          http("Console REST - ${realm}/attack-detection/brute-force/users/ID")
            .get("/auth/admin/realms/${realm}/attack-detection/brute-force/users/${userId}")
            .headers(AUTHORIZATION)
            .check(status.is(200))
        )
      )
    this
  }

  def openUserCredentials() : AdminConsoleScenarioBuilder = {
    refreshTokenIfExpired()
    chainBuilder = chainBuilder
      .exec(http("Console - user-credentials.html")
        .get("/auth/resources/${resourceVersion}/admin/keycloak/partials/user-credentials.html")
        .headers(UI_HEADERS)
        .check(status.is(200))
        .resources(

          http("Console REST - ${realm}/users/ID")
            .get("/auth/admin/realms/${realm}/users/${userId}")
            .headers(AUTHORIZATION)
            .check(status.is(200)),

          http("Console REST - ${realm}")
            .get("/auth/admin/realms/${realm}")
            .headers(AUTHORIZATION)
            .check(status.is(200)),

          http("Console REST - realms")
            .get("/auth/admin/realms")
            .headers(AUTHORIZATION)
            .check(status.is(200)),

          http("Console REST - ${realm}/authentication/required-actions")
            .get("/auth/admin/realms/${realm}/authentication/required-actions")
            .headers(AUTHORIZATION)
            .check(status.is(200))
        )
      )
    this
  }

  def setTemporaryPassword() : AdminConsoleScenarioBuilder = {
    refreshTokenIfExpired()
    chainBuilder = chainBuilder
      .exec(http("Console REST - ${realm}/users/ID/reset-password PUT")
        .put("/auth/admin/realms/${realm}/users/${userId}/reset-password")
        .headers(AUTHORIZATION)
        .header("Content-Type", "application/json")
        .body(StringBody("""{"type":"password","value":"testtest","temporary":true}"""))
        .check(status.is(204)
        )
      )
    this
  }

  def logout() : AdminConsoleScenarioBuilder = {
    refreshTokenIfExpired()
    chainBuilder = chainBuilder
      .exec(http("Browser logout")
        .get("/auth/realms/master/protocol/openid-connect/logout")
        .headers(UI_HEADERS)
        .queryParam("redirect_uri", APP_URL)
        .check(status.is(302), header("Location").is(APP_URL)
        )
      )
    this
  }
}
