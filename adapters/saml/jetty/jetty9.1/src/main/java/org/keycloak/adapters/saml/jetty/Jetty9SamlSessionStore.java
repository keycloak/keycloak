package org.keycloak.adapters.saml.jetty;

import org.eclipse.jetty.server.Request;
import org.keycloak.adapters.jetty.spi.JettyUserSessionManagement;
import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.adapters.spi.AdapterSessionStore;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.spi.SessionIdMapper;

import javax.servlet.http.HttpSession;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class Jetty9SamlSessionStore extends JettySamlSessionStore {
    public Jetty9SamlSessionStore(Request request, AdapterSessionStore sessionStore, HttpFacade facade, SessionIdMapper idMapper, JettyUserSessionManagement sessionManagement, SamlDeployment deployment) {
        super(request, sessionStore, facade, idMapper, sessionManagement, deployment);
    }

    @Override
    protected String changeSessionId(HttpSession session) {
        Request request = this.request;
        if (!deployment.turnOffChangeSessionIdOnLogin()) return request.changeSessionId();
        else return session.getId();
    }
}
