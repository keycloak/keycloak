package org.keycloak.tests.workflow.execution;

import java.time.Duration;
import java.util.List;

import jakarta.mail.internet.MimeMessage;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.WorkflowsResource;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.workflow.DisableUserStepProviderFactory;
import org.keycloak.models.workflow.NotifyUserStepProviderFactory;
import org.keycloak.models.workflow.ResourceOperationType;
import org.keycloak.models.workflow.Workflow;
import org.keycloak.models.workflow.WorkflowProvider;
import org.keycloak.models.workflow.WorkflowStateProvider;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.workflow.AbstractWorkflowTest;
import org.keycloak.tests.workflow.config.WorkflowsBlockingServerConfig;

import org.junit.jupiter.api.Test;

import static org.keycloak.tests.workflow.util.EmailTestUtils.findEmailByRecipient;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests disabling an active workflow and its effect on scheduled steps and new user bindings.
 */
@KeycloakIntegrationTest(config = WorkflowsBlockingServerConfig.class)
public class DisableActiveWorkflowTest extends AbstractWorkflowTest {

    @InjectMailServer
    private MailServer mailServer;

    @Test
    public void testDisableActiveWorkflow() {
        // create a test workflow
        String workflowId;
        try (Response response = managedRealm.admin().workflows().create(WorkflowRepresentation.withName("test-workflow")
                .onEvent(ResourceOperationType.USER_CREATED.toString())
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build()
                ).build())) {
            workflowId = ApiUtil.getCreatedId(response);
        }

        WorkflowsResource workflows = managedRealm.admin().workflows();
        List<WorkflowRepresentation> actualWorkflows = workflows.list();
        assertThat(actualWorkflows, hasSize(1));
        WorkflowRepresentation workflow = actualWorkflows.get(0);
        assertThat(workflow.getName(), is("test-workflow"));

        // create a new user - should bind the user to the workflow and setup the first step
        managedRealm.admin().users().create(UserConfigBuilder.create().username("testuser").email("testuser@example.com").build()).close();

        // Advance time so the user is eligible for the first step, then run the scheduled steps so they transition to the next one.
        runScheduledSteps(Duration.ofDays(6));

        runOnServer.run((RunOnServer) session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, "testuser");
            assertTrue(user.isEnabled(), "The second step (disable) should NOT have run.");
        });

        // Verify that the first step (notify) was executed by checking email was sent
        MimeMessage testUserMessage = findEmailByRecipient(mailServer, "testuser@example.com");
        assertNotNull(testUserMessage, "The first step (notify) should have sent an email.");

        mailServer.runCleanup();

        // disable the workflow - scheduled steps should be paused and workflow should not activate for new users
        workflow.setEnabled(false);
        try (Response response = managedRealm.admin().workflows().workflow(workflowId).update(workflow)) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        // create another user - should NOT bind the user to the workflow as it is disabled
        managedRealm.admin().users().create(UserConfigBuilder.create().username("anotheruser").build()).close();

        // Advance time so the first user would be eligible for the second step, then run the scheduled steps.
        runScheduledSteps(Duration.ofDays(12));

        runOnServer.run((RunOnServer) session -> {
            RealmModel realm = session.getContext().getRealm();
            WorkflowProvider provider = session.getProvider(WorkflowProvider.class);

            List<Workflow> registeredWorkflow = provider.getWorkflows().toList();
            assertEquals(1, registeredWorkflow.size());
            WorkflowStateProvider stateProvider = session.getKeycloakSessionFactory().getProviderFactory(WorkflowStateProvider.class).create(session);
            List<WorkflowStateProvider.ScheduledStep> scheduledSteps = stateProvider.getScheduledStepsByWorkflow(registeredWorkflow.get(0)).toList();

            // verify that there's only one scheduled step, for the first user
            assertEquals(1, scheduledSteps.size());
            UserModel scheduledStepUser = session.users().getUserById(realm, scheduledSteps.get(0).resourceId());
            assertNotNull(scheduledStepUser);
            assertTrue(scheduledStepUser.getUsername().startsWith("testuser"));

            UserModel user = session.users().getUserByUsername(realm, "testuser");
            // Verify that the step was NOT executed as the workflow is disabled.
            assertTrue(user.isEnabled(), "The second step (disable) should NOT have run as the workflow is disabled.");
        });

        // re-enable the workflow - scheduled steps should resume and new users should be bound to the workflow
        workflow.setEnabled(true);
        try (Response response = managedRealm.admin().workflows().workflow(workflowId).update(workflow)) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        // create a third user - should bind the user to the workflow as it is enabled again
        managedRealm.admin().users().create(UserConfigBuilder.create().username("thirduser").email("thirduser@example.com").build()).close();

        // Advance time so the first user would be eligible for the second step, and third user would be eligible for the first step, then run the scheduled steps.
        runScheduledSteps(Duration.ofDays(12));

        runOnServer.run((RunOnServer) session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, "testuser");
            // Verify that the step was executed as the workflow was re-enabled.
            assertFalse(user.isEnabled(), "The second step (disable) should have run as the workflow was re-enabled.");

            // Verify that the third user was bound to the workflow
            user = session.users().getUserByUsername(realm, "thirduser");
            assertTrue(user.isEnabled(), "The second step (disable) should NOT have run");
        });

        // Verify that the first step (notify) was executed by checking email was sent
        testUserMessage = findEmailByRecipient(mailServer, "thirduser@example.com");
        assertNotNull(testUserMessage, "The first step (notify) should have sent an email.");

        mailServer.runCleanup();
    }
}
