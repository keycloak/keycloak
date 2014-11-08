package org.keycloak.adapters.jetty;

import org.eclipse.jetty.server.Request;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.KeycloakDeployment;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakJettyAuthenticator extends AbstractKeycloakJettyAuthenticator {

    public KeycloakJettyAuthenticator() {
        super();
    }


    @Override
    protected AbstractJettyRequestAuthenticator createRequestAuthenticator(Request request, JettyHttpFacade facade, KeycloakDeployment deployment, AdapterTokenStore tokenStore) {
        return new JettyRequestAuthenticator(deployment, this, tokenStore, facade, request);
    }


}
