package examples

import io.gatling.core.Predef._
import io.gatling.http.Predef._

/**
  * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
  */
class SimpleExample2 extends Simulation {

  // Create two scenarios
  // First one called Simple with three steps:
  //   - first perform an HTTP GET
  //   - then pause for 10 seconds
  //   - then perform a different HTTP GET

  val scn = scenario("Simple")
    .exec(http("Home")
      .get("http://localhost:8080")
      .check(status is 200))
    .pause(10)
    .exec(http("Auth Home")
      .get("http://localhost:8080/auth")
      .check(status is 200))


  // The second scenario called Account with only one step:
  //   - perform an HTTP GET

  val scn2 = scenario("Account")
    .exec(http("Account")
      .get("http://localhost:8080/auth/realms/master/account")
      .check(status is 200))

  // Run both scenarios:
  //   - first scenario with 100 parallel users, starting all at the same time
  //   - second scenario with 50 parallel users, starting all at the same time

  setUp(
    scn.inject(atOnceUsers(100)),
    scn2.inject(atOnceUsers(50))
  )
}
