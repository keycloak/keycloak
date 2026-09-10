package org.keycloak.tests.scim.tck;


import jakarta.ws.rs.core.Response.Status;

import org.keycloak.scim.client.ScimClient;
import org.keycloak.scim.client.ScimClientException;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.scim.client.annotations.InjectScimClient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest(config = ScimServerConfig.class)
public class RealmConfigTest extends AbstractScimTest {

    @InjectRealm
    ManagedRealm realm;

    @InjectScimClient
    ScimClient client;

    @Test
    public void testFeatureDisabled() {
        try {
            client.config().get();
            fail("Expected exception when retrieving service provider config");
        } catch (ScimClientException e) {
            assertEquals(Status.NOT_FOUND.getStatusCode(), e.getError().getStatusInt());
        }
    }
}
