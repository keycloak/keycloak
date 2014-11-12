package org.keycloak.adapters.tomcat;

import org.apache.catalina.connector.Request;
import org.apache.catalina.deploy.LoginConfig;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Keycloak authentication valve
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakAuthenticatorValve extends AbstractKeycloakAuthenticatorValve {
    @Override
    public boolean authenticate(Request request, HttpServletResponse response, LoginConfig config) throws IOException {
        return authenticateInternal(request, response);
    }

    @Override
    public void logout(Request request) throws ServletException {
        logoutInternal(request);
        super.logout(request);
    }
}
