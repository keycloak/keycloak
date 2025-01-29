package org.keycloak.tests.infinispan;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Objects;

@KeycloakIntegrationTest(config = InfinispanXMLBackwardCompatibilityTest.ServerConfigWithMetrics.class)
public class InfinispanXMLBackwardCompatibilityTest {

    private static final String CONFIG_FILE = "infinispan-xml-kc26.xml";

    @InjectRealm
    ManagedRealm realm;

    @Test
    void testValidAndInvalidPasswordValidation() {
        RealmRepresentation representation = realm.admin().toRepresentation();
        Assertions.assertNotNull(representation);
    }


    public static class ServerConfigWithMetrics implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            try {
                return config
                        .configFile(Paths.get(Objects.requireNonNull(getClass().getResource("/embedded-infinispan-config/" + CONFIG_FILE)).toURI()))
                        .option("cache-config-file", CONFIG_FILE);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
