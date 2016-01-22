package org.keycloak.adapters.jetty;

import org.eclipse.jetty.server.Request;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.jetty.core.JettyRequestAuthenticator;
import org.keycloak.adapters.spi.HttpFacade;

import javax.servlet.http.HttpSession;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class Jetty91RequestAuthenticator extends JettyRequestAuthenticator {
    public Jetty91RequestAuthenticator(HttpFacade facade, KeycloakDeployment deployment, AdapterTokenStore tokenStore, int sslRedirectPort, Request request) {
        super(facade, deployment, tokenStore, sslRedirectPort, request);
    }

    @Override
    protected String changeHttpSessionId(boolean create) {
        Request request = this.request;
        HttpSession session = request.getSession(false);
        if (session == null) {
            return request.getSession(true).getId();
        }
        if (deployment.isTurnOffChangeSessionIdOnLogin() == false) return request.changeSessionId();
        else return session.getId();
    }
}
