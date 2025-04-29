package org.keycloak.services;

import org.keycloak.provider.Provider;
import org.keycloak.services.client.ClientService;

public interface KeycloakServices extends Provider {

    ClientService clients();
}
