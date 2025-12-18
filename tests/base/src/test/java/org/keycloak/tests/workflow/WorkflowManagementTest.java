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
import java.util.List;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.BearerAuthFilter;
import org.keycloak.admin.client.resource.WorkflowsResource;
import org.keycloak.models.workflow.DeleteUserStepProviderFactory;
import org.keycloak.models.workflow.DisableUserStepProviderFactory;
import org.keycloak.models.workflow.NotifyUserStepProviderFactory;
import org.keycloak.models.workflow.ResourceOperationType;
import org.keycloak.models.workflow.ResourceType;
import org.keycloak.models.workflow.RestartWorkflowStepProviderFactory;
import org.keycloak.models.workflow.SetUserAttributeStepProviderFactory;
import org.keycloak.models.workflow.WorkflowStateProvider;
import org.keycloak.models.workflow.WorkflowStateProvider.ScheduledStep;
import org.keycloak.models.workflow.conditions.IdentityProviderWorkflowConditionFactory;
import org.keycloak.models.workflow.conditions.RoleWorkflowConditionFactory;
import org.keycloak.representations.idm.ErrorRepresentation;
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
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.workflow.config.WorkflowsBlockingServerConfig;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.jakarta.rs.yaml.JacksonYAMLProvider;
import com.fasterxml.jackson.jakarta.rs.yaml.YAMLMediaTypes;
import org.junit.jupiter.api.Test;

import static org.keycloak.models.workflow.ResourceOperationType.USER_AUTHENTICATED;
import static org.keycloak.models.workflow.ResourceOperationType.USER_CREATED;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests CRUD operations on workflows through the API.
 */
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

        List<WorkflowRepresentation> workflows = managedRealm.admin().workflows().list();
        assertThat(workflows, hasSize(1));
        WorkflowRepresentation workflow = workflows.get(0);
        assertThat(workflow.getSteps(), hasSize(2));
        assertThat(workflow.getSteps().get(0).getUses(), is(NotifyUserStepProviderFactory.ID));
        assertThat(workflow.getSteps().get(1).getUses(), is(DisableUserStepProviderFactory.ID));
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
    public void testFailCreateWorkflowWithDuplicateName() {
        // create first workflow
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(USER_CREATED.name())
                .withSteps(
                        WorkflowStepRepresentation.create().of(SetUserAttributeStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .withConfig("key", "value")
                                .build())
                .build()).close();

        // try to create second workflow with same name
        try (Response response = managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(USER_AUTHENTICATED.name())
                .withSteps(
                        WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(10))
                                .build())
                .build())) {
            assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
            assertThat(response.readEntity(ErrorRepresentation.class).getErrorMessage(),
                    equalTo("Workflow name must be unique. A workflow with name 'myworkflow' already exists."));
        }

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

        // create a new user - should bind the user to the first workflow.
        String userId;
        try(Response response = managedRealm.admin().users().create(UserConfigBuilder.create().username("testuser")
                .email("testuser@example.com").build())) {
            userId = ApiUtil.getCreatedId(response);
        }

        runOnServer.run((RunOnServer) session -> {
            // the workflow state table should have a scheduled step for the user in the first workflow
            WorkflowStateProvider stateProvider = session.getKeycloakSessionFactory().getProviderFactory(WorkflowStateProvider.class).create(session);
            List<ScheduledStep> steps = stateProvider.getScheduledStepsByWorkflow(workflowId).toList();
            assertThat(steps, hasSize(1));
            assertThat(steps.get(0).resourceId(), is(userId));
        });

        // delete the first workflow - should also delete all scheduled steps
        workflows.workflow(workflowId).delete().close();
        List<WorkflowRepresentation> availableWorkflows = workflows.list();
        assertThat(availableWorkflows, hasSize(1));
        assertThat(availableWorkflows.get(0).getName(), equalTo("another-workflow"));

        runOnServer.run((RunOnServer) session -> {
            // the workflow state table should have no scheduled steps for the deleted workflow
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

        try (Response response = workflows.create(workflowRep)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
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
