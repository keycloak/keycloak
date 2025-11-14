package org.keycloak.cache;

import java.util.Map;

import org.keycloak.models.ClientModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.Provider;

public interface AlternativeLookupProvider extends Provider {

    IdentityProviderModel lookupIdentityProviderFromIssuer(KeycloakSession session, String issuerUrl);

    ClientModel lookupClientFromClientAttributes(KeycloakSession session, Map<String, String> attributes);

}
