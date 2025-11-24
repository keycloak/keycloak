package org.keycloak.tests.admin.model.workflow;

import java.time.Duration;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.workflow.client.DisableClientStepProviderFactory;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import org.junit.jupiter.api.Test;

import static org.keycloak.models.workflow.ResourceOperationType.CLIENT_ADDED;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = WorkflowsBlockingServerConfig.class)
public class DisableClientStepTest extends AbstractWorkflowTest {

    @Test
    public void testStepRun() {
        var response = managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(CLIENT_ADDED.name())
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(DisableClientStepProviderFactory.ID)
                                .after(Duration.ofDays(1))
                                .build()
                ).build());
        assertEquals(201, response.getStatus());
        response.close();

        ClientRepresentation rep = new ClientRepresentation();
        rep.setName("my-client-name");
        rep.setClientId("my-client");
        rep.setFullScopeAllowed(true);
        rep.setSecret("618268aa-51e6-4e64-93c4-3c0bc65b8171");
        rep.setProtocol("openid-connect");
        rep.setPublicClient(false);
        rep.setEnabled(true);
        response = managedRealm.admin().clients().create(rep);
        assertEquals(201, response.getStatus());
        response.close();

        runScheduledSteps(Duration.ZERO);

        runOnServer.run((session -> {
            RealmModel realm = session.getContext().getRealm();
            ClientModel client = session.clients().getClientByClientId(realm, "my-client");
            assertNotNull(client);
            assertTrue(client.isEnabled());
        }));

        runScheduledSteps(Duration.ofDays(2));

        runOnServer.run((session -> {
            RealmModel realm = session.getContext().getRealm();
            ClientModel client = session.clients().getClientByClientId(realm, "my-client");
            assertNotNull(client);
            assertFalse(client.isEnabled());
        }));

    }
}
