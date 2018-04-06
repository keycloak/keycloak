package examples

import io.gatling.core.Predef._
import io.gatling.http.Predef._

/**
  * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
  */
class SimpleExample4 extends Simulation {

  // Specify defaults for http requests
  val httpConf = http
    .baseURL("http://localhost:8080/auth") // This is the root for all relative URLs
    .acceptHeader("text/html,application/xhtml+xml,application/xml")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

  val account = exec(http("Account")
      .get("/realms/master/account")       // URL is appended to baseURL
      .check(status is 200))

  val scn = scenario("Account")
    .exec(account)

  setUp(
    // rather than starting all 100 users at once, increase the count over a period of 10 seconds
    scn.inject(rampUsers(100) over 10).protocols(httpConf)
  )
}
