package org.keycloak.adapters.undertow;

import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.api.ConfidentialPortManager;
import org.keycloak.adapters.RealmConfiguration;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ServletOAuthAuthenticator extends OAuthAuthenticator {
    protected ConfidentialPortManager portManager;

    public ServletOAuthAuthenticator(HttpServerExchange exchange, RealmConfiguration realmInfo, ConfidentialPortManager portManager) {
        super(exchange, realmInfo, -1);
        this.portManager = portManager;
    }

    @Override
    protected int sslRedirectPort() {
        return portManager.getConfidentialPort(exchange);
    }
}
