/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.tests.workflow;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.BearerAuthFilter;
import org.keycloak.admin.client.resource.WorkflowsResource;
import org.keycloak.broker.oidc.KeycloakOIDCIdentityProviderFactory;
import org.keycloak.common.util.Time;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.workflow.DeleteUserStepProviderFactory;
import org.keycloak.models.workflow.DisableUserStepProviderFactory;
import org.keycloak.models.workflow.NotifyUserStepProviderFactory;
import org.keycloak.models.workflow.ResourceOperationType;
import org.keycloak.models.workflow.ResourceType;
import org.keycloak.models.workflow.RestartWorkflowStepProviderFactory;
import org.keycloak.models.workflow.SetUserAttributeStepProviderFactory;
import org.keycloak.models.workflow.Workflow;
import org.keycloak.models.workflow.WorkflowProvider;
import org.keycloak.models.workflow.WorkflowStateProvider;
import org.keycloak.models.workflow.WorkflowStateProvider.ScheduledStep;
import org.keycloak.models.workflow.WorkflowStep;
import org.keycloak.models.workflow.conditions.IdentityProviderWorkflowConditionFactory;
import org.keycloak.models.workflow.conditions.RoleWorkflowConditionFactory;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.remote.providers.runonserver.FetchOnServer;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.MailUtils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.jakarta.rs.yaml.JacksonYAMLProvider;
import com.fasterxml.jackson.jakarta.rs.yaml.YAMLMediaTypes;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.keycloak.models.workflow.ResourceOperationType.USER_CREATED;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = WorkflowsBlockingServerConfig.class)
public class WorkflowManagementTest extends AbstractWorkflowTest {

    @InjectUser(ref = "alice", config = DefaultUserConfig.class, lifecycle = LifeCycle.METHOD, realmRef = DEFAULT_REALM_NAME)
    private ManagedUser userAlice;

    @InjectMailServer
    private MailServer mailServer;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectAdminClient(ref = "managed", realmRef = "managedRealm")
    Keycloak adminClient;

    @Test
    public void testCreate() {
        WorkflowRepresentation expectedWorkflow = WorkflowRepresentation.withName("myworkflow")
                .onEvent(USER_CREATED.name())
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build()
                ).build();

        WorkflowsResource workflows = managedRealm.admin().workflows();

        try (Response response = workflows.create(expectedWorkflow)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        }

        List<WorkflowRepresentation> actualWorkflows = workflows.list();
        assertThat(actualWorkflows, hasSize(1));

        assertThat(actualWorkflows.get(0).getSteps(), hasSize(2));
        assertThat(actualWorkflows.get(0).getSteps().get(0).getUses(), is(NotifyUserStepProviderFactory.ID));
        assertThat(actualWorkflows.get(0).getSteps().get(1).getUses(), is(DisableUserStepProviderFactory.ID));
        assertThat(actualWorkflows.get(0).getState(), is(nullValue()));
    }

    @Test
    public void testCreateWithNoConditions() {
        WorkflowRepresentation expectedWorkflow = WorkflowRepresentation.withName("myworkflow")
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build()
                ).build();

        expectedWorkflow.setConditions(null);

        try (Response response = managedRealm.admin().workflows().create(expectedWorkflow)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        }
    }

    @Test
    public void testCreateWithNoWorkflowSetDefaultWorkflow() {
        WorkflowRepresentation expectedWorkflow = WorkflowRepresentation.withName("default-workflow")
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build()
                ).build();

        expectedWorkflow.setConditions(null);

        try (Response response = managedRealm.admin().workflows().create(expectedWorkflow)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        }

        assertEquals(1, managedRealm.admin().workflows().list().size());
    }

    @Test
    public void testDelete() {
        WorkflowsResource workflows = managedRealm.admin().workflows();

        String workflowId;
        try (Response response = workflows.create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(ResourceOperationType.USER_CREATED.toString())
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        WorkflowStepRepresentation.create().of(RestartWorkflowStepProviderFactory.ID)
                                .build())
                .build())) {
            workflowId = ApiUtil.getCreatedId(response);
        }

        workflows.create(WorkflowRepresentation.withName("another-workflow")
                .onEvent(ResourceOperationType.USER_AUTHENTICATED.toString())
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        WorkflowStepRepresentation.create().of(RestartWorkflowStepProviderFactory.ID)
                                .build()
                ).build()).close();

        // create a new user - should bind the user to the workflow and setup the only step in the workflow
        managedRealm.admin().users().create(UserConfigBuilder.create().username("testuser").email("testuser@example.com").build()).close();

        List<WorkflowRepresentation> actualWorkflows = workflows.list();
        assertThat(actualWorkflows, hasSize(2));

        workflows.workflow(workflowId).delete().close();
        actualWorkflows = workflows.list();
        assertThat(actualWorkflows, hasSize(1));

        runOnServer.run((RunOnServer) session -> {
            WorkflowProvider provider = session.getProvider(WorkflowProvider.class);

            List<Workflow> registeredWorkflows = provider.getWorkflows().toList();
            assertEquals(1, registeredWorkflows.size());
            WorkflowStateProvider stateProvider = session.getKeycloakSessionFactory().getProviderFactory(WorkflowStateProvider.class).create(session);
            List<ScheduledStep> steps = stateProvider.getScheduledStepsByWorkflow(workflowId).toList();
            assertTrue(steps.isEmpty());
        });
    }

    @Test
    public void testUpdateWorkflowWithNoScheduledSteps() {
        WorkflowRepresentation workflowRep = WorkflowRepresentation.withName("test-workflow")
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(10))
                                .build()
                ).build();

        WorkflowsResource workflows = managedRealm.admin().workflows();

        String workflowId;
        try (Response response = workflows.create(workflowRep)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
            workflowId = ApiUtil.getCreatedId(response);
        }

        List<WorkflowRepresentation> actualWorkflows = workflows.list();
        assertThat(actualWorkflows, hasSize(1));
        WorkflowRepresentation workflow = actualWorkflows.get(0);
        assertThat(workflow.getName(), is("test-workflow"));

        // while the workflow has no scheduled steps - i.e. no resource is currently going through the workflow - we can update any property
        workflow.setName("changed");
        workflow.setConditions(IdentityProviderWorkflowConditionFactory.ID + "(someidp)");
        workflow.setOn("user-authenticated");

        managedRealm.admin().workflows().workflow(workflow.getId()).update(workflow).close();
        workflow = workflows.workflow(workflow.getId()).toRepresentation();
        assertThat(workflow.getName(), is("changed"));
        assertThat(workflow.getOn(), is("user-authenticated"));
        assertThat(workflow.getConditions(), is(IdentityProviderWorkflowConditionFactory.ID + "(someidp)"));

        // even adding or removing steps should be allowed
        WorkflowStepRepresentation newStep = WorkflowStepRepresentation.create().of(DeleteUserStepProviderFactory.ID)
                .after(Duration.ofDays(10))
                .build();
        workflow.getSteps().remove(1); // remove the disable step
        workflow.getSteps().get(0).getConfig().putSingle("custom_message", "Your account will be disabled"); // change the notify step config
        workflow.getSteps().add(newStep);  // add a new delete step

        managedRealm.admin().workflows().workflow(workflow.getId()).update(workflow).close();
        workflow = workflows.workflow(workflow.getId()).toRepresentation();
        assertThat(workflow.getSteps(), hasSize(2));
        assertThat(workflow.getSteps().get(0).getUses(), is(NotifyUserStepProviderFactory.ID));
        assertThat(workflow.getSteps().get(0).getConfig().getFirst("custom_message"), is("Your account will be disabled"));
        assertThat(workflow.getSteps().get(1).getUses(), is(DeleteUserStepProviderFactory.ID));
    }

    @Test
    public void testUpdateWorkflowWithScheduledSteps() {
        WorkflowRepresentation expectedWorkflows = WorkflowRepresentation.withName("test-workflow")
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(10))
                                .build()
                ).build();

        WorkflowsResource workflows = managedRealm.admin().workflows();

        String workflowId;
        try (Response response = workflows.create(expectedWorkflows)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
            workflowId = ApiUtil.getCreatedId(response);
        }

        // bind the workflow to a resource, so it schedules the first step
        managedRealm.admin().workflows().workflow(workflowId).activate(ResourceType.USERS.name(), userAlice.getId());

        // when a scheduled step exists, we cannot change the 'on' event, nor the number or order of steps. Individual step config can still be updated, except for the 'uses'.
        WorkflowRepresentation workflow = managedRealm.admin().workflows().workflow(workflowId).toRepresentation();
        workflow.setName("changed");
        workflow.setConditions(IdentityProviderWorkflowConditionFactory.ID + "(someidp)");
        workflow.getSteps().get(0).getConfig().putSingle("custom_message", "Your account will be disabled"); // modify one of the steps config

        managedRealm.admin().workflows().workflow(workflow.getId()).update(workflow).close();
        workflow = workflows.workflow(workflow.getId()).toRepresentation();
        assertThat(workflow.getName(), is("changed"));
        assertThat(workflow.getConditions(), is(IdentityProviderWorkflowConditionFactory.ID + "(someidp)"));
        assertThat(workflow.getSteps().get(0).getConfig().getFirst("custom_message"), is("Your account will be disabled"));

        // now let's try to update the 'on' event - should fail
        workflow.setOn("user-logged-in");
        try (Response response = workflows.workflow(workflow.getId()).update(workflow)) {
            assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        }

        // restore the 'on' value, but try removing a step
        workflow.setOn(null);
        WorkflowStepRepresentation removedStep = workflow.getSteps().remove(1); // remove disable step
        try (Response response = workflows.workflow(workflow.getId()).update(workflow)) {
            assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        }

        // restore the step, but invert the order of the steps
        workflow.getSteps().add(0, removedStep);
        try (Response response = workflows.workflow(workflow.getId()).update(workflow)) {
            assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        }

        // restore the original order, but try changing the 'uses' of one step (i.e. replace it with something else)
        workflow.getSteps().remove(0); // this will put notify back as the first step.
        WorkflowStepRepresentation newStep = WorkflowStepRepresentation.create().of(DeleteUserStepProviderFactory.ID)
                .after(Duration.ofDays(10))
                .build();
        workflow.getSteps().add(newStep); // we've added a delete step in the place of the disable step, with same config
        try (Response response = workflows.workflow(workflow.getId()).update(workflow)) {
            assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        }
    }

    @Test
    public void testUpdateWorkflowConditionsCancelsExecutionForAffectedResources() {
        WorkflowRepresentation expectedWorkflows = WorkflowRepresentation.withName("test-workflow")
                .withSteps(
                        WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build()
                ).build();

        WorkflowsResource workflows = managedRealm.admin().workflows();

        String workflowId;
        try (Response response = workflows.create(expectedWorkflows)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
            workflowId = ApiUtil.getCreatedId(response);
        }

        // bind the workflow to a resource, so it schedules the first step
        managedRealm.admin().workflows().workflow(workflowId).activate(ResourceType.USERS.name(), userAlice.getId());

        // check step has been scheduled for the user
        List<WorkflowRepresentation> scheduledSteps = managedRealm.admin().workflows().getScheduledWorkflows(userAlice.getId());
        assertThat("A step should have been scheduled for the user " + userAlice.getUsername(), scheduledSteps, hasSize(1));

        // now update the workflow to add a condition that will make the user no longer eligible
        WorkflowRepresentation workflow = managedRealm.admin().workflows().workflow(workflowId).toRepresentation();
        workflow.setConditions(RoleWorkflowConditionFactory.ID + "(realm-management/realm-admin)");
        managedRealm.admin().workflows().workflow(workflowId).update(workflow).close();

        // simulate running the step - user should no longer be eligible, so the step should be cancelled
        runScheduledSteps(Duration.ofDays(6));

        // check the user is still enabled and no scheduled steps exist
        scheduledSteps = managedRealm.admin().workflows().getScheduledWorkflows(userAlice.getId());
        assertThat(scheduledSteps, empty());

    }

    @Test
    public void testSearch() {
        // create a few workflows with different names
        String[] workflowNames = {"alpha-workflow", "beta-workflow", "gamma-workflow", "delta-workflow"};
        for (String name : workflowNames) {
            managedRealm.admin().workflows().create(WorkflowRepresentation.withName(name)
                    .onEvent(ResourceOperationType.USER_CREATED.toString())
                    .withSteps(
                            WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                    .after(Duration.ofDays(5))
                                    .build()
                    ).build()).close();
        }

        // use the API to search for workflows by name, both partial and exact matches
        WorkflowsResource workflows = managedRealm.admin().workflows();
        List<WorkflowRepresentation> representations = workflows.list("alpha", false, null, null);
        assertThat(representations, hasSize(1));
        assertThat(representations.get(0).getName(), is("alpha-workflow"));

        representations = workflows.list("workflow", false, null, null);
        assertThat(representations, hasSize(4));
        representations = workflows.list("beta-workflow", true, null, null);
        assertThat(representations, hasSize(1));
        assertThat(representations.get(0).getName(), is("beta-workflow"));
        representations = workflows.list("nonexistent", false, null, null);
        assertThat(representations, hasSize(0));

        // test pagination parameters
        representations = workflows.list(null, null, 1, 2);
        assertThat(representations, hasSize(2));
        // returned workflows should be ordered by name
        assertThat(representations.get(0).getName(), is("beta-workflow"));
        assertThat(representations.get(1).getName(), is("delta-workflow"));

        representations = workflows.list("gamma", false, 0, 10);
        assertThat(representations, hasSize(1));
        assertThat(representations.get(0).getName(), is("gamma-workflow"));
    }

    @Test
    public void testGetActiveWorkflowsForResource() {
        // create a few of simple workflows
        String[] workflowNames = {"workflow-one", "workflow-two", "workflow-three", "workflow-four"};
        for (String name : workflowNames) {

            String workflowId;
            try (Response response =
            managedRealm.admin().workflows().create(WorkflowRepresentation.withName(name)
                    .withSteps(
                            WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                    .after(Duration.ofDays(5))
                                    .build(),
                            WorkflowStepRepresentation.create().of(SetUserAttributeStepProviderFactory.ID)
                                    .withConfig("key", "value")
                                    .build(),
                            WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                                    .after(Duration.ofDays(15))
                                    .build()
                    ).build())) {
                workflowId = ApiUtil.getCreatedId(response);
            }

            // bind all workflows, except the second one, to user alice
            if (!name.equals("workflow-two")) {
                managedRealm.admin().workflows().workflow(workflowId).activate(ResourceType.USERS.name(), userAlice.getId());
            }
        }

        // use the API to fetch the active workflows for the user
        List<WorkflowRepresentation> scheduledWorkflows = managedRealm.admin().workflows().getScheduledWorkflows(userAlice.getId());
        assertThat(scheduledWorkflows, hasSize(3));
        // assert that "workflow-two" is not among them
        assertTrue(scheduledWorkflows.stream().noneMatch(step -> step.getName().equals("workflow-two")));

        // assert that all workflows have the scheduledAt set to a positive value, and that the first and second steps are scheduled for the same time
        for (WorkflowRepresentation workflow : scheduledWorkflows) {
            assertThat(workflow.getSteps(), hasSize(3));
            assertThat(workflow.getSteps().get(0).getScheduledAt(), greaterThan(0L));
            assertThat(workflow.getSteps().get(1).getScheduledAt(), greaterThan(0L));
            assertThat(workflow.getSteps().get(0).getScheduledAt(), equalTo(workflow.getSteps().get(1).getScheduledAt()));
            // the third step have a scheduledAt higher than the previous two
            assertThat(workflow.getSteps().get(2).getScheduledAt(), greaterThan(workflow.getSteps().get(1).getScheduledAt()));
            // it should be precisely 15 days after the second step
            long expectedThirdStepScheduledAt = workflow.getSteps().get(1).getScheduledAt() + Duration.ofDays(15).toMillis();
            assertThat(workflow.getSteps().get(2).getScheduledAt(), is(expectedThirdStepScheduledAt));
        }
    }

    @Test
    public void testWorkflowDoesNotFallThroughStepsInSingleRun() {
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(ResourceOperationType.USER_CREATED.toString())
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
            ScheduledStep scheduledStep = stateProvider.getScheduledStep(workflow.getId(), user.getId());
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
                ScheduledStep scheduledStep = stateProvider.getScheduledStep(workflow.getId(), user.getId());
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

    @Test
    public void testAssignWorkflowToExistingResources() {
        // create some realm users
        for (int i = 0; i < 10; i++) {
            managedRealm.admin().users().create(UserConfigBuilder.create().username("user-" + i).build()).close();
        }

        // create some users associated with a federated identity
        for (int i = 0; i < 10; i++) {
            managedRealm.admin().users().create(UserConfigBuilder.create().username("idp-user-" + i)
                    .federatedLink("someidp", UUID.randomUUID().toString(), "idp-user-" + i).build()).close();
        }

        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
        idp.setAlias("someidp");
        idp.setProviderId(KeycloakOIDCIdentityProviderFactory.PROVIDER_ID);
        idp.setEnabled(true);
        managedRealm.admin().identityProviders().create(idp).close();

        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(ResourceOperationType.USER_FEDERATED_IDENTITY_ADDED.name())
                .onCondition(IdentityProviderWorkflowConditionFactory.ID + "(someidp)")
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(10))
                                .build()
                ).build()).close();

        // now with the workflow in place, let's create a couple more idp users - these will be attached to the workflow on
        // creation.
        for (int i = 0; i < 3; i++) {
            managedRealm.admin().users().create(UserConfigBuilder.create().username("new-idp-user-" + i)
                    .federatedLink("someidp", UUID.randomUUID().toString(), "new-idp-user-" + i).build()).close();
        }

        // new realm users created after the workflow - these should not be attached to the workflow because they are not idp users.
        for (int i = 0; i < 3; i++) {
            managedRealm.admin().users().create(UserConfigBuilder.create().username("new-user-" + i).build()).close();
        }

        runOnServer.run((RunOnServer) session -> {
            RealmModel realm = session.getContext().getRealm();
            WorkflowProvider provider = session.getProvider(WorkflowProvider.class);
            List<Workflow> registeredWorkflows = provider.getWorkflows().toList();
            assertEquals(1, registeredWorkflows.size());
            Workflow workflow = registeredWorkflows.get(0);

            List<WorkflowStep> steps = workflow.getSteps().toList();
            assertEquals(2, steps.size());
            WorkflowStep notifyStep = steps.get(0);

            // check no workflows are yet attached to the previous users, only to the ones created after the workflow was in place
            WorkflowStateProvider stateProvider = session.getKeycloakSessionFactory().getProviderFactory(WorkflowStateProvider.class).create(session);
            List<ScheduledStep> scheduledSteps = stateProvider.getScheduledStepsByWorkflow(workflow).toList();
            assertEquals(3, scheduledSteps.size());
            scheduledSteps.forEach(scheduledStep -> {
                assertEquals(notifyStep.getId(), scheduledStep.stepId());
                UserModel user = session.users().getUserById(realm, scheduledStep.resourceId());
                assertNotNull(user);
                assertTrue(user.getUsername().startsWith("new-idp-user-"));
            });
        });

        // let's run the schedule steps for the new users so they transition to the next one.
        runScheduledSteps(Duration.ofDays(6));


        runOnServer.run((RunOnServer) session -> {
            RealmModel realm = session.getContext().getRealm();
            // check the same users are now scheduled to run the second step.
            WorkflowProvider provider = session.getProvider(WorkflowProvider.class);
            List<Workflow> registeredWorkflows = provider.getWorkflows().toList();
            assertEquals(1, registeredWorkflows.size());
            Workflow workflow = registeredWorkflows.get(0);
            WorkflowStep disableStep = workflow.getSteps().toList().get(1);
            WorkflowStateProvider stateProvider = session.getKeycloakSessionFactory().getProviderFactory(WorkflowStateProvider.class).create(session);
            List<ScheduledStep> scheduledSteps = stateProvider.getScheduledStepsByWorkflow(workflow).toList();
            assertEquals(3, scheduledSteps.size());
            scheduledSteps.forEach(scheduledStep -> {
                assertEquals(disableStep.getId(), scheduledStep.stepId());
                UserModel user = session.users().getUserById(realm, scheduledStep.resourceId());
                assertNotNull(user);
                assertTrue(user.getUsername().startsWith("new-idp-user-"));
            });
        });

        List<WorkflowRepresentation> workflows = managedRealm.admin().workflows().list();
        assertThat(workflows, hasSize(1));
        // activate the workflow for all eligible users - i.e. only users from the same idp who are not yet assigned to the workflow.
        managedRealm.admin().workflows().workflow(workflows.get(0).getId()).activateAll();

        runOnServer.run((RunOnServer) session -> {
            RealmModel realm = session.getContext().getRealm();
            // check the same users are now scheduled to run the second step.
            WorkflowProvider provider = session.getProvider(WorkflowProvider.class);
            List<Workflow> registeredWorkflows = provider.getWorkflows().toList();
            assertEquals(1, registeredWorkflows.size());
            Workflow workflow = registeredWorkflows.get(0);
            // check workflow was correctly assigned to the old users, not affecting users already associated with the workflow.
            WorkflowStateProvider stateProvider = session.getProvider(WorkflowStateProvider.class);
            List<ScheduledStep> scheduledSteps = stateProvider.getScheduledStepsByWorkflow(workflow).toList();
            assertEquals(13, scheduledSteps.size());

            List<WorkflowStep> steps = workflow.getSteps().toList();
            assertEquals(2, steps.size());
            WorkflowStep notifyStep = steps.get(0);
            List<ScheduledStep> scheduledToNotify = scheduledSteps.stream()
                    .filter(step -> notifyStep.getId().equals(step.stepId())).toList();
            assertEquals(10, scheduledToNotify.size());
            scheduledToNotify.forEach(scheduledStep -> {
                UserModel user = session.users().getUserById(realm, scheduledStep.resourceId());
                assertNotNull(user);
                assertTrue(user.getUsername().startsWith("idp-user-"));
            });

            WorkflowStep disableStep = workflow.getSteps().toList().get(1);
            List<ScheduledStep> scheduledToDisable = scheduledSteps.stream()
                    .filter(step -> disableStep.getId().equals(step.stepId())).toList();
            assertEquals(3, scheduledToDisable.size());
            scheduledToDisable.forEach(scheduledStep -> {
                UserModel user = session.users().getUserById(realm, scheduledStep.resourceId());
                assertNotNull(user);
                assertTrue(user.getUsername().startsWith("new-idp-user-"));
            });
        });
    }

    @Test
    public void testDisableWorkflow() {
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
        managedRealm.admin().workflows().workflow(workflowId).update(workflow).close();

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
            List<ScheduledStep> scheduledSteps = stateProvider.getScheduledStepsByWorkflow(registeredWorkflow.get(0)).toList();

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
        workflow.getConfig().putSingle("enabled", "true");
        managedRealm.admin().workflows().workflow(workflowId).update(workflow).close();

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

    @Test
    public void testRestartWorkflow() {
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(ResourceOperationType.USER_CREATED.toString())
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
            ScheduledStep scheduledStep = stateProvider.getScheduledStep(workflow.getId(), user.getId());
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
            ScheduledStep scheduledStep = stateProvider.getScheduledStep(workflow.getId(), user.getId());
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
            ScheduledStep scheduledStep = stateProvider.getScheduledStep(workflow.getId(), user.getId());
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
                .onEvent(ResourceOperationType.USER_CREATED.toString())
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
            assertThat(response.getStatus(), is(Status.BAD_REQUEST.getStatusCode()));
            assertThat(response.readEntity(ErrorRepresentation.class).getErrorMessage(),
                    is("No scheduled step found if restarting at position 1"));
        }
        try (Response response = managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(ResourceOperationType.USER_CREATED.toString())
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
            assertThat(response.getStatus(), is(Status.BAD_REQUEST.getStatusCode()));
            assertThat(response.readEntity(ErrorRepresentation.class).getErrorMessage(),
                    is("No scheduled step found if restarting at position 2"));
        }
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(ResourceOperationType.USER_CREATED.toString())
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
            ScheduledStep scheduledStep = stateProvider.getScheduledStep(workflow.getId(), user.getId());
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
            ScheduledStep scheduledStep = stateProvider.getScheduledStep(workflow.getId(), user.getId());
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
            ScheduledStep scheduledStep = stateProvider.getScheduledStep(workflow.getId(), user.getId());
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
            ScheduledStep scheduledStep = stateProvider.getScheduledStep(workflow.getId(), user.getId());
            assertNotNull(scheduledStep, "A step should have been scheduled for the user " + user.getUsername());
            assertEquals(expectedStep.getId(), scheduledStep.stepId(), "The step should have been scheduled again");
            assertThat("The step should have been scheduled again at a later time", scheduledStep.scheduledAt(), greaterThan(reScheduledAtLast));
        });

        mailServer.runCleanup();
    }

    @Test
    public void testRestartFromLastStep() {
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(ResourceOperationType.USER_CREATED.toString())
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
            ScheduledStep scheduledStep = stateProvider.getScheduledStep(workflow.getId(), user.getId());
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
            ScheduledStep scheduledStep = stateProvider.getScheduledStep(workflow.getId(), user.getId());
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
            ScheduledStep scheduledStep = stateProvider.getScheduledStep(workflow.getId(), user.getId());
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
            ScheduledStep scheduledStep = stateProvider.getScheduledStep(workflow.getId(), user.getId());
            assertNotNull(scheduledStep, "A step should have been scheduled for the user " + user.getUsername());
            assertEquals(expectedStep.getId(), scheduledStep.stepId(), "The step should have been scheduled again");
            assertThat("The step should have been scheduled again at a later time", scheduledStep.scheduledAt(), greaterThan(reScheduledAtLast));
        });
        mailServer.runCleanup();
    }

    @Test
    public void testRunImmediateWorkflow() {
        // create a test workflow with no time conditions - should run immediately when scheduled
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(USER_CREATED.name())
                .withSteps(
                        WorkflowStepRepresentation.create().of(SetUserAttributeStepProviderFactory.ID)
                                .withConfig("message", "message")
                                .build(),
                        WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                                .build()
                ).build()).close();

        // create a new user - should be bound to the new workflow and all steps should run right away
        managedRealm.admin().users().create(UserConfigBuilder.create().username("testuser").build()).close();

        // check the user has the attribute set and is disabled
        runOnServer.run(session -> {
            UserModel user = session.users().getUserByUsername(session.getContext().getRealm(), "testuser");
            assertThat(user, notNullValue());
            assertThat(user.getAttributes(), notNullValue());
            assertThat(user.getAttributes().get("message"), notNullValue());
            assertThat(user.getAttributes().get("message").get(0), is("message"));
            assertFalse(user.isEnabled());
        });
    }

    @Test
    public void testFailCreateWorkflowWithNegativeTime() {
        WorkflowRepresentation workflow = WorkflowRepresentation.withName("myworkflow")
                .onEvent(USER_CREATED.name())
                .withSteps(
                        WorkflowStepRepresentation.create().of(SetUserAttributeStepProviderFactory.ID)
                                .after(Duration.ofDays(-5))
                                .withConfig("key", "value")
                                .build())
                .build();
        try (Response response = managedRealm.admin().workflows().create(workflow)) {
            assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
            assertThat(response.readEntity(ErrorRepresentation.class).getErrorMessage(), equalTo("Step 'after' configuration cannot be negative."));
        }
    }

    @Test
    public void testNotifyUserStepSendsEmailWithDefaultDisableMessage() {
        // Create workflow: disable at 10 days, notify 3 days before (at day 7)
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(USER_CREATED.name())
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(7))
                                .withConfig("reason", "inactivity")
                                .build(),
                        WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(3))
                                .build()
                ).build()).close();

        managedRealm.admin().users().create(UserConfigBuilder.create().username("testuser").email("test@example.com").name("John", "").build()).close();

        // Simulate user being 7 days old (eligible for notify step)
        runScheduledSteps(Duration.ofDays(7));

        // Verify email was sent to our test user
        MimeMessage testUserMessage = findEmailByRecipient(mailServer, "test@example.com");
        assertNotNull(testUserMessage, "No email found for test@example.com");
        verifyEmailContent(testUserMessage, "test@example.com", "Disable", "John", "3", "inactivity");

        mailServer.runCleanup();
    }

    @Test
    public void testNotifyUserStepSendsEmailWithDefaultDeleteMessage() {
        // Create workflow: delete at 30 days, notify 15 days before (at day 15)
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(USER_CREATED.name())
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(15))
                                .withConfig("reason", "inactivity")
                                .build(),
                        WorkflowStepRepresentation.create().of(DeleteUserStepProviderFactory.ID)
                                .after(Duration.ofDays(15))
                                .build()
                ).build()).close();

        managedRealm.admin().users().create(UserConfigBuilder.create().username("testuser2").email("test2@example.com").name("Jane", "").build()).close();

        // Simulate user being 15 days old
        runScheduledSteps(Duration.ofDays(15));

        // Verify email was sent to our test user
        MimeMessage testUserMessage = findEmailByRecipient(mailServer, "test2@example.com");
        assertNotNull(testUserMessage, "No email found for test2@example.com");
        verifyEmailContent(testUserMessage, "test2@example.com", "Deletion", "Jane", "15", "inactivity", "permanently deleted");

        mailServer.runCleanup();
    }

    @Test
    public void testNotifyUserStepWithCustomMessageOverride() {
        // Create workflow: disable at 7 days, notify 2 days before (at day 5) with custom message
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(USER_CREATED.name())
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .withConfig("reason", "compliance requirement")
                                .withConfig("message", "${user.firstName}, your account requires immediate attention due to new compliance policies.")
                                .withConfig("subject", "customComplianceSubject")
                                .build(),
                        WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(7))
                                .build()
                ).build()).close();

        managedRealm.admin().users().create(UserConfigBuilder.create().username("testuser3").email("test3@example.com").name("Bob", "").build()).close();

        // Simulate user being 5 days old
        runScheduledSteps(Duration.ofDays(5));

        // Verify email was sent to our test user
        MimeMessage testUserMessage = findEmailByRecipient(mailServer, "test3@example.com");
        assertNotNull(testUserMessage, "No email found for test3@example.com");
        verifyEmailContent(testUserMessage, "test3@example.com", "", "Bob", "2", "immediate attention due to new compliance policies");

        mailServer.runCleanup();
    }

    @Test
    public void testNotifyUserStepSkipsUsersWithoutEmailButLogsWarning() {
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(USER_CREATED.name())
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(10))
                                .build()
                ).build()).close();

        String userId;
        try (Response response = managedRealm.admin().users().create(UserConfigBuilder.create().username("testuser4").name("NoEmail", "").build())) {
            userId = ApiUtil.getCreatedId(response);
        }

        runScheduledSteps(Duration.ofDays(5));
        List<WorkflowRepresentation> scheduledWorkflows = managedRealm.admin().workflows().getScheduledWorkflows(userId);
        assertThat(scheduledWorkflows, hasSize(1));
        List<WorkflowStepRepresentation> steps = scheduledWorkflows.get(0).getSteps();
        assertThat(steps, hasSize(1));
        assertEquals(DisableUserStepProviderFactory.ID, steps.get(0).getUses());

        // Should NOT send email to user without email address
        MimeMessage testUserMessage = findEmailByRecipientContaining("testuser4");
        assertNull(testUserMessage, "No email should be sent to user without email address");
    }

    @Test
    public void testCompleteUserLifecycleWithMultipleNotifications() {
        // Create workflow: just disable at 30 days with one notification before
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(USER_CREATED.name())
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(15))
                                .withConfig("reason", "inactivity")
                                .build(),
                        WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(15))
                                .build()
                ).build()).close();

        managedRealm.admin().users().create(UserConfigBuilder.create().username("testuser5").email("testuser5@example.com").name("TestUser5", "").build()).close();

        // Day 15: First notification - this should run the notify step and schedule the disable step
        runScheduledSteps(Duration.ofDays(15));

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, "testuser5");
            // Check that user is still enabled after notification
            user = session.users().getUserById(realm, user.getId());
            assertTrue(user.isEnabled(), "User should still be enabled after notification");
        });

        // Day 30 + 15 minutes: Disable user - run 15 minutes after the scheduled time to ensure it's due
        runScheduledSteps(Duration.ofDays(30).plus(Duration.ofMinutes(15)));

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, "testuser5");
            // Verify user is disabled
            user = session.users().getUserById(realm, user.getId());
            assertNotNull(user, "User should still exist after disable");
            assertFalse(user.isEnabled(), "User should be disabled");
        });

        // Verify notification was sent
        MimeMessage testUserMessage = findEmailByRecipient(mailServer, "testuser5@example.com");
        assertNotNull(testUserMessage, "No email found for testuser5@example.com");
        verifyEmailContent(testUserMessage, "testuser5@example.com", "Disable", "TestUser5", "15", "inactivity");

        mailServer.runCleanup();
    }

    @Test
    public void testCreateUsingYaml() throws IOException {
        YAMLMapper yamlMapper = YAMLMapper.builder().serializationInclusion(Include.NON_NULL).build();
        WorkflowRepresentation expected = WorkflowRepresentation.withName("test")
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build()
                ).build();

        try (Client httpClient = Keycloak.getClientProvider().newRestEasyClient(null, null, true)) {
            httpClient.register(JacksonYAMLProvider.class);
            WebTarget workflowsApi = httpClient.target(keycloakUrls.getBaseUrl().toString())
                    .path("admin")
                    .path("realms")
                    .path(managedRealm.getName())
                    .path("workflows")
                    .register(new BearerAuthFilter(adminClient.tokenManager()));

            String workflowId;
            try (Response response = workflowsApi.request().post(Entity.entity(yamlMapper.writeValueAsString(expected),
                    YAMLMediaTypes.APPLICATION_JACKSON_YAML))) {
                assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
                workflowId = ApiUtil.getCreatedId(response);
            }

            try (Response response = workflowsApi.request().accept(YAMLMediaTypes.APPLICATION_JACKSON_YAML).get()) {
                assertEquals(Status.OK.getStatusCode(), response.getStatus());
                List<WorkflowRepresentation> workflows = yamlMapper.readValue(response.readEntity(String.class),
                        new TypeReference<>() {
                        });
                assertFalse(workflows.isEmpty());
                expected = workflows.get(0);
            }

            try (Response response = workflowsApi.path(workflowId).request()
                    .accept(YAMLMediaTypes.APPLICATION_JACKSON_YAML).get()) {
                assertEquals(Status.OK.getStatusCode(), response.getStatus());
                WorkflowRepresentation actual = yamlMapper.readValue(response.readEntity(String.class), WorkflowRepresentation.class);
                assertEquals(expected.getName(), actual.getName());
            }

            try (Response response = workflowsApi.path(workflowId).request()
                    .put(Entity.entity(yamlMapper.writeValueAsString(expected), YAMLMediaTypes.APPLICATION_JACKSON_YAML))) {
                assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
            }
        }
    }

    public static List<MimeMessage> findEmailsByRecipient(MailServer mailServer, String expectedRecipient) {
        return Arrays.stream(mailServer.getReceivedMessages())
                .filter(msg -> {
                    try {
                        return MailUtils.getRecipient(msg).equals(expectedRecipient);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .toList();
    }

    public static MimeMessage findEmailByRecipient(MailServer mailServer, String expectedRecipient) {
        return Arrays.stream(mailServer.getReceivedMessages())
                .filter(msg -> {
                    try {
                        return MailUtils.getRecipient(msg).equals(expectedRecipient);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .findFirst()
                .orElse(null);
    }

    private MimeMessage findEmailByRecipientContaining(String recipientPart) {
        return Arrays.stream(mailServer.getReceivedMessages())
                .filter(msg -> {
                    try {
                        return MailUtils.getRecipient(msg).contains(recipientPart);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .findFirst()
                .orElse(null);
    }

    public static void verifyEmailContent(MimeMessage message, String expectedRecipient, String subjectContains,
                                          String... contentContains) {
        try {
            assertEquals(expectedRecipient, MailUtils.getRecipient(message));
            assertThat(message.getSubject(), Matchers.containsString(subjectContains));

            MailUtils.EmailBody body = MailUtils.getBody(message);
            String textContent = body.getText();
            String htmlContent = body.getHtml();

            for (String expectedContent : contentContains) {
                boolean foundInText = textContent.contains(expectedContent);
                boolean foundInHtml = htmlContent.contains(expectedContent);
                assertTrue(foundInText || foundInHtml,
                        "Email content should contain: " + expectedContent +
                                "\nText: " + textContent +
                                "\nHTML: " + htmlContent);
            }
        } catch (MessagingException | IOException e) {
            Assertions.fail("Failed to read email message: " + e.getMessage());
        }
    }

    private static class DefaultUserConfig implements UserConfig {

        @Override
        public UserConfigBuilder configure(UserConfigBuilder user) {
            user.username("alice");
            user.password("alice");
            user.name("alice", "alice");
            user.email("master-admin@email.org");
            return user;
        }
    }
}
