package org.keycloak.tests.cluster;

import org.keycloak.common.Profile;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

/**
 * Failover tests exercise Infinispan owner-count behavior for in-memory sessions.
 * Persistent user sessions would reload state from the database after a node loss and
 * hide owner-count regressions, so disable that feature for these tests.
 */
public class ClusterFailoverServerConfig implements KeycloakServerConfig {
    @Override
    public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
        return config.featuresDisabled(Profile.Feature.PERSISTENT_USER_SESSIONS);
    }
}
