package org.keycloak.adapters.tomcat;

import org.apache.catalina.connector.Request;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.realm.GenericPrincipal;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.List;

/**
 * Keycloak authentication valve
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakAuthenticatorValve extends AbstractKeycloakAuthenticatorValve {
    public boolean authenticate(Request request, HttpServletResponse response) throws IOException {
        return authenticateInternal(request, response);
    }

    protected void initInternal() {
        StandardContext standardContext = (StandardContext) context;
        standardContext.addLifecycleListener(this);
    }

    public void logout(Request request) {
        logoutInternal(request);
    }

    @Override
    protected GenericPrincipalFactory createPrincipalFactory() {
        return new GenericPrincipalFactory() {
            @Override
            protected GenericPrincipal createPrincipal(Principal userPrincipal, List<String> roles) {
                return new GenericPrincipal(userPrincipal.getName(), null, roles, userPrincipal, null);
            }
        };
    }
}
