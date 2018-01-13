package org.keycloak.gatling;

import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OAuthRequestAuthenticator;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.RequestAuthenticator;
import org.keycloak.adapters.spi.HttpFacade;

/**
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class MockRequestAuthenticator extends RequestAuthenticator {
   public static String KEY = MockRequestAuthenticator.class.getName();

   private RefreshableKeycloakSecurityContext keycloakSecurityContext;
   // This is application-specific user session, used for backchannel operations
   private final String sessionId;

   public MockRequestAuthenticator(HttpFacade facade, KeycloakDeployment deployment, AdapterTokenStore tokenStore, int sslRedirectPort, String sessionId) {
      super(facade, deployment, tokenStore, sslRedirectPort);
      this.sessionId = sessionId;
   }

   @Override
   protected OAuthRequestAuthenticator createOAuthAuthenticator() {
      return new OAuthRequestAuthenticator(this, facade, deployment, sslRedirectPort, tokenStore);
   }

   @Override
   protected void completeOAuthAuthentication(KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal) {
      keycloakSecurityContext = principal.getKeycloakSecurityContext();
   }

   @Override
   protected void completeBearerAuthentication(KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal, String method) {
      throw new UnsupportedOperationException();
   }

   @Override
   protected String changeHttpSessionId(boolean create) {
      return sessionId;
   }

   public RefreshableKeycloakSecurityContext getKeycloakSecurityContext() {
      return keycloakSecurityContext;
   }
}
