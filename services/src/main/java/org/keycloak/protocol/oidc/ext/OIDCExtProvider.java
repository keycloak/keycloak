package org.keycloak.protocol.oidc.ext;

import org.keycloak.events.EventBuilder;
import org.keycloak.provider.Provider;

public interface OIDCExtProvider extends Provider {

    void setEvent(EventBuilder event);

    @Override
    default void close() {
    }

}
