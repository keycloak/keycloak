package org.keycloak.tests.broker;

import java.net.URL;

import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

public class BrokerVaultServerConfig implements KeycloakServerConfig {

    @Override
    public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder builder) {
        URL vaultDir = BrokerVaultServerConfig.class.getResource("vault");
        if (vaultDir == null) {
            throw new IllegalStateException("Unable to find broker vault test resources");
        }

        return builder
                .dependency("org.keycloak.tests", "keycloak-tests-custom-providers")
                .option("vault", "file")
                .option("vault-dir", vaultDir.getPath());
    }
}
