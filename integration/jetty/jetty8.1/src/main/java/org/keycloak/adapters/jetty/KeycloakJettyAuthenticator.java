package org.keycloak.adapters.jetty;

import org.eclipse.jetty.server.AbstractHttpConnection;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.UserIdentity;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.jetty.core.AbstractKeycloakJettyAuthenticator;

import javax.servlet.ServletRequest;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakJettyAuthenticator extends AbstractKeycloakJettyAuthenticator {

    public KeycloakJettyAuthenticator() {
        super();
    }


    @Override
    public AdapterTokenStore createSessionTokenStore(Request request, KeycloakDeployment resolvedDeployment) {
        return new JettySessionTokenStore(request, resolvedDeployment);
    }

    @Override
    protected Request resolveRequest(ServletRequest req) {
        return (req instanceof Request)?(Request)req: AbstractHttpConnection.getCurrentConnection().getRequest();
    }

    @Override
    protected Authentication createAuthentication(UserIdentity userIdentity) {
        return new KeycloakAuthentication(getAuthMethod(), userIdentity) {
            @Override
            public void logout() {
                logoutCurrent(AbstractHttpConnection.getCurrentConnection().getRequest());
            }
        };
    }


}
