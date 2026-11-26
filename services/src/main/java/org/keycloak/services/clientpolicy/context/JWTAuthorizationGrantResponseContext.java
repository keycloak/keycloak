package org.keycloak.services.clientpolicy.context;

import jakarta.ws.rs.core.MultivaluedMap;

import org.keycloak.models.ClientSessionContext;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;

/**
 *
 * @author rmartinc
 */
public class JWTAuthorizationGrantResponseContext extends AbstractClientSessionCtxTokenResponseContext {

    private final MultivaluedMap<String, String> params;

    public JWTAuthorizationGrantResponseContext(MultivaluedMap<String, String> params,
            ClientSessionContext clientSessionCtx,
            TokenManager.AccessTokenResponseBuilder accessTokenResponseBuilder) {
        super(clientSessionCtx, accessTokenResponseBuilder);
        this.params = params;
    }

    public MultivaluedMap<String, String> getParams() {
        return params;
    }

    @Override
    public ClientPolicyEvent getEvent() {
        return ClientPolicyEvent.JWT_AUTHORIZATION_GRANT_RESPONSE;
    }
}
