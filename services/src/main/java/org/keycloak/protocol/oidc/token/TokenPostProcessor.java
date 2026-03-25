package org.keycloak.protocol.oidc.token;

import org.keycloak.provider.Provider;

public interface TokenPostProcessor extends Provider {

    void process(TokenPostProcessorContext context);

    default void close() {}
}
