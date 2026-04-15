package org.keycloak.testframework.tests;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@KeycloakIntegrationTest
@TestMethodOrder(MethodOrderer.MethodName.class)
public class AttachToTest {

    @InjectRealm(config = AttachToTestRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectRealm(ref = "master", attachTo = "master")
    ManagedRealm attachedRealm;

    @InjectClient
    ManagedClient managedClient;

    @InjectClient(ref = "admin-cli", attachTo = "admin-cli")
    ManagedClient attachedClient;

    @InjectClient(ref = "my-client", attachTo = "my-client")
    ManagedClient myClient;

    @Test
    public void aAttachedRealm() {
        Assertions.assertEquals("master", attachedRealm.getName());
        Assertions.assertEquals("master", attachedRealm.admin().toRepresentation().getRealm());
        attachedRealm.updateWithCleanup(r -> r.editUsernameAllowed(true));
    }

    @Test
    public void bRealmCleanup() {
        Assertions.assertFalse(attachedRealm.admin().toRepresentation().isEditUsernameAllowed());
    }

    @Test
    public void cManagedClient() {
        Assertions.assertEquals("default", adminClient.realm("default").clients().get(managedClient.getId()).toRepresentation().getClientId());
    }

    @Test
    public void dAttachedClient() {
        Assertions.assertEquals("admin-cli", attachedClient.getClientId());
        Assertions.assertEquals("admin-cli", attachedClient.admin().toRepresentation().getClientId());
    }

    @Test
    public void eManagedClient() {
        Assertions.assertEquals("default", managedClient.getClientId());
        Assertions.assertEquals("default", managedClient.admin().toRepresentation().getClientId());
    }
    
    @Test
    @Order(1)
    public void testingUpdateWithCleanupWithAttachToChanged() {
        myClient.updateWithCleanup(c -> c.description("new description"));
        Assertions.assertNotNull(myClient.admin().toRepresentation().getDescription());
    }

    @Test
    @Order(2)
    public void testingUpdateWithCleanupWithAttachToOriginal() {
        Assertions.assertNull(myClient.admin().toRepresentation().getDescription());
    }

    private static class AttachToTestRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder builder) {
            builder.addClient("my-client");

            return builder;
        }
    }
}
