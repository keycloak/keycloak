package org.keycloak.testframework.tests;

import jakarta.ws.rs.NotFoundException;

import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RoleBuilder;
import org.keycloak.testframework.realm.UserBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@KeycloakIntegrationTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RealmCleanupTest {

    @InjectRealm(config = RealmCleanupRealmConfig.class)
    ManagedRealm managedRealm;

    @Test
    @Order(1)
    public void markRealmDirty() {
        managedRealm.admin().users().create(UserBuilder.create().username("myuser").build()).close();
        managedRealm.dirty();
    }

    @Test
    @Order(2)
    public void verifyRealmDirty() {
        Assertions.assertTrue(managedRealm.admin().users().search("myuser").isEmpty());
    }

    @Test
    @Order(3)
    public void updateWithRollback() {
        managedRealm.updateWithCleanup(r  -> r.registrationAllowed(true));
    }

    @Test
    @Order(4)
    public void verifyUpdateWithRollback() {
        Assertions.assertFalse(managedRealm.admin().toRepresentation().isRegistrationAllowed());
    }

    @Test
    @Order(5)
    public void addUser() {
        managedRealm.addUser(UserBuilder.create().username("myuser"));
    }

    @Test
    @Order(6)
    public void verifyAddUserRollback() {
        Assertions.assertTrue(managedRealm.admin().users().search("myuser").isEmpty());
    }

    @Test
    @Order(7)
    public void updateUser() {
        managedRealm.updateUserWithCleanup("someuser", u -> u.firstName("Bob"));
    }

    @Test
    @Order(8)
    public void verifyUpdateUserRollback() {
        Assertions.assertEquals("Sarah", managedRealm.admin().users().search("someuser").get(0).getFirstName());
    }

    @Test
    @Order(9)
    public void customCleanup() {
        managedRealm.cleanup().add(r -> r.roles().get("foo").remove());
        managedRealm.admin().roles().create(RoleBuilder.create().name("foo").build());
    }

    @Test
    @Order(10)
    public void verifyCustomCleanupRollback() {
        Assertions.assertThrows(NotFoundException.class, () -> managedRealm.admin().roles().get("foo").toRepresentation());
    }

    public static class RealmCleanupRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.users(UserBuilder.create("someuser").firstName("Sarah"));
            return realm;
        }
    }

}
