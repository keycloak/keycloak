package org.keycloak.performance

import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit._
import scala.collection.JavaConversions._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.core.session._
import io.gatling.core.structure.ChainBuilder
import org.keycloak.performance.iteration._
import org.keycloak.performance.dataset._
import org.keycloak.performance.dataset.idm._
import org.keycloak.performance.dataset.idm.authorization._

object AdminCLI extends AdminCLI

trait AdminCLI extends Admin {

  val adminCLIHttpConf = http
  .disableFollowRedirect
  .acceptHeader("application/json, text/plain, */*")

  object Auth {
    
    def init = exec(s => {
        val serverUrl = TestConfig.serverUrisIterator.next
        s.setAll(
          "keycloakServer" -> serverUrl,
          "username" -> TestConfig.authUser,
          "password" -> TestConfig.authPassword,
          "clientId" -> "admin-cli"
        )
      }).exitHereIfFailed
    
    def login = exec(
      http("Admin Login")
      .post("${keycloakServer}/realms/master/protocol/openid-connect/token")
      .headers(ACCEPT_ALL)
      .formParam("grant_type", "password")
      .formParam("username", "${username}")
      .formParam("password", "${password}")
      .formParam("client_id", "${clientId}")
      .check(status.is(200),
             jsonPath("$.access_token").saveAs("accessToken"),
             jsonPath("$.refresh_token").saveAs("refreshToken"),
             jsonPath("$.expires_in").saveAs("expiresIn"),
             header("Date").saveAs("tokenTime")))
    .exitHereIfFailed
    .exec{s => 
      s.set("accessTokenRefreshTime", ZonedDateTime.parse(s("tokenTime").as[String], DATE_FMT_RFC1123).toEpochSecond * 1000)
    }
    
    def needTokenRefresh(session: Session): Boolean = {
      val lastRefresh = session("accessTokenRefreshTime").as[Long]

      // 5 seconds before expiry is time to refresh
      lastRefresh + session("expiresIn").as[String].toInt * 1000 - 5000 < System.currentTimeMillis() ||
      // or if refreshTokenPeriod is set force refresh even if not necessary
      (TestConfig.refreshTokenPeriod > 0 &&
       lastRefresh + TestConfig.refreshTokenPeriod * 1000 < System.currentTimeMillis())
    }

    def refreshTokenIfExpired = doIf(s => needTokenRefresh(s)) {
      exec{s => println("Access Token Expired. Refreshing.")
           s}
      .exec(
        http("Refresh Token")
        .post("${keycloakServer}/realms/master/protocol/openid-connect/token")
        .headers(ACCEPT_ALL)
        .formParam("grant_type", "refresh_token")
        .formParam("refresh_token", "${refreshToken}")
        .formParam("client_id", "admin-cli")
        .check(status.is(200),
               jsonPath("$.access_token").saveAs("accessToken"),
               jsonPath("$.refresh_token").saveAs("refreshToken"),
               jsonPath("$.expires_in").saveAs("expiresIn"),
               header("Date").saveAs("tokenTime"))
      ).exec{s => 
        s.set("accessTokenRefreshTime", ZonedDateTime.parse(s("tokenTime").as[String], DATE_FMT_RFC1123).toEpochSecond * 1000)
      }
    }

    def logout = exec (refreshTokenIfExpired).exec(
      http("Admin Logout")
      .get("${keycloakServer}/realms/master/protocol/openid-connect/logout")
      .check(status.is(200))
    )
    
  }
  
}