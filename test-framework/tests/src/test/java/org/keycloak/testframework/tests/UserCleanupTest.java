package org.keycloak.testframework.tests;

import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.realm.UserConfigBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@KeycloakIntegrationTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserCleanupTest {

    @InjectUser(config = UserCleanupTest.UserCleanupUserConfig.class)
    ManagedUser managedUser;

    @Test
    @Order(1)
    public void markUserDirty() {
        managedUser.updateWithCleanup(u -> u.name("Oliwia", "Keycloakowicz").email("newmail@test.com"));
        managedUser.dirty();

        UserRepresentation userRepresentation = managedUser.admin().toRepresentation();
        Assertions.assertEquals("Oliwia", userRepresentation.getFirstName());
        Assertions.assertEquals("Keycloakowicz", userRepresentation.getLastName());
        Assertions.assertEquals("newmail@test.com", userRepresentation.getEmail());
    }

    @Test
    @Order(2)
    public void verifyUserDirty() {
        UserRepresentation userRepresentation = managedUser.admin().toRepresentation();
        Assertions.assertEquals("Johnny", userRepresentation.getFirstName());
        Assertions.assertEquals("Keycloakey", userRepresentation.getLastName());
        Assertions.assertEquals("testuser@localhost.org", userRepresentation.getEmail());
    }

    @Test
    @Order(3)
    public void updateWithRollback() {
        managedUser.updateWithCleanup(r -> r.enabled(false));
    }

    @Test
    @Order(4)
    public void verifyUpdateWithRollback() {
        Assertions.assertTrue(managedUser.admin().toRepresentation().isEnabled());
    }

    @Test
    @Order(5)
    public void customCleanup() {
        UserRepresentation userRepresentation = managedUser.admin().toRepresentation();
        userRepresentation.setEnabled(false);
        managedUser.cleanup().add(u -> u.update(userRepresentation));
    }

    @Test
    @Order(6)
    public void verifyCustomCleanup() {
        UserRepresentation userRepresentation = managedUser.admin().toRepresentation();
        Assertions.assertFalse(userRepresentation.isEnabled());
    }

    static class UserCleanupUserConfig implements UserConfig {

        @Override
        public UserConfigBuilder configure(UserConfigBuilder user) {
            return user.username("testUser")
                    .name("Johnny", "Keycloakey")
                    .password("password")
                    .email("testuser@localhost.org")
                    .emailVerified(true);
        }
    }

}
