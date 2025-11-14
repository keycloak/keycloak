package org.keycloak.broker.jwtauthorizationgrant;

import java.util.Map;

import org.keycloak.Config;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.common.Profile;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

public class JWTAuthorizationGrantIdentityProviderFactory extends AbstractIdentityProviderFactory<JWTAuthorizationGrantIdentityProvider> implements EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "jwt-authorization-grant";

    @Override
    public String getName() {
        return "JWT Authorization Grant";
    }

    @Override
    public JWTAuthorizationGrantIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new JWTAuthorizationGrantIdentityProvider(session, new JWTAuthorizationGrantIdentityProviderConfig(model));
    }

    @Override
    public Map<String, String> parseConfig(KeycloakSession session, String configString) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IdentityProviderModel createConfig() {
        return new JWTAuthorizationGrantIdentityProviderConfig();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.JWT_AUTHORIZATION_GRANT);
    }

}
