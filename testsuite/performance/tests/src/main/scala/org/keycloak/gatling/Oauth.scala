package org.keycloak.gatling

import io.gatling.core.session._
import org.keycloak.adapters.spi.HttpFacade.Cookie

/**
  * @author Radim Vansa &lt;rvansa@redhat.com&gt;
  */
case class Oauth(requestName: Expression[String]) {
  def authorize(uri: Expression[String], cookies: Expression[List[Cookie]]) = new AuthorizeActionBuilder(new AuthorizeAttributes(requestName, uri, cookies));
  def refresh() = new RefreshTokenActionBuilder(requestName);
}
