package org.keycloak.tests.workflow.execution;

import java.time.Duration;
import java.util.List;

import jakarta.mail.internet.MimeMessage;

import org.keycloak.common.util.Time;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.workflow.DisableUserStepProviderFactory;
import org.keycloak.models.workflow.NotifyUserStepProviderFactory;
import org.keycloak.models.workflow.Workflow;
import org.keycloak.models.workflow.WorkflowProvider;
import org.keycloak.models.workflow.WorkflowStateProvider;
import org.keycloak.models.workflow.WorkflowStep;
import org.keycloak.models.workflow.events.UserCreatedWorkflowEventFactory;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
import org.keycloak.tests.workflow.AbstractWorkflowTest;
import org.keycloak.tests.workflow.config.WorkflowsBlockingServerConfig;

import org.junit.jupiter.api.Test;

import static org.keycloak.tests.workflow.util.EmailTestUtils.findEmailByRecipient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests that scheduled workflow steps are executed correctly over time.
 */
@KeycloakIntegrationTest(config = WorkflowsBlockingServerConfig.class)
public class ScheduledWorkflowExecutionTest extends AbstractWorkflowTest {

    @InjectMailServer
    MailServer mailServer;

    @Test
    public void testWorkflowDoesNotFallThroughStepsInSingleRun() {
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(UserCreatedWorkflowEventFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(10))
                                .build()
                ).build()).close();

        // create a new user - should bind the user to the workflow and setup the first step
        managedRealm.admin().users().create(UserConfigBuilder.create().username("testuser").email("testuser@example.com").build()).close();

        runOnServer.run((RunOnServer) session -> {
            RealmModel realm = session.getContext().getRealm();
            WorkflowProvider provider = session.getProvider(WorkflowProvider.class);
            UserModel user = session.users().getUserByUsername(realm, "testuser");

            List<Workflow> registeredWorkflows = provider.getWorkflows().toList();
            assertEquals(1, registeredWorkflows.size());

            Workflow workflow = registeredWorkflows.get(0);
            List<WorkflowStep> steps = workflow.getSteps().toList();
            assertEquals(2, steps.size());
            WorkflowStep notifyStep = steps.get(0);

            WorkflowStateProvider stateProvider = session.getProvider(WorkflowStateProvider.class);
            WorkflowStateProvider.ScheduledStep scheduledStep = stateProvider.getScheduledStep(workflow.getId(), user.getId());
            assertNotNull(scheduledStep, "A step should have been scheduled for the user " + user.getUsername());
            assertEquals(notifyStep.getId(), scheduledStep.stepId());
        });

        // Simulate the user being 12 days old, making them eligible for both steps' time conditions.
        runScheduledSteps(Duration.ofDays(12));

        runOnServer.run((RunOnServer) session -> {
            RealmModel realm = session.getContext().getRealm();
            WorkflowProvider provider = session.getProvider(WorkflowProvider.class);
            UserModel user = session.users().getUserByUsername(realm, "testuser");

            try {
                user = session.users().getUserById(realm, user.getId());
                WorkflowStateProvider stateProvider = session.getProvider(WorkflowStateProvider.class);
                List<Workflow> registeredWorkflows = provider.getWorkflows().toList();
                assertEquals(1, registeredWorkflows.size());

                Workflow workflow = registeredWorkflows.get(0);
                // Verify that the next step was scheduled for the user
                WorkflowStep disableStep = workflow.getSteps().toList().get(1);
                WorkflowStateProvider.ScheduledStep scheduledStep = stateProvider.getScheduledStep(workflow.getId(), user.getId());
                assertNotNull(scheduledStep, "A step should have been scheduled for the user " + user.getUsername());
                assertEquals(disableStep.getId(), scheduledStep.stepId(), "The second step should have been scheduled");
            } finally {
                Time.setOffset(0);
            }
        });

        // Verify that the first step (notify) was executed by checking email was sent
        MimeMessage testUserMessage = findEmailByRecipient(mailServer, "testuser@example.com");
        assertNotNull(testUserMessage, "The first step (notify) should have sent an email.");

        mailServer.runCleanup();
    }

}
