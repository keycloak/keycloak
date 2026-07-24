package org.keycloak.testframework.tests;

import org.keycloak.models.KeycloakSession;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.annotations.TestOnServer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class RunTestOnServerTest {

    @InjectRealm
    ManagedRealm realm;

    @TestOnServer
    public void test(KeycloakSession session) throws Throwable {
        Assertions.assertNotNull(session);
        Assertions.assertNull(realm);
    }

    @Test
    public void test2() {
        Assertions.assertNotNull(realm);
    }

}
