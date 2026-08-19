package org.keycloak.tests.providers.broker.oidc;

import org.keycloak.broker.oidc.KeycloakOIDCIdentityProvider;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.KeycloakSession;

public class LegacyIdIdentityProvider extends KeycloakOIDCIdentityProvider {

    public static final String LEGACY_ID = "3.14159265359";

    public LegacyIdIdentityProvider(KeycloakSession session, OIDCIdentityProviderConfig config) {
        super(session, config);
    }

    @Override
    public BrokeredIdentityContext getFederatedIdentity(String response) {
        BrokeredIdentityContext user = super.getFederatedIdentity(response);
        user.setLegacyId(LEGACY_ID);
        return user;
    }
}
