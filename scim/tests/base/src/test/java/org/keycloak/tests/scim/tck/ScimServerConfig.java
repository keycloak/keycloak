package org.keycloak.tests.scim.tck;

import org.keycloak.common.Profile.Feature;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

public class ScimServerConfig implements KeycloakServerConfig {

    @Override
    public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
        return config.features(Feature.SCIM_API);
    }
}
