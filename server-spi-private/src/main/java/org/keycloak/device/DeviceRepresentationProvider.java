package org.keycloak.device;

import org.keycloak.provider.Provider;
import org.keycloak.representations.account.DeviceRepresentation;

public interface DeviceRepresentationProvider extends Provider {

    DeviceRepresentation deviceRepresentation();

    @Override
    default void close() {
    }
}
