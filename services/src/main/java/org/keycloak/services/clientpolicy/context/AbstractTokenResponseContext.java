package org.keycloak.services.clientpolicy.context;

import org.keycloak.protocol.oidc.TokenManager;

/**
 *
 * @author rmartinc
 */
public abstract class AbstractTokenResponseContext implements ClientPolicyClientSessionContext {

    private final TokenManager.AccessTokenResponseBuilder accessTokenResponseBuilder;

    public AbstractTokenResponseContext(TokenManager.AccessTokenResponseBuilder accessTokenResponseBuilder) {
        this.accessTokenResponseBuilder = accessTokenResponseBuilder;
    }

    public TokenManager.AccessTokenResponseBuilder getAccessTokenResponseBuilder() {
        return accessTokenResponseBuilder;
    }
}
