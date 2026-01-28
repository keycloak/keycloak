package org.keycloak.test.examples;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.testframework.admin.AdminClientBuilder;
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class AdminClientFactoryTest {

    @InjectRealm(config = RealmSpecificAdminClientTest.RealmWithClientAndUser.class)
    ManagedRealm realm;

    @InjectAdminClientFactory(lifecycle = LifeCycle.METHOD)
    AdminClientFactory adminClientFactory;

    static Keycloak AUTO_CLOSE_INSTANCE;

    @AfterAll
    public static void checkClosed() {
        Assertions.assertThrows(IllegalStateException.class, () -> AUTO_CLOSE_INSTANCE.realms().findAll());
    }

    @Test
    public void testAdminClientFactory() {
        try (Keycloak keycloak = createBuilder().build()) {
            Assertions.assertNotNull(keycloak.realm(realm.getName()).toRepresentation());
        }
        AUTO_CLOSE_INSTANCE = createBuilder().autoClose().build();
    }

    private AdminClientBuilder createBuilder() {
        return adminClientFactory.create()
                .realm(realm.getName())
                .clientId("myclient")
                .clientSecret("mysecret")
                .username("myadmin")
                .password("mypassword");
    }

}
