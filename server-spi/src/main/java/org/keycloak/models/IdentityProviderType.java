package org.keycloak.models;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static org.keycloak.models.IdentityProviderCapability.USER_LINKING;

public enum IdentityProviderType {

    ANY,
    USER_AUTHENTICATION(USER_LINKING),
    CLIENT_ASSERTION,
    EXCHANGE_EXTERNAL_TOKEN(USER_LINKING),
    JWT_AUTHORIZATION_GRANT(USER_LINKING);

    private final Set<IdentityProviderCapability> capabilities;

    IdentityProviderType(IdentityProviderCapability... capabilities) {
        if (capabilities == null || capabilities.length == 0) {
            this.capabilities = Collections.emptySet();
        } else {
            this.capabilities = Arrays.stream(capabilities).collect(Collectors.toSet());
        }
    }

    public Set<IdentityProviderCapability> getCapabilities() {
        return capabilities;
    }

}
