package org.keycloak.test.examples;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.test.framework.annotations.InjectAdminClient;
import org.keycloak.test.framework.annotations.InjectFlow;
import org.keycloak.test.framework.annotations.KeycloakIntegrationTest;
import org.keycloak.test.framework.flow.ManagedLoginFlow;

@KeycloakIntegrationTest
public class ManagedLoginFlowTest {

    @InjectAdminClient
    private Keycloak keycloak;

    @InjectFlow
    private ManagedLoginFlow flow;

    @BeforeEach
    public void setup() {
        flow.execute();
    }

    @AfterEach
    public void cleanup() {
        flow.rollback();
    }

    @Test
    public void testFlow() {

        Assertions.assertTrue(keycloak.serverInfo().getInfo().getSystemInfo().getUserName().equals("admin"));
    }
}
