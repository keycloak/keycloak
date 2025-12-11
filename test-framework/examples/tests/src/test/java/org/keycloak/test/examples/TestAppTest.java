package org.keycloak.test.examples;

import org.keycloak.representations.adapters.action.PushNotBeforeAction;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.TestApp;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectTestApp;
import org.keycloak.testframework.realm.ManagedRealm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class TestAppTest {

    @InjectOAuthClient(kcAdmin = true)
    OAuthClient oauth;

    @InjectTestApp
    TestApp testApp;

    @InjectRealm
    ManagedRealm managedRealm;

    @Test
    public void testPushNotBefore() throws InterruptedException {
        String clientUuid = managedRealm.admin().clients().findByClientId("test-app").stream().findFirst().get().getId();
        managedRealm.admin().clients().get(clientUuid).pushRevocation();

        PushNotBeforeAction adminPushNotBefore = testApp.kcAdmin().getAdminPushNotBefore();
        Assertions.assertNotNull(adminPushNotBefore);
    }

}
