package org.keycloak.models.resource;

import org.keycloak.provider.Provider;

public interface KeycloakResourceServices extends Provider {

    ClientService clients();
}
