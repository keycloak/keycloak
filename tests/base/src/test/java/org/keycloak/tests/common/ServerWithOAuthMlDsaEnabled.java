package org.keycloak.tests.common;

import org.keycloak.common.Profile;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

public final class ServerWithOAuthMlDsaEnabled implements KeycloakServerConfig {

    @Override
    public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
        return config.features(Profile.Feature.OAUTH_ML_DSA);
    }
}
