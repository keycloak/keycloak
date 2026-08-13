package org.keycloak.testframework.tests;

import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestCleanup;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class SetupTest {

    @InjectRealm(lifecycle = LifeCycle.CLASS)
    ManagedRealm realm;

    @TestSetup
    public void setupRealms() {
        RealmRepresentation rep = realm.admin().toRepresentation();
        Assertions.assertNull(rep.getAttributes().get("test.setup"));
        rep.getAttributes().put("test.setup", "myvalue");
        realm.admin().update(rep);
    }

    @TestCleanup
    public void cleanupRealms() {
        RealmRepresentation rep = realm.admin().toRepresentation();
        Assertions.assertEquals("myvalue", rep.getAttributes().get("test.setup"));
        rep.getAttributes().remove("test.setup");
        realm.admin().update(rep);
    }

    @BeforeEach
    public void beforeEach() {
        verifyState();
    }

    @AfterEach
    public void afterEach() {
        verifyState();
    }

    @Test
    public void someTest() {
        verifyState();
    }

    @Test
    public void anotherTest() {
        verifyState();
    }

    private void verifyState() {
        Assertions.assertEquals("myvalue", realm.admin().toRepresentation().getAttributes().get("test.setup"));
    }

}
