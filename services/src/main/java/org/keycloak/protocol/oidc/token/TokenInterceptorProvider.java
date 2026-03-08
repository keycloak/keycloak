package org.keycloak.protocol.oidc.token;

import org.keycloak.models.ClientSessionContext;
import org.keycloak.provider.Provider;
import org.keycloak.representations.AccessToken;

public interface TokenInterceptorProvider extends Provider {

    AccessToken intercept(AccessToken token, ClientSessionContext clientSessionCtx);

}
