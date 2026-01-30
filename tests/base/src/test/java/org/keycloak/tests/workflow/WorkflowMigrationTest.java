package org.keycloak.tests.workflow;

import java.time.Duration;
import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.models.workflow.AddRequiredActionStepProvider;
import org.keycloak.models.workflow.AddRequiredActionStepProviderFactory;
import org.keycloak.models.workflow.DeleteUserStepProviderFactory;
import org.keycloak.models.workflow.DisableUserStepProviderFactory;
import org.keycloak.models.workflow.SetUserAttributeStepProviderFactory;
import org.keycloak.models.workflow.WorkflowStateProvider;
import org.keycloak.models.workflow.client.DeleteClientStepProviderFactory;
import org.keycloak.models.workflow.conditions.UserAttributeWorkflowConditionFactory;
import org.keycloak.models.workflow.events.ClientCreatedWorkflowEventFactory;
import org.keycloak.models.workflow.events.UserCreatedWorkflowEventFactory;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.workflows.StepExecutionStatus;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.workflow.config.WorkflowsBlockingServerConfig;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

/**
 * Tests migrating resources from one workflow to another.
 */
@KeycloakIntegrationTest(config = WorkflowsBlockingServerConfig.class)
public class WorkflowMigrationTest extends AbstractWorkflowTest {

    @Test
    public void testMigrationFailsIfWorkflowsNotCompatible() {
        // create two incompatible workflows - one that operates on users, another on clients
        var response = managedRealm.admin().workflows().create(WorkflowRepresentation.withName("client-workflow")
                .onEvent(ClientCreatedWorkflowEventFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(DeleteClientStepProviderFactory.ID)
                                .after(Duration.ofDays(10))
                                .build()
                ).build());
        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        String clientWorkflowId = ApiUtil.getCreatedId(response);
        response.close();

        response = managedRealm.admin().workflows().create(WorkflowRepresentation.withName("user-workflow")
                .onEvent(UserCreatedWorkflowEventFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(AddRequiredActionStepProviderFactory.ID)
                                .withConfig(AddRequiredActionStepProvider.REQUIRED_ACTION_KEY, "UPDATE_PASSWORD")
                                .after(Duration.ofDays(5))
                                .build()
                ).build());
        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        String userWorkflowId = ApiUtil.getCreatedId(response);
        response.close();

        // create a few of users so that they are attached to the user workflow
        for (int i = 1; i <= 3; i++) {
            try (var createUserResponse = managedRealm.admin().users().create(UserConfigBuilder.create().username("user-" + i).build())) {
                assertThat(createUserResponse.getStatus(), is(Response.Status.CREATED.getStatusCode()));
                String userId = ApiUtil.getCreatedId(createUserResponse);
                // check created user is attached to the first workflow
                List<WorkflowRepresentation> activeWorkflows = managedRealm.admin().workflows().getScheduledWorkflows(userId);
                assertThat(activeWorkflows, hasSize(1));
                assertThat(activeWorkflows.get(0).getName(), is("user-workflow"));
            }
        }

        // attempt to migrate the users from the user workflow to the client workflow - should fail
        WorkflowRepresentation userWorkflow = managedRealm.admin().workflows().workflow(userWorkflowId).toRepresentation();
        String fromStepId = userWorkflow.getSteps().get(0).getId();
        WorkflowRepresentation clientWorkflow = managedRealm.admin().workflows().workflow(clientWorkflowId).toRepresentation();
        String toStepId = clientWorkflow.getSteps().get(0).getId();

        try (var migrateResponse = managedRealm.admin().workflows().migrate(fromStepId, toStepId)) {
            assertThat(migrateResponse.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        }
    }

    @Test
    public void testMigrationFailsIfResourcesDontMeetWorkflowConditions() {
        // create two user workflows, the second having a condition that the users don't meet
        var response = managedRealm.admin().workflows().create(WorkflowRepresentation.withName("workflow-1")
                .onEvent(UserCreatedWorkflowEventFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build()
                ).build());
        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        String firstWorkflowId = ApiUtil.getCreatedId(response);
        response.close();

        response = managedRealm.admin().workflows().create(WorkflowRepresentation.withName("workflow-2")
                .onCondition(UserAttributeWorkflowConditionFactory.ID + "(some-missing-key:some-missing-value)")
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(SetUserAttributeStepProviderFactory.ID)
                                .withConfig("key", "value")
                                .after(Duration.ofDays(10))
                                .build()
                ).build());
        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        String secondWorkflowId = ApiUtil.getCreatedId(response);
        response.close();

        // create a few of users so that they are attached to the first workflow
        for (int i = 1; i <= 3; i++) {
            try (var createUserResponse = managedRealm.admin().users().create(UserConfigBuilder.create().username("user-" + i).build())) {
                assertThat(createUserResponse.getStatus(), is(Response.Status.CREATED.getStatusCode()));
                String userId = ApiUtil.getCreatedId(createUserResponse);
                // check created user is attached to the first workflow
                List<WorkflowRepresentation> activeWorkflows = managedRealm.admin().workflows().getScheduledWorkflows(userId);
                assertThat(activeWorkflows, hasSize(1));
                assertThat(activeWorkflows.get(0).getName(), is("workflow-1"));
            }
        }

        // attempt to migrate the users from the first workflow to the second workflow - should fail
        WorkflowRepresentation firstWorkflow = managedRealm.admin().workflows().workflow(firstWorkflowId).toRepresentation();
        String fromStepId = firstWorkflow.getSteps().get(0).getId();
        WorkflowRepresentation secondWorkflow = managedRealm.admin().workflows().workflow(secondWorkflowId).toRepresentation();
        String toStepId = secondWorkflow.getSteps().get(0).getId();

        try (var migrateResponse = managedRealm.admin().workflows().migrate(fromStepId, toStepId)) {
            assertThat(migrateResponse.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        }
    }

    @Test
    public void testMigrateToScheduledStepInDifferentWorkflow() {

        var response = managedRealm.admin().workflows().create(WorkflowRepresentation.withName("workflow-1")
                .onEvent(UserCreatedWorkflowEventFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build()
                ).build());
        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        String firstWorkflowId = ApiUtil.getCreatedId(response);
        response.close();

        response = managedRealm.admin().workflows().create(WorkflowRepresentation.withName("workflow-2")
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        WorkflowStepRepresentation.create()
                                .of(DeleteUserStepProviderFactory.ID)
                                .after(Duration.ofDays(10))
                                .build()
                ).build());
        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        String secondWorkflowId = ApiUtil.getCreatedId(response);
        response.close();

        // create a few of users so that they are attached to the first workflow
        String[] userIds = new String[4];
        for (int i = 0; i <= 3; i++) {
            try (var createUserResponse = managedRealm.admin().users().create(UserConfigBuilder.create().username("user-" + i).build())) {
                assertThat(createUserResponse.getStatus(), is(Response.Status.CREATED.getStatusCode()));
                userIds[i] = ApiUtil.getCreatedId(createUserResponse);
                // check created user is attached to the first workflow
                List<WorkflowRepresentation> activeWorkflows = managedRealm.admin().workflows().getScheduledWorkflows(userIds[i]);
                assertThat(activeWorkflows, hasSize(1));
                assertThat(activeWorkflows.get(0).getName(), is("workflow-1"));
            }
        }

        // migrate users to the delete step in the second workflow
        WorkflowRepresentation firstWorkflow = managedRealm.admin().workflows().workflow(firstWorkflowId).toRepresentation();
        String fromStepId = firstWorkflow.getSteps().get(0).getId();
        WorkflowRepresentation secondWorkflow = managedRealm.admin().workflows().workflow(secondWorkflowId).toRepresentation();
        assertThat(secondWorkflow.getSteps().get(1).getUses(), is(DeleteUserStepProviderFactory.ID));
        String toStepId = secondWorkflow.getSteps().get(1).getId();

        try (var migrateResponse = managedRealm.admin().workflows().migrate(fromStepId, toStepId)) {
            assertThat(migrateResponse.getStatus(), is(Response.Status.NO_CONTENT.getStatusCode()));
        }

        // check users are now attached to the second workflow and scheduled to run the second step
        runOnServer.run((RunOnServer) session -> {
            // the workflow state table should have a scheduled step for the user in the first workflow
            for (int i = 0; i <= 3; i++) {
                WorkflowStateProvider stateProvider = session.getKeycloakSessionFactory().getProviderFactory(WorkflowStateProvider.class).create(session);
                List<WorkflowStateProvider.ScheduledStep> steps = stateProvider.getScheduledStepsByResource(userIds[i]).toList();
                assertThat(steps, hasSize(1));
                assertThat(steps.get(0).workflowId(), is(secondWorkflowId));
                assertThat(steps.get(0).stepId(), is(toStepId));
            }
        });
    }

    @Test
    public void testMigrateToImmediateStepInDifferentWorkflow() {
        var response = managedRealm.admin().workflows().create(WorkflowRepresentation.withName("workflow-1")
                .onEvent(UserCreatedWorkflowEventFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build()
                ).build());
        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        String firstWorkflowId = ApiUtil.getCreatedId(response);
        response.close();

        response = managedRealm.admin().workflows().create(WorkflowRepresentation.withName("workflow-2")
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(DisableUserStepProviderFactory.ID)
                                .build(),
                        WorkflowStepRepresentation.create()
                                .of(DeleteUserStepProviderFactory.ID)
                                .after(Duration.ofDays(10))
                                .build()
                ).build());
        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        String secondWorkflowId = ApiUtil.getCreatedId(response);
        response.close();

        // create a few of users so that they are attached to the first workflow
        String[] userIds = new String[4];
        for (int i = 0; i <= 3; i++) {
            try (var createUserResponse = managedRealm.admin().users().create(UserConfigBuilder.create().username("user-" + i).build())) {
                assertThat(createUserResponse.getStatus(), is(Response.Status.CREATED.getStatusCode()));
                userIds[i] = ApiUtil.getCreatedId(createUserResponse);
                // check created user is attached to the first workflow
                List<WorkflowRepresentation> activeWorkflows = managedRealm.admin().workflows().getScheduledWorkflows(userIds[i]);
                assertThat(activeWorkflows, hasSize(1));
                assertThat(activeWorkflows.get(0).getName(), is("workflow-1"));
            }
        }

        // migrate users to the first step in the second workflow - it is an immediate step, so it should run right away
        WorkflowRepresentation firstWorkflow = managedRealm.admin().workflows().workflow(firstWorkflowId).toRepresentation();
        String fromStepId = firstWorkflow.getSteps().get(0).getId();
        WorkflowRepresentation secondWorkflow = managedRealm.admin().workflows().workflow(secondWorkflowId).toRepresentation();
        String toStepId = secondWorkflow.getSteps().get(0).getId();

        try (var migrateResponse = managedRealm.admin().workflows().migrate(fromStepId, toStepId)) {
            assertThat(migrateResponse.getStatus(), is(Response.Status.NO_CONTENT.getStatusCode()));
        }

        // check the users are now disabled, and scheduled to run the second step
        for (int i = 0; i <= 3; i++) {
            UserRepresentation user = managedRealm.admin().users().get(userIds[i]).toRepresentation();
            assertThat(user.isEnabled(), is(false));
            List<WorkflowRepresentation> activeWorkflows = managedRealm.admin().workflows().getScheduledWorkflows(userIds[i]);
            assertThat(activeWorkflows, hasSize(1));
            WorkflowRepresentation firstActiveWorkflow = activeWorkflows.get(0);
            assertThat(firstActiveWorkflow.getName(), is("workflow-2"));
            assertThat(firstActiveWorkflow.getSteps(), hasSize(2));
            assertThat(firstActiveWorkflow.getSteps().get(0).getExecutionStatus(), is(StepExecutionStatus.COMPLETED));
            assertThat(firstActiveWorkflow.getSteps().get(1).getExecutionStatus(), is(StepExecutionStatus.PENDING));
        }
    }

    @Test
    public void testMigrateToStepInSameWorkflow() {
        var response = managedRealm.admin().workflows().create(WorkflowRepresentation.withName("workflow")
                .onEvent(UserCreatedWorkflowEventFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create()
                                .of(AddRequiredActionStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .withConfig(AddRequiredActionStepProvider.REQUIRED_ACTION_KEY, "UPDATE_PASSWORD")
                                .build(),
                        WorkflowStepRepresentation.create()
                                .of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(10))
                                .build(),
                        WorkflowStepRepresentation.create()
                                .of(DeleteUserStepProviderFactory.ID)
                                .after(Duration.ofDays(20))
                                .build()
                ).build());
        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        String workflowId = ApiUtil.getCreatedId(response);
        response.close();

        // create a few of users so that they are attached to the workflow
        String[] userIds = new String[4];
        for (int i = 0; i <= 3; i++) {
            try (var createUserResponse = managedRealm.admin().users().create(UserConfigBuilder.create().username("user-" + i).build())) {
                assertThat(createUserResponse.getStatus(), is(Response.Status.CREATED.getStatusCode()));
                userIds[i] = ApiUtil.getCreatedId(createUserResponse);
                // check created user is attached to the first workflow
                List<WorkflowRepresentation> activeWorkflows = managedRealm.admin().workflows().getScheduledWorkflows(userIds[i]);
                assertThat(activeWorkflows, hasSize(1));
                assertThat(activeWorkflows.get(0).getName(), is("workflow"));
                assertThat(activeWorkflows.get(0).getSteps(), hasSize(3));
                assertThat(activeWorkflows.get(0).getSteps().get(0).getExecutionStatus(), is(StepExecutionStatus.PENDING));
            }
        }

        // migrate users to the delete step in the same workflow
        WorkflowRepresentation workflow = managedRealm.admin().workflows().workflow(workflowId).toRepresentation();
        String fromStepId = workflow.getSteps().get(0).getId();
        String toStepId = workflow.getSteps().get(2).getId();

        try (var migrateResponse = managedRealm.admin().workflows().migrate(fromStepId, toStepId)) {
            assertThat(migrateResponse.getStatus(), is(Response.Status.NO_CONTENT.getStatusCode()));
        }

        // check the users are now scheduled to run the delete step instead of the first step
        for (int i = 0; i <= 3; i++) {
            List<WorkflowRepresentation> activeWorkflows = managedRealm.admin().workflows().getScheduledWorkflows(userIds[i]);
            assertThat(activeWorkflows, hasSize(1));
            WorkflowRepresentation firstActiveWorkflow = activeWorkflows.get(0);
            assertThat(firstActiveWorkflow.getName(), is("workflow"));
            assertThat(firstActiveWorkflow.getSteps(), hasSize(3));
            assertThat(firstActiveWorkflow.getSteps().get(0).getExecutionStatus(), is(StepExecutionStatus.COMPLETED));
            assertThat(firstActiveWorkflow.getSteps().get(1).getExecutionStatus(), is(StepExecutionStatus.COMPLETED));
            assertThat(firstActiveWorkflow.getSteps().get(2).getExecutionStatus(), is(StepExecutionStatus.PENDING));
        }
    }
}
