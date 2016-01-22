package org.keycloak.adapters.tomcat;

import org.apache.catalina.connector.Request;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.KeycloakDeployment;

import javax.servlet.http.HttpSession;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class Tomcat8RequestAuthenticator extends CatalinaRequestAuthenticator {
    public Tomcat8RequestAuthenticator(KeycloakDeployment deployment, AdapterTokenStore tokenStore, CatalinaHttpFacade facade, Request request, GenericPrincipalFactory principalFactory) {
        super(deployment, tokenStore, facade, request, principalFactory);
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
