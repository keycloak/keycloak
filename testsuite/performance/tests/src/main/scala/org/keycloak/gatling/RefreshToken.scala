package org.keycloak.gatling

import akka.actor.ActorDSL._
import akka.actor.ActorRef
import io.gatling.core.action.Interruptable
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.config.Protocols
import io.gatling.core.result.writer.DataWriterClient
import io.gatling.core.session.{Expression, Session}
import io.gatling.core.validation.Validation

/**
  * @author Radim Vansa &lt;rvansa@redhat.com&gt;
  */
class RefreshTokenActionBuilder(requestName: Expression[String]) extends ActionBuilder{
  override def build(next: ActorRef, protocols: Protocols): ActorRef = {
    actor(actorName("refresh-token"))(new RefreshTokenAction(requestName, next))
  }
}

class RefreshTokenAction(
                          requestName: Expression[String],
                          val next: ActorRef
                        ) extends Interruptable with ExitOnFailure with DataWriterClient {
  override def executeOrFail(session: Session): Validation[_] = {
    val requestAuth: MockRequestAuthenticator = session(MockRequestAuthenticator.KEY).as[MockRequestAuthenticator]
    Blocking(() =>
      Stopwatch(() => requestAuth.getKeycloakSecurityContext.refreshExpiredToken(false))
        .check(identity, _ => "AuthorizeAction: refreshToken() failed")
        .recordAndContinue(this, session, requestName(session).get)
    )
  }
}
