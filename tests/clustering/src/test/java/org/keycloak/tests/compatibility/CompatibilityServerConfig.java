package org.keycloak.tests.compatibility;

import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

/**
 * Configuration shared by mixed-version compatibility tests.
 */
public class CompatibilityServerConfig implements KeycloakServerConfig {

    @Override
    public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
        // Older nodes issue admin tokens without the kc-admin-api scope and audience. The compatibility tests use the
        // Admin API to arrange and verify their scenarios, but do not test Admin API token compatibility.
        // This was due to fix CVE-2026-19688.
        return config.option("spi-admin--audience-check-enabled", "false");
    }
}
