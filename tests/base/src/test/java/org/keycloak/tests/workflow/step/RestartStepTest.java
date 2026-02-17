package org.keycloak.tests.workflow.step;

import java.time.Duration;

import jakarta.ws.rs.core.Response;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.workflow.NotifyUserStepProviderFactory;
import org.keycloak.models.workflow.RestartWorkflowStepProviderFactory;
import org.keycloak.models.workflow.SetUserAttributeStepProviderFactory;
import org.keycloak.models.workflow.Workflow;
import org.keycloak.models.workflow.WorkflowProvider;
import org.keycloak.models.workflow.WorkflowStateProvider;
import org.keycloak.models.workflow.WorkflowStep;
import org.keycloak.models.workflow.events.UserCreatedWorkflowEventFactory;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.remote.providers.runonserver.FetchOnServer;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
import org.keycloak.tests.workflow.AbstractWorkflowTest;
import org.keycloak.tests.workflow.config.WorkflowsBlockingServerConfig;

import org.junit.jupiter.api.Test;

import static org.keycloak.tests.workflow.util.EmailTestUtils.findEmailsByRecipient;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests the execution of the 'restart' workflow step.
 */
@KeycloakIntegrationTest(config = WorkflowsBlockingServerConfig.class)
public class RestartStepTest extends AbstractWorkflowTest {

    @InjectMailServer
    private MailServer mailServer;

    @Test
    public void testRestartWorkflow() {
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(UserCreatedWorkflowEventFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        WorkflowStepRepresentation.create().of(RestartWorkflowStepProviderFactory.ID)
                                .build()
                ).build()).close();

        // create a new user - should bind the user to the workflow and setup the only step in the workflow
        managedRealm.admin().users().create(UserConfigBuilder.create().username("testuser").email("testuser@example.com").build()).close();

        Long scheduledAt = runOnServer.fetch((FetchOnServer) session -> {
            RealmModel realm = session.getContext().getRealm();
            WorkflowProvider provider = session.getProvider(WorkflowProvider.class);
            UserModel user = session.users().getUserByUsername(realm, "testuser");
            Workflow workflow = provider.getWorkflows().toList().get(0);
            WorkflowStateProvider stateProvider = session.getProvider(WorkflowStateProvider.class);
            WorkflowStateProvider.ScheduledStep scheduledStep = stateProvider.getScheduledStep(workflow.getId(), user.getId());
            assertNotNull(scheduledStep, "A step should have been scheduled for the user " + user.getUsername());
            return scheduledStep.scheduledAt();
        }, Long.class);

        runScheduledSteps(Duration.ofDays(6));

        Long reScheduleAt = runOnServer.fetch((FetchOnServer) session -> {
            RealmModel realm = session.getContext().getRealm();
            WorkflowProvider provider = session.getProvider(WorkflowProvider.class);

            UserModel user = session.users().getUserByUsername(realm, "testuser");
            Workflow workflow = provider.getWorkflows().toList().get(0);
            WorkflowStep step = workflow.getSteps().toList().get(0);

            // Verify that the step was scheduled again for the user
            WorkflowStateProvider stateProvider = session.getProvider(WorkflowStateProvider.class);
            WorkflowStateProvider.ScheduledStep scheduledStep = stateProvider.getScheduledStep(workflow.getId(), user.getId());
            assertNotNull(scheduledStep, "A step should have been scheduled for the user " + user.getUsername());
            assertEquals(step.getId(), scheduledStep.stepId(), "The step should have been scheduled again");
            assertThat("The step should have been scheduled again at a later time", scheduledStep.scheduledAt(), greaterThan(scheduledAt));
            return  scheduledStep.scheduledAt();
        }, Long.class);

        runScheduledSteps(Duration.ofDays(12));

        runOnServer.run((RunOnServer) session -> {
            RealmModel realm = session.getContext().getRealm();
            WorkflowProvider provider = session.getProvider(WorkflowProvider.class);

            UserModel user = session.users().getUserByUsername(realm, "testuser");
            Workflow workflow = provider.getWorkflows().toList().get(0);
            WorkflowStep step = workflow.getSteps().toList().get(0);

            // Verify that the step was scheduled again for the user
            WorkflowStateProvider stateProvider = session.getProvider(WorkflowStateProvider.class);
            WorkflowStateProvider.ScheduledStep scheduledStep = stateProvider.getScheduledStep(workflow.getId(), user.getId());
            assertNotNull(scheduledStep, "A step should have been scheduled for the user " + user.getUsername());
            assertEquals(step.getId(), scheduledStep.stepId(), "The step should have been scheduled again");
            assertThat("The step should have been scheduled again at a later time", scheduledStep.scheduledAt(), greaterThan(reScheduleAt));
        });


        // Verify that there should be two emails sent
        assertEquals(2, findEmailsByRecipient(mailServer, "testuser@example.com").size());
        mailServer.runCleanup();
    }

    @Test
    public void testRestartFromPosition() {
        try (Response response = managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(UserCreatedWorkflowEventFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        WorkflowStepRepresentation.create().of(SetUserAttributeStepProviderFactory.ID)
                                .withConfig("test", "value")
                                .build(),
                        WorkflowStepRepresentation.create().of(RestartWorkflowStepProviderFactory.ID)
                                .withConfig(RestartWorkflowStepProviderFactory.CONFIG_POSITION, "1")
                                .build()
                ).build())) {
            assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
            assertThat(response.readEntity(ErrorRepresentation.class).getErrorMessage(),
                    is("No scheduled step found if restarting at position 1"));
        }
        try (Response response = managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(UserCreatedWorkflowEventFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        WorkflowStepRepresentation.create().of(SetUserAttributeStepProviderFactory.ID)
                                .withConfig("test", "value")
                                .build(),
                        WorkflowStepRepresentation.create().of(RestartWorkflowStepProviderFactory.ID)
                                .withConfig(RestartWorkflowStepProviderFactory.CONFIG_POSITION, "2")
                                .build()
                ).build())) {
            assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
            assertThat(response.readEntity(ErrorRepresentation.class).getErrorMessage(),
                    is("No scheduled step found if restarting at position 2"));
        }
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(UserCreatedWorkflowEventFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        WorkflowStepRepresentation.create().of(SetUserAttributeStepProviderFactory.ID)
                                .withConfig("first", "first")
                                .build(),
                        WorkflowStepRepresentation.create().of(SetUserAttributeStepProviderFactory.ID)
                                .withConfig("second", "second")
                                .after(Duration.ofDays(5))
                                .build(),
                        WorkflowStepRepresentation.create().of(RestartWorkflowStepProviderFactory.ID)
                                .withConfig(RestartWorkflowStepProviderFactory.CONFIG_POSITION, "1")
                                .build()
                ).build()).close();

        // create a new user - should bind the user to the workflow and setup the only step in the workflow
        managedRealm.admin().users().create(UserConfigBuilder.create().username("testuser").email("testuser@example.com").build()).close();

        Long scheduleAt = runOnServer.fetch((FetchOnServer) session -> {
            RealmModel realm = session.getContext().getRealm();
            WorkflowProvider provider = session.getProvider(WorkflowProvider.class);
            UserModel user = session.users().getUserByUsername(realm, "testuser");
            Workflow workflow = provider.getWorkflows().toList().get(0);
            WorkflowStep step = workflow.getSteps().toList().get(0);
            WorkflowStateProvider stateProvider = session.getProvider(WorkflowStateProvider.class);
            WorkflowStateProvider.ScheduledStep scheduledStep = stateProvider.getScheduledStep(workflow.getId(), user.getId());
            assertNotNull(scheduledStep, "A step should have been scheduled for the user " + user.getUsername());
            assertEquals(step.getId(), scheduledStep.stepId(), "The step should have been scheduled again");
            return  scheduledStep.scheduledAt();
        }, Long.class);

        runScheduledSteps(Duration.ofDays(6));

        Long reScheduledAt = runOnServer.fetch((FetchOnServer) session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, "testuser");
            // Verify that the first attribute was set, and the second is not yet set
            assertThat(user.getAttributes().get("first"), containsInAnyOrder("first"));
            assertThat(user.getAttributes().get("second"), nullValue());

            // remove the first attribute to verify it gets set again after restart
            user.removeAttribute("first");

            WorkflowProvider provider = session.getProvider(WorkflowProvider.class);
            Workflow workflow = provider.getWorkflows().toList().get(0);
            WorkflowStep step = workflow.getSteps().toList().get(2);
            WorkflowStateProvider stateProvider = session.getProvider(WorkflowStateProvider.class);
            WorkflowStateProvider.ScheduledStep scheduledStep = stateProvider.getScheduledStep(workflow.getId(), user.getId());
            assertNotNull(scheduledStep, "A step should have been scheduled for the user " + user.getUsername());
            assertEquals(step.getId(), scheduledStep.stepId(), "The step should have been scheduled again");
            assertThat("The step should have been scheduled again at a later time", scheduledStep.scheduledAt(), greaterThan(scheduleAt));
            return scheduledStep.scheduledAt();
        }, Long.class);

        runScheduledSteps(Duration.ofDays(6));

        Long reScheduledAtLast = runOnServer.fetch((FetchOnServer) session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, "testuser");
            // Verify that both attributes are set
            assertThat(user.getAttributes().get("first"), containsInAnyOrder("first"));
            assertThat(user.getAttributes().get("second"), containsInAnyOrder("second"));
            WorkflowProvider provider = session.getProvider(WorkflowProvider.class);
            Workflow workflow = provider.getWorkflows().toList().get(0);
            WorkflowStep expectedStep = workflow.getSteps().toList().get(2);

            WorkflowStateProvider stateProvider = session.getProvider(WorkflowStateProvider.class);
            WorkflowStateProvider.ScheduledStep scheduledStep = stateProvider.getScheduledStep(workflow.getId(), user.getId());
            assertNotNull(scheduledStep, "A step should have been scheduled for the user " + user.getUsername());
            assertEquals(expectedStep.getId(), scheduledStep.stepId(), "The step should have been scheduled again");
            assertThat("The step should have been scheduled again at a later time", scheduledStep.scheduledAt(), greaterThan(reScheduledAt));
            return scheduledStep.scheduledAt();
        }, Long.class);

        runScheduledSteps(Duration.ofDays(12));

        // Verify that there should be one email sent, the first step should not have run again
        assertEquals(1, findEmailsByRecipient(mailServer, "testuser@example.com").size());

        runOnServer.run((RunOnServer) session -> {
            WorkflowProvider provider = session.getProvider(WorkflowProvider.class);
            Workflow workflow = provider.getWorkflows().toList().get(0);
            WorkflowStep expectedStep = workflow.getSteps().toList().get(2);
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, "testuser");
            WorkflowStateProvider stateProvider = session.getProvider(WorkflowStateProvider.class);
            WorkflowStateProvider.ScheduledStep scheduledStep = stateProvider.getScheduledStep(workflow.getId(), user.getId());
            assertNotNull(scheduledStep, "A step should have been scheduled for the user " + user.getUsername());
            assertEquals(expectedStep.getId(), scheduledStep.stepId(), "The step should have been scheduled again");
            assertThat("The step should have been scheduled again at a later time", scheduledStep.scheduledAt(), greaterThan(reScheduledAtLast));
        });

        mailServer.runCleanup();
    }

    @Test
    public void testRestartFromLastStep() {
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(UserCreatedWorkflowEventFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        WorkflowStepRepresentation.create().of(SetUserAttributeStepProviderFactory.ID)
                                .withConfig("first", "first")
                                .build(),
                        WorkflowStepRepresentation.create().of(SetUserAttributeStepProviderFactory.ID)
                                .withConfig("second", "second")
                                .after(Duration.ofDays(5))
                                .build(),
                        WorkflowStepRepresentation.create().of(RestartWorkflowStepProviderFactory.ID)
                                .withConfig(RestartWorkflowStepProviderFactory.CONFIG_POSITION, "2")
                                .build()
                ).build()).close();

        // create a new user - should bind the user to the workflow and setup the only step in the workflow
        managedRealm.admin().users().create(UserConfigBuilder.create().username("testuser").email("testuser@example.com").build()).close();

        Long scheduleAt = runOnServer.fetch((FetchOnServer) session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, "testuser");
            WorkflowProvider provider = session.getProvider(WorkflowProvider.class);
            Workflow workflow = provider.getWorkflows().toList().get(0);
            WorkflowStep step = workflow.getSteps().toList().get(0);
            WorkflowStateProvider stateProvider = session.getProvider(WorkflowStateProvider.class);
            WorkflowStateProvider.ScheduledStep scheduledStep = stateProvider.getScheduledStep(workflow.getId(), user.getId());
            assertNotNull(scheduledStep, "A step should have been scheduled for the user " + user.getUsername());
            assertEquals(step.getId(), scheduledStep.stepId(), "The step should have been scheduled again");
            return  scheduledStep.scheduledAt();
        }, Long.class);

        runScheduledSteps(Duration.ofDays(6));

        Long reScheduledAt = runOnServer.fetch((FetchOnServer) session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, "testuser");
            // Verify that the first attribute was set, and the second is not yet set
            assertThat(user.getAttributes().get("first"), containsInAnyOrder("first"));
            assertThat(user.getAttributes().get("second"), nullValue());

            // remove the first attribute to verify it gets set again after restart
            user.removeAttribute("first");
            WorkflowProvider provider = session.getProvider(WorkflowProvider.class);
            Workflow workflow = provider.getWorkflows().toList().get(0);
            WorkflowStep step = workflow.getSteps().toList().get(2);
            WorkflowStateProvider stateProvider = session.getProvider(WorkflowStateProvider.class);
            WorkflowStateProvider.ScheduledStep scheduledStep = stateProvider.getScheduledStep(workflow.getId(), user.getId());
            assertNotNull(scheduledStep, "A step should have been scheduled for the user " + user.getUsername());
            assertEquals(step.getId(), scheduledStep.stepId(), "The step should have been scheduled again");
            assertThat("The step should have been scheduled again at a later time", scheduledStep.scheduledAt(), greaterThan(scheduleAt));
            return scheduledStep.scheduledAt();
        }, Long.class);

        runScheduledSteps(Duration.ofDays(6));

        Long reScheduledAtLast = runOnServer.fetch((FetchOnServer) session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, "testuser");
            // Verify that first attribute is not set, and the second is set
            assertThat(user.getAttributes().get("first"), nullValue());
            assertThat(user.getAttributes().get("second"), containsInAnyOrder("second"));
            WorkflowProvider provider = session.getProvider(WorkflowProvider.class);
            Workflow workflow = provider.getWorkflows().toList().get(0);
            WorkflowStep expectedStep = workflow.getSteps().toList().get(2);
            WorkflowStateProvider stateProvider = session.getProvider(WorkflowStateProvider.class);
            WorkflowStateProvider.ScheduledStep scheduledStep = stateProvider.getScheduledStep(workflow.getId(), user.getId());
            assertNotNull(scheduledStep, "A step should have been scheduled for the user " + user.getUsername());
            assertEquals(expectedStep.getId(), scheduledStep.stepId(), "The step should have been scheduled again");
            assertThat("The step should have been scheduled again at a later time", scheduledStep.scheduledAt(), greaterThan(reScheduledAt));
            user.removeAttribute("second");
            return scheduledStep.scheduledAt();
        }, Long.class);

        runScheduledSteps(Duration.ofDays(12));

        // Verify that there should be one email sent, the first step should not have run again
        assertEquals(1, findEmailsByRecipient(mailServer, "testuser@example.com").size());
        runOnServer.run((RunOnServer) session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, "testuser");
            // Verify that the first attribute is not set, and the second is set again
            assertThat(user.getAttributes().get("first"), nullValue());
            assertThat(user.getAttributes().get("second"), containsInAnyOrder(("second")));
            WorkflowProvider provider = session.getProvider(WorkflowProvider.class);
            Workflow workflow = provider.getWorkflows().toList().get(0);
            WorkflowStep expectedStep = workflow.getSteps().toList().get(2);
            WorkflowStateProvider stateProvider = session.getProvider(WorkflowStateProvider.class);
            WorkflowStateProvider.ScheduledStep scheduledStep = stateProvider.getScheduledStep(workflow.getId(), user.getId());
            assertNotNull(scheduledStep, "A step should have been scheduled for the user " + user.getUsername());
            assertEquals(expectedStep.getId(), scheduledStep.stepId(), "The step should have been scheduled again");
            assertThat("The step should have been scheduled again at a later time", scheduledStep.scheduledAt(), greaterThan(reScheduledAtLast));
        });
        mailServer.runCleanup();
    }
}
