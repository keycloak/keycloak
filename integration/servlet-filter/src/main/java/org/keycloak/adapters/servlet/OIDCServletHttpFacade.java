package org.keycloak.adapters.servlet;

import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.OIDCHttpFacade;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OIDCServletHttpFacade extends ServletHttpFacade implements OIDCHttpFacade {

    public OIDCServletHttpFacade(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    @Override
    public KeycloakSecurityContext getSecurityContext() {
        return (KeycloakSecurityContext)request.getAttribute(KeycloakSecurityContext.class.getName());
    }
}
