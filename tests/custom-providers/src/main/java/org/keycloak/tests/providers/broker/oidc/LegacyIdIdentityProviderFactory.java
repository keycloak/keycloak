package org.keycloak.tests.providers.broker.oidc;

import org.keycloak.broker.oidc.KeycloakOIDCIdentityProvider;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;

public class LegacyIdIdentityProviderFactory extends OIDCIdentityProviderFactory {

    public static final String PROVIDER_ID = "legacy-id-idp";

    @Override
    public String getName() {
        return PROVIDER_ID;
    }

    @Override
    public KeycloakOIDCIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new LegacyIdIdentityProvider(session, new OIDCIdentityProviderConfig(model));
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
