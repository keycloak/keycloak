package org.keycloak.models.mapper;

import org.keycloak.provider.Provider;

public interface ModelMapper extends Provider {

    ClientModelMapper clients();

    default void close() {
    }
}
