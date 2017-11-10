package examples

import io.gatling.core.Predef._
import io.gatling.http.Predef._

/**
  * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
  */
class SimpleExample3 extends Simulation {

  // Create a scenario where user performs the same operation in a loop without any pause
  // Each loop iteration will be displayed as individual request in the report
  val rapidlyRefreshAccount = repeat(10, "i") {
    exec(http("Account ${i}")
      .get("http://localhost:8080/auth/realms/master/account")
      .check(status is 200))
  }

  val scn = scenario("Account Refresh")
    .exec(rapidlyRefreshAccount)

  setUp(
    scn.inject(atOnceUsers(100))
  )
}
