package org.keycloak.social.nia;

import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.broker.social.SocialIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.saml.validators.DestinationValidator;

public class NiaIdentityProviderFactory extends AbstractIdentityProviderFactory<NiaIdentityProvider>
        implements SocialIdentityProviderFactory<NiaIdentityProvider> {

    public static final String NIA_PROVIDER_ID = "saml";
    public static final String NIA_PROVIDER_NAME = "NIA Identity Provider";
    public static final String[] COMPATIBLE_PROVIDER = new String[]{NIA_PROVIDER_ID};
    private DestinationValidator destinationValidator;

    @Override
    public String getName() {
        return NIA_PROVIDER_NAME;
    }

    @Override
    public String getId() {
        return NIA_PROVIDER_ID;
    }

    @Override
    public NiaIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new NiaIdentityProvider(session, new NiaIdentityProviderConfig(model),
                destinationValidator);
    }

    @Override
    public NiaIdentityProviderConfig createConfig() {
        return new NiaIdentityProviderConfig();

    }
}
