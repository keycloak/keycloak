package org.keycloak.gatling

import io.gatling.core.session.Expression

/**
  * @author Radim Vansa &lt;rvansa@redhat.com&gt;
  */
object Predef {
  def oauth(requestName: Expression[String]) = new Oauth(requestName)
}
