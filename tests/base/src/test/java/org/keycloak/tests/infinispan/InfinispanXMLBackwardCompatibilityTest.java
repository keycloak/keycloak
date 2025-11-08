package org.keycloak.tests.infinispan;

import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest(config = InfinispanXMLBackwardCompatibilityTest.ServerConfigWithCustomInfinispanXML.class)
public class InfinispanXMLBackwardCompatibilityTest {

    private static final String CONFIG_FILE = "/embedded-infinispan-config/infinispan-xml-kc26.xml";

    @InjectRealm
    ManagedRealm realm;

    @Test
    void testKeycloakStartedSuccessfullyWithOlderInfinispanXML() {
        RealmRepresentation representation = realm.admin().toRepresentation();
        Assertions.assertNotNull(representation);
    }


    public static class ServerConfigWithCustomInfinispanXML implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.cacheConfigFile(CONFIG_FILE);
        }
    }
}
