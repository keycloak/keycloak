package org.keycloak.admin.api.mapper;

import org.keycloak.provider.Provider;

public interface ApiModelMapper extends Provider {

    ApiClientMapper clients();

    default void close() {
    }
}
