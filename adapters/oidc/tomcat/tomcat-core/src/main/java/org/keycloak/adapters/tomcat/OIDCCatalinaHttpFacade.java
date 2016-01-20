package org.keycloak.adapters.tomcat;

import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.OIDCHttpFacade;

import javax.servlet.http.HttpServletResponse;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OIDCCatalinaHttpFacade extends CatalinaHttpFacade implements OIDCHttpFacade{

    public OIDCCatalinaHttpFacade(org.apache.catalina.connector.Request request, HttpServletResponse response) {
        super(response, request);
    }

    @Override
    public KeycloakSecurityContext getSecurityContext() {
        return (KeycloakSecurityContext)request.getAttribute(KeycloakSecurityContext.class.getName());
    }

}
