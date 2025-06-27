package org.keycloak.broker.oidc;

import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;

public class CustomOIDCIdentityProviderFactory extends OIDCIdentityProviderFactory {
    public static final String PROVIDER_ID = "custom-oidc";

    @Override
    public String getName() {
        return "Custom OIDC";
    }

    @Override
    public CustomOIDCIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new CustomOIDCIdentityProvider(session, new OIDCIdentityProviderConfig(model));
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
