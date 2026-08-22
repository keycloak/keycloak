package org.keycloak.tests.workflow.condition;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.workflow.Workflow;
import org.keycloak.models.workflow.WorkflowProvider;
import org.keycloak.models.workflow.WorkflowStateProvider;
import org.keycloak.models.workflow.WorkflowStateProvider.ScheduledStep;
import org.keycloak.models.workflow.client.DisableClientStepProviderFactory;
import org.keycloak.models.workflow.conditions.ClientAttributeWorkflowConditionFactory;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowScheduleRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.workflow.AbstractWorkflowTest;
import org.keycloak.tests.workflow.config.WorkflowsBlockingServerConfig;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = WorkflowsBlockingServerConfig.class)
public class ClientAttributeWorkflowConditionTest extends AbstractWorkflowTest {

    @Test
    public void testConditionForAnyValuedAttribute() {
        createWorkflow(ClientAttributeWorkflowConditionFactory.ID + "(attribute)");

        String matching1 = createClient("client-1", Map.of("attribute", "singleValue"));
        String matching2 = createClient("client-2", Map.of("attribute", "otherValue"));
        createClient("client-3", Map.of());

        awaitActivationFor(matching1, matching2);

        runScheduledSteps(Duration.ofDays(6));

        assertClientEnabled("client-1", false);
        assertClientEnabled("client-2", false);
        assertClientEnabled("client-3", true);
    }

    @Test
    public void testConditionForExactValue() {
        createWorkflow(ClientAttributeWorkflowConditionFactory.ID + "(attribute:valid)");

        String matching = createClient("client-1", Map.of("attribute", "valid"));
        createClient("client-2", Map.of("attribute", "not-valid"));
        createClient("client-3", Map.of());

        awaitActivationFor(matching);

        runScheduledSteps(Duration.ofDays(6));

        assertClientEnabled("client-1", false);
        assertClientEnabled("client-2", true);
        assertClientEnabled("client-3", true);
    }

    private void createWorkflow(String condition) {
        // the clients are picked up by the scheduled activation - the client-created event cannot be used
        // to activate attribute-based workflows because it is published before the attributes are set
        // (see https://github.com/keycloak/keycloak/issues/51594)
        WorkflowRepresentation workflow = WorkflowRepresentation.withName("myworkflow")
                .schedule(WorkflowScheduleRepresentation.create().after("1s").build())
                .onCondition(condition)
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(DisableClientStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build()
                ).build();

        try (Response response = managedRealm.admin().workflows().create(workflow)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }
    }

    private String createClient(String clientId, Map<String, String> attributes) {
        ClientRepresentation rep = new ClientRepresentation();
        rep.setClientId(clientId);
        rep.setProtocol("openid-connect");
        rep.setEnabled(true);
        rep.setAttributes(attributes);
        try (Response response = managedRealm.admin().clients().create(rep)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            return ApiUtil.getCreatedId(response);
        }
    }

    private void awaitActivationFor(String... expectedClientUuids) {
        Awaitility.await()
                .timeout(Duration.ofSeconds(15))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> runOnServer.run((RunOnServer) session -> {
                    WorkflowProvider provider = session.getProvider(WorkflowProvider.class);
                    List<Workflow> workflows = provider.getWorkflows().toList();
                    assertThat(workflows, hasSize(1));

                    WorkflowStateProvider stateProvider = session.getProvider(WorkflowStateProvider.class);
                    List<String> scheduledResources = stateProvider.getScheduledStepsByWorkflow(workflows.get(0).getId())
                            .map(ScheduledStep::resourceId)
                            .toList();
                    assertThat(scheduledResources, containsInAnyOrder(expectedClientUuids));
                }));
    }

    private void assertClientEnabled(String clientId, boolean expectedEnabled) {
        runOnServer.run((session -> {
            RealmModel realm = session.getContext().getRealm();
            ClientModel client = session.clients().getClientByClientId(realm, clientId);
            if (expectedEnabled) {
                assertTrue(client.isEnabled());
            } else {
                assertFalse(client.isEnabled());
            }
        }));
    }
}
