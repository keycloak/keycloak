package org.keycloak.test.examples;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.adapters.action.PushNotBeforeAction;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.TestApp;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectTestApp;
import org.keycloak.testframework.realm.ManagedRealm;

import java.util.Optional;
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
        String clientUuid = Optional.ofNullable(managedRealm.admin().clients().findClientByClientId("test-app")).orElseThrow().getId();
        managedRealm.admin().clients().get(clientUuid).pushRevocation();

        PushNotBeforeAction adminPushNotBefore = testApp.kcAdmin().getAdminPushNotBefore();
        Assertions.assertNotNull(adminPushNotBefore);
    }

}
