package org.keycloak.protocol.oidc.grants.jwtauthorization.validator;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.protocol.oidc.JWTAuthorizationGrantValidator;
import org.keycloak.protocol.oidc.JWTAuthorizationGrantValidatorFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

public class IDJWTAuthorizationGrantValidatorFactory implements JWTAuthorizationGrantValidatorFactory, EnvironmentDependentProviderFactory {

    @Override
    public String getId() {
        return "oauth-id-jag+jwt";
    }

    @Override
    public String getShortcut() {
        return "idjag";
    }

    @Override
    public JWTAuthorizationGrantValidator create(KeycloakSession session) {
        return new IDJWTAuthorizationGrantValidator(session);
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.IDENTITY_ASSERTION_JWT_VALIDATOR);
    }
}
