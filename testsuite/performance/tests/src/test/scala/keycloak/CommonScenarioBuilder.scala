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
  * @author <a href="mailto:tkyjovsk@redhat.com">Tomas Kyjovsky</a>
  */
object CommonScenarioBuilder {

  def rampDownNotStarted(): Validation[Boolean] = {
    System.currentTimeMillis < TestConfig.measurementEndTime
  }

}

