package org.keycloak.tests.admin.client.v2;

import jakarta.ws.rs.NotFoundException;

import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
@KeycloakIntegrationTest
public class ClientApiV2DisabledTest extends AbstractClientApiV2Test {
    @Test
    public void getClient() {
        NotFoundException ex = assertThrows(
            NotFoundException.class,
            () -> getClientApi("account").getClient()
        );

        assertTrue(ex.getMessage().contains("HTTP 404 Not Found"));
    }
}
