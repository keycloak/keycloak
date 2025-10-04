package org.keycloak.protocol.oidc.spi;

import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.provider.Provider;

/** A pluggable OIDC TokenManager. Implementations should extend TokenManager. */
public interface TokenManagerProvider extends Provider {
    /** Return a TokenManager instance to handle OIDC token building. */
    TokenManager get();
    @Override default void close() {}
}
