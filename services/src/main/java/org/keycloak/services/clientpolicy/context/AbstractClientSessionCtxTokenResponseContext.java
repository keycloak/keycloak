package org.keycloak.services.clientpolicy.context;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.protocol.oidc.TokenManager;

/**
 *
 * @author rmartinc
 */
public abstract class AbstractClientSessionCtxTokenResponseContext extends AbstractTokenResponseContext {

    private final ClientSessionContext clientSessionCtx;

    public AbstractClientSessionCtxTokenResponseContext(ClientSessionContext clientSessionCtx,
            TokenManager.AccessTokenResponseBuilder accessTokenResponseBuilder) {
        super(accessTokenResponseBuilder);
        this.clientSessionCtx = clientSessionCtx;
    }

    @Override
    public AuthenticatedClientSessionModel getClientSession() {
        return clientSessionCtx.getClientSession();
    }

    public ClientSessionContext getClientSessionContext() {
        return clientSessionCtx;
    }
}
