package org.keycloak.protocol.oidc.token;

import org.keycloak.provider.Provider;

public interface TokenInterceptorProvider extends Provider {

    void intercept(TokenInterceptorContext context);

    default void close() {}
}
