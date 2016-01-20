package org.keycloak.adapters.saml.wildfly;

import io.undertow.security.api.SecurityContext;
import io.undertow.server.HttpServerExchange;
import org.keycloak.adapters.spi.SessionIdMapper;
import org.keycloak.adapters.saml.SamlSession;
import org.keycloak.adapters.saml.undertow.ServletSamlSessionStore;
import org.keycloak.adapters.undertow.UndertowUserSessionManagement;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class WildflySamlSessionStore extends ServletSamlSessionStore {
    public WildflySamlSessionStore(HttpServerExchange exchange, UndertowUserSessionManagement sessionManagement,
                                   SecurityContext securityContext, SessionIdMapper idMapper) {
        super(exchange, sessionManagement, securityContext, idMapper);
    }

    @Override
    public boolean isLoggedIn() {
        if (super.isLoggedIn()) {
            SecurityInfoHelper.propagateSessionInfo(getAccount());
            return true;
        }
        return false;
    }

    @Override
    public void saveAccount(SamlSession account) {
        super.saveAccount(account);
        SecurityInfoHelper.propagateSessionInfo(account);
    }


}
