package org.keycloak.gatling;

import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.OidcKeycloakAccount;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.RequestAuthenticator;

/**
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class MockTokenStore implements AdapterTokenStore {
   @Override
   public void checkCurrentToken() {
         }

   @Override
   public boolean isCached(RequestAuthenticator authenticator) {
      return false;
   }

   @Override
   public void saveAccountInfo(OidcKeycloakAccount account) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void logout() {
   }

   @Override
   public void refreshCallback(RefreshableKeycloakSecurityContext securityContext) {
   }

   @Override
   public void saveRequest() {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean restoreRequest() {
      throw new UnsupportedOperationException();
   }
}
