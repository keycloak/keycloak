package org.keycloak.social.orcid;

import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.broker.social.SocialIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;

public class OrcidIdentityProviderFactory extends AbstractIdentityProviderFactory<OrcidIdentityProvider> implements SocialIdentityProviderFactory<OrcidIdentityProvider> {

    public static final String PROVIDER_ID = "orcid";

    @Override
    public String getName() {
        return "ORCID";
    }

    @Override
    public OrcidIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new OrcidIdentityProvider(session, new OrcidIdentityProviderConfig(model));
    }

    @Override
    public OrcidIdentityProviderConfig createConfig() {
        return new OrcidIdentityProviderConfig();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}