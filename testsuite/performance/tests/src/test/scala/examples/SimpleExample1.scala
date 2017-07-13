package examples

import io.gatling.core.Predef._
import io.gatling.http.Predef._

/**
  * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
  */
class SimpleExample1 extends Simulation {

  // Create a scenario with three steps:
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

  // Run the scenario with 100 parallel users, all starting at the same time
  setUp(scn.inject(atOnceUsers(100)))
}
