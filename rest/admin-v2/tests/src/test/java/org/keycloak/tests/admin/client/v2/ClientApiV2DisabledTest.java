package org.keycloak.tests.admin.client.v2;

import jakarta.ws.rs.NotFoundException;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;

import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
@KeycloakIntegrationTest
public class ClientApiV2DisabledTest extends AbstractClientApiV2Test {
    @InjectHttpClient
    CloseableHttpClient client;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectRealm(attachTo = "master", ref = "master")
    ManagedRealm masterRealm;

    @Override
    protected Keycloak getAdminClient() {
        return this.adminClient;
    }

    @Override
    protected ManagedRealm getTestRealm() {
        return this.masterRealm;
    }

    @Test
    public void getClient() {
        NotFoundException ex = assertThrows(
            NotFoundException.class,
            () -> getClientApi("account").getClient()
        );

        assertTrue(ex.getMessage().contains("HTTP 404 Not Found"));
    }
}
