package org.keycloak.test.examples;



import org.keycloak.admin.client.Keycloak;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class AdminClientTest {

    @InjectRealm(ref = "customrealm", config = CustomRealmConf.class)
    ManagedRealm customRealm;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM, ref = "customclient", realmRef = "customrealm", client = "realmclient", user = "realmuser")
    Keycloak customAdminClient;

    @InjectEvents(ref = "customevents", realmRef = "customrealm")
    Events customRealmEvents;

    @Test
    public void testDefaultAdminClient() {
        Assertions.assertFalse(adminClient.tokenManager().getAccessToken().getToken().isEmpty());
    }

    @Test
    public void testAdminClientWithNoAccessAsUser() {
        Assertions.assertFalse(customAdminClient.tokenManager().getAccessToken().getToken().isEmpty());
        EventAssertion.assertSuccess(customRealmEvents.poll()).clientId("realmclient").details("username", "realmuser");
    }

    private static class CustomRealmConf implements RealmConfig {
        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.addUser("realmuser")
                    .password("realmuser")
                    .name("Realm", "User").email("realm@user").emailVerified(true)
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN);
            realm.addClient("realmclient")
                    .secret("realmclientsecret").directAccessGrantsEnabled(true);
            return realm;
        }
    }
}
