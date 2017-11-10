package keycloak

import java.time.ZonedDateTime

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import io.gatling.core.pause.Normal
import io.gatling.core.structure.ChainBuilder
import keycloak.AdminConsoleSimulationHelper._

import org.keycloak.performance.TestConfig


/**
  * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
  */
object SimulationsHelper {

  implicit class SimulationsChainBuilderExtras(val builder: ChainBuilder) {

    def acsim_refreshTokenIfExpired() : ChainBuilder = {
      builder
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
    }

    def openAdminConsoleHome() : ChainBuilder = {
      builder
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
          )
        )
    }

    def acsim_loginThroughLoginForm() : ChainBuilder = {
      builder
        .exec(http("JS Adapter Auth - Login Form Redirect")
          .get("/auth/realms/master/protocol/openid-connect/auth?client_id=security-admin-console&redirect_uri=${keycloakServerUrlEncoded}%2Fadmin%2Fmaster%2Fconsole%2F&state=${state}&nonce=${nonce}&response_mode=fragment&response_type=code&scope=openid")
          .headers(UI_HEADERS)
          .check(status.is(200), regex("action=\"([^\"]*)\"").find.transform(_.replaceAll("&amp;", "&")).saveAs("login-form-uri")))
        .exitHereIfFailed
        .thinkPause()
        // Successful login
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
              .headers(ACCEPT_ALL)  //  ++ Map("Referer" -> "/auth/realms/master/protocol/openid-connect/login-status-iframe.html?version=3.3.0.cr1-201708011508") ${resourceVersion}
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

        // DO NOT forget the leading dot, or the wrong ScenarioBuilder will be returned
        .acsim_openRealmSettings()
    }

    def acsim_openRealmSettings() : ChainBuilder = {
      builder
        .acsim_refreshTokenIfExpired()
        .exec(http("Console Realm Settings")
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
            .check(status.is(200)),

          // request fonts for css also set referer
          http("OpenSans-Semibold-webfont.woff")
            .get("/auth/resources/${resourceVersion}/admin/keycloak/lib/patternfly/fonts/OpenSans-Semibold-webfont.woff")
            .headers(UI_HEADERS)
            .check(status.is(200)),

          http("OpenSans-Bold-webfont.woff")
            .get("/auth/resources/${resourceVersion}/admin/keycloak/lib/patternfly/fonts/OpenSans-Bold-webfont.woff")
            .headers(UI_HEADERS)
            .check(status.is(200)),

          http("OpenSans-Light-webfont.woff")
            .get("/auth/resources/${resourceVersion}/admin/keycloak/lib/patternfly/fonts/OpenSans-Light-webfont.woff")
            .headers(UI_HEADERS)
            .check(status.is(200))
        )
      )
    }

    def acsim_openClients() : ChainBuilder = {
      builder
        .acsim_refreshTokenIfExpired()
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
    }

    def acsim_openCreateNewClient() : ChainBuilder = {
      builder
        .acsim_refreshTokenIfExpired()
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
    }

    def acsim_submitNewClient() : ChainBuilder = {
      builder
        .acsim_refreshTokenIfExpired()
        .exec(http("Console REST - ${realm}/clients POST")
          .post("/auth/admin/realms/${realm}/clients")
          .headers(AUTHORIZATION)
          .header("Content-Type", "application/json")
          .body(StringBody("""
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
    }

    def acsim_updateClient() : ChainBuilder = {
      builder
        .acsim_refreshTokenIfExpired()
        .exec(s => {
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
    }

    def acsim_openClientDetails() : ChainBuilder = {
      builder
        .acsim_refreshTokenIfExpired()
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
    }

    def acsim_openUsers() : ChainBuilder = {
      builder
        .acsim_refreshTokenIfExpired()
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
    }

    def acsim_viewAllUsers() : ChainBuilder = {
      builder
        .acsim_refreshTokenIfExpired()
        .exec(http("Console REST - ${realm}/users")
          .get("/auth/admin/realms/${realm}/users?first=0&max=20")
          .headers(AUTHORIZATION)
          .check(status.is(200))
        )
    }

    def acsim_viewTenPagesOfUsers() : ChainBuilder = {
      builder
        .acsim_refreshTokenIfExpired()
        .repeat(10, "i") {
          exec(s => s.set("offset", s("i").as[Int]*20))
            .pause(1)
            .exec(http("Console REST - ${realm}/users?first=${offset}")
              .get("/auth/admin/realms/${realm}/users?first=${offset}&max=20")
              .headers(AUTHORIZATION)
              .check(status.is(200))
            )
        }
    }

    def acsim_find20Users() : ChainBuilder = {
      builder
        .acsim_refreshTokenIfExpired()
        .exec(http("Console REST - ${realm}/users?first=0&max=20&search=user")
          .get("/auth/admin/realms/${realm}/users?first=0&max=20&search=user")
          .headers(AUTHORIZATION)
          .check(status.is(200))
        )
    }

    def acsim_findUnlimitedUsers() : ChainBuilder = {
      builder
        .acsim_refreshTokenIfExpired()
        .exec(http("Console REST - ${realm}/users?search=user")
          .get("/auth/admin/realms/${realm}/users?search=user")
          .headers(AUTHORIZATION)
          .check(status.is(200))
        )
    }

    def acsim_findRandomUser() : ChainBuilder = {
      builder
        .acsim_refreshTokenIfExpired()
        .exec(s => s.set("randomUsername", AdminConsoleSimulationHelper.getRandomUser()))
        .exec(http("Console REST - ${realm}/users?first=0&max=20&search=USERNAME")
          .get("/auth/admin/realms/${realm}/users?first=0&max=20&search=${randomUsername}")
          .headers(AUTHORIZATION)
          .check(status.is(200), jsonPath("$[0]['id']").saveAs("userId"))
        )
    }

    def acsim_openUser() : ChainBuilder = {
      builder
        .acsim_refreshTokenIfExpired()
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
    }

    def acsim_openUserCredentials() : ChainBuilder = {
      builder
        .acsim_refreshTokenIfExpired()
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
    }

    def acsim_setTemporaryPassword() : ChainBuilder = {
      builder
        .acsim_refreshTokenIfExpired()
        .exec(http("Console REST - ${realm}/users/ID/reset-password PUT")
          .put("/auth/admin/realms/${realm}/users/${userId}/reset-password")
          .headers(AUTHORIZATION)
          .header("Content-Type", "application/json")
          .body(StringBody("""{"type":"password","value":"testtest","temporary":true}"""))
          .check(status.is(204)
          )
        )
    }

    def acsim_logOut() : ChainBuilder = {
      builder
        .acsim_refreshTokenIfExpired()
        .exec(http("Browser logout")
          .get("/auth/realms/master/protocol/openid-connect/logout")
          .headers(UI_HEADERS)
          .queryParam("redirect_uri", APP_URL)
          .check(status.is(302), header("Location").is(APP_URL)
          )
        )
    }

    def thinkPause() : ChainBuilder = {
      builder.pause(TestConfig.userThinkTime, Normal(TestConfig.userThinkTime * 0.2))
    }
  }
}
