package org.keycloak.adapters.jetty.core;

import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.OIDCHttpFacade;
import org.keycloak.adapters.jetty.spi.JettyHttpFacade;

import javax.servlet.http.HttpServletResponse;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OIDCJettyHttpFacade extends JettyHttpFacade implements OIDCHttpFacade {

    public OIDCJettyHttpFacade(org.eclipse.jetty.server.Request request, HttpServletResponse response) {
        super(request, response);
    }

    @Override
    public KeycloakSecurityContext getSecurityContext() {
        return (KeycloakSecurityContext)request.getAttribute(KeycloakSecurityContext.class.getName());
    }

}
