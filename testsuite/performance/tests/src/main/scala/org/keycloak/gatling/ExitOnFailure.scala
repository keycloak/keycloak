package org.keycloak.gatling

import io.gatling.core.action.{Chainable, UserEnd}
import io.gatling.core.session.Session
import io.gatling.core.validation.Validation

/**
  * @author Radim Vansa &lt;rvansa@redhat.com&gt;
  */
trait ExitOnFailure extends Chainable {
  override def execute(session: Session): Unit = {
    executeOrFail(session).onFailure { message =>
      logger.error(s"'${self.path.name}' failed to execute: $message")
      UserEnd.instance ! session.markAsFailed
    }
  }

  def executeOrFail(session: Session): Validation[_]
}

