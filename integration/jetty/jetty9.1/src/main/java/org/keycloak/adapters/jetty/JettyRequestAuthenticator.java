package org.keycloak.adapters.jetty;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.MultiMap;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.KeycloakDeployment;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JettyRequestAuthenticator extends AbstractJettyRequestAuthenticator {

    public JettyRequestAuthenticator(KeycloakDeployment deployment,
                                     AbstractKeycloakJettyAuthenticator valve, AdapterTokenStore tokenStore,
                                     JettyHttpFacade facade,
                                     Request request) {
        super(facade, deployment, tokenStore, -1, valve, request);
    }


    @Override
    protected MultiMap<String> extractFormParameters(Request base_request) {
        MultiMap<String> formParameters = new MultiMap<String>();
        base_request.extractParameters();
        return base_request.getParameters();
    }
    @Override
    protected void restoreFormParameters(MultiMap<String> j_post, Request base_request) {
        base_request.setParameters(j_post);
    }
}
