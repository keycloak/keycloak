package org.keycloak.test.examples;

import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class CleanupTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @Test
    public void dirty() {
        checkCleanState();

        managedRealm.dirty();

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setEnabled(true);
        userRepresentation.setUsername("foobar");
        managedRealm.admin().users().create(userRepresentation);
    }

    @Test
    public void cleanupTask() {
        checkCleanState();

        managedRealm.cleanup().add(r -> r.users().list().forEach(u -> r.users().delete(u.getId()).close()));

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setEnabled(true);
        userRepresentation.setUsername("foobar");
        managedRealm.admin().users().create(userRepresentation).close();
    }

    @Test
    public void cleanupTaskReusableTasks() {
        checkCleanState();

        managedRealm.cleanup().deleteUsers();

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setEnabled(true);
        userRepresentation.setUsername("foobar");
        managedRealm.admin().users().create(userRepresentation).close();
    }

    @Test
    public void test() {
        checkCleanState();

        managedRealm.updateWithCleanup(r -> r.registrationEmailAsUsername(true));

        Assertions.assertTrue(managedRealm.admin().toRepresentation().isRegistrationEmailAsUsername());
    }

    @Test
    public void test2() {
        checkCleanState();

        managedRealm.updateWithCleanup(r -> r.registrationEmailAsUsername(true));

        Assertions.assertTrue(managedRealm.admin().toRepresentation().isRegistrationEmailAsUsername());
    }

    private void checkCleanState() {
        Assertions.assertTrue(managedRealm.admin().users().list().isEmpty());
        Assertions.assertFalse(managedRealm.admin().toRepresentation().isRegistrationEmailAsUsername());
    }

}
