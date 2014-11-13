package org.keycloak.adapters.tomcat;

import org.apache.catalina.connector.Request;

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
    public boolean authenticate(Request request, HttpServletResponse response) throws IOException {
        return authenticateInternal(request, response);
    }

    @Override
    public void logout(Request request) {
        logoutInternal(request);
        try {
            super.logout(request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
