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

package org.keycloak.tests.admin.model.workflow;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.core.Response;
import org.hamcrest.Matchers;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.WorkflowsResource;
import org.keycloak.broker.oidc.KeycloakOIDCIdentityProviderFactory;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.workflow.DeleteUserStepProviderFactory;
import org.keycloak.models.workflow.DisableUserStepProviderFactory;
import org.keycloak.models.workflow.EventBasedWorkflowProviderFactory;
import org.keycloak.models.workflow.NotifyUserStepProviderFactory;
import org.keycloak.models.workflow.WorkflowStep;
import org.keycloak.models.workflow.ResourceOperationType;
import org.keycloak.models.workflow.Workflow;
import org.keycloak.models.workflow.WorkflowsManager;
import org.keycloak.models.workflow.WorkflowStateProvider;
import org.keycloak.models.workflow.WorkflowStateProvider.ScheduledStep;
import org.keycloak.models.workflow.SetUserAttributeStepProviderFactory;
import org.keycloak.models.workflow.UserCreationTimeWorkflowProviderFactory;
import org.keycloak.models.workflow.conditions.IdentityProviderWorkflowConditionFactory;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.workflows.WorkflowConstants;
import org.keycloak.representations.workflows.WorkflowSetRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.representations.workflows.WorkflowConditionRepresentation;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.tests.utils.MailUtils;

@KeycloakIntegrationTest(config = WorkflowsServerConfig.class)
public class WorkflowManagementTest {

    private static final String REALM_NAME = "default";

    @InjectRunOnServer(permittedPackages = {"org.keycloak.tests", "org.hamcrest"})
    RunOnServerClient runOnServer;

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @InjectMailServer
    private MailServer mailServer;

    @Test
    public void testCreate() {
        WorkflowSetRepresentation expectedWorkflows = WorkflowRepresentation.create()
                .of(UserCreationTimeWorkflowProviderFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build()
                ).build();

        WorkflowsResource workflows = managedRealm.admin().workflows();

        try (Response response = workflows.create(expectedWorkflows)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        }

        List<WorkflowRepresentation> actualWorkflows = workflows.list();
        assertThat(actualWorkflows, Matchers.hasSize(1));

        assertThat(actualWorkflows.get(0).getUses(), is(UserCreationTimeWorkflowProviderFactory.ID));
        assertThat(actualWorkflows.get(0).getSteps(), Matchers.hasSize(2));
        assertThat(actualWorkflows.get(0).getSteps().get(0).getUses(), is(NotifyUserStepProviderFactory.ID));
        assertThat(actualWorkflows.get(0).getSteps().get(1).getUses(), is(DisableUserStepProviderFactory.ID));
        assertThat(actualWorkflows.get(0).getState(), is(nullValue()));
    }

    @Test
    public void testCreateWithNoConditions() {
        WorkflowSetRepresentation expectedWorkflows = WorkflowRepresentation.create()
                .of(EventBasedWorkflowProviderFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build()
                ).build();

        expectedWorkflows.getWorkflows().get(0).setConditions(null);

        try (Response response = managedRealm.admin().workflows().create(expectedWorkflows)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        }
    }

    @Test
    public void testCreateWithNoWorkflowSetDefaultWorkflow() {
        WorkflowSetRepresentation expectedWorkflows = WorkflowRepresentation.create()
                .of(null)
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build()
                ).build();

        expectedWorkflows.getWorkflows().get(0).setConditions(null);

        try (Response response = managedRealm.admin().workflows().create(expectedWorkflows)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        }

        assertEquals(1, managedRealm.admin().workflows().list().size());
        assertEquals(WorkflowConstants.DEFAULT_WORKFLOW, managedRealm.admin().workflows().list().get(0).getUses());
    }

    @Test
    public void testDelete() {
        WorkflowsResource workflows = managedRealm.admin().workflows();

        workflows.create(WorkflowRepresentation.create()
                .of(UserCreationTimeWorkflowProviderFactory.ID)
                .onEvent(ResourceOperationType.USER_ADD.toString())
                .recurring()
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build()
                ).of(EventBasedWorkflowProviderFactory.ID)
                .onEvent(ResourceOperationType.USER_LOGIN.toString())
                .recurring()
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build()
                ).build()).close();

        // create a new user - should bind the user to the workflow and setup the only step in the workflow
        managedRealm.admin().users().create(UserConfigBuilder.create().username("testuser").email("testuser@example.com").build()).close();

        List<WorkflowRepresentation> actualWorkflows = workflows.list();
        assertThat(actualWorkflows, Matchers.hasSize(2));

        WorkflowRepresentation workflow = actualWorkflows.stream().filter(p -> UserCreationTimeWorkflowProviderFactory.ID.equals(p.getUses())).findAny().orElse(null);
        assertThat(workflow, notNullValue());
        String id = workflow.getId();
        workflows.workflow(id).delete().close();
        actualWorkflows = workflows.list();
        assertThat(actualWorkflows, Matchers.hasSize(1));

        runOnServer.run((RunOnServer) session -> {
            configureSessionContext(session);
            WorkflowsManager manager = new WorkflowsManager(session);

            List<Workflow> registeredWorkflows = manager.getWorkflows();
            assertEquals(1, registeredWorkflows.size());
            WorkflowStateProvider stateProvider = session.getKeycloakSessionFactory().getProviderFactory(WorkflowStateProvider.class).create(session);
            List<ScheduledStep> steps = stateProvider.getScheduledStepsByWorkflow(id);
            assertTrue(steps.isEmpty());
        });
    }

    @Test
    public void testUpdate() {
        WorkflowSetRepresentation expectedWorkflows = WorkflowRepresentation.create()
                .of(UserCreationTimeWorkflowProviderFactory.ID)
                .name("test-workflow")
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(10))
                                .build()
                ).build();

        WorkflowsResource workflows = managedRealm.admin().workflows();

        try (Response response = workflows.create(expectedWorkflows)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        }

        List<WorkflowRepresentation> actualWorkflows = workflows.list();
        assertThat(actualWorkflows, Matchers.hasSize(1));
        WorkflowRepresentation workflow = actualWorkflows.get(0);
        assertThat(workflow.getName(), is("test-workflow"));

        workflow.setName("changed");
        managedRealm.admin().workflows().workflow(workflow.getId()).update(workflow).close();
        actualWorkflows = workflows.list();
        workflow = actualWorkflows.get(0);
        assertThat(workflow.getName(), is("changed"));

        // now let's try to update another property that we can't update
        String previousOn = workflow.getOn();
        workflow.setOn(ResourceOperationType.USER_LOGIN.toString());
        try (Response response = workflows.workflow(workflow.getId()).update(workflow)) {
            assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        }

        // restore previous value, but change the conditions
        workflow.setOn(previousOn);
        workflow.setConditions(Collections.singletonList(
                WorkflowConditionRepresentation.create().of(IdentityProviderWorkflowConditionFactory.ID)
                        .withConfig(IdentityProviderWorkflowConditionFactory.EXPECTED_ALIASES, "someidp")
                        .build()
        ));
        try (Response response = workflows.workflow(workflow.getId()).update(workflow)) {
            assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        }

        // revert conditions, but change one of the steps
        workflow.setConditions(null);
        workflow.getSteps().get(0).setAfter(Duration.ofDays(8).toMillis());
        try (Response response = workflows.workflow(workflow.getId()).update(workflow)) {
            assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        }

    }

    @Test
    public void testWorkflowDoesNotFallThroughStepsInSingleRun() {
        managedRealm.admin().workflows().create(WorkflowRepresentation.create()
                .of(UserCreationTimeWorkflowProviderFactory.ID)
                .onEvent(ResourceOperationType.USER_ADD.toString())
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
            RealmModel realm = configureSessionContext(session);
            WorkflowsManager manager = new WorkflowsManager(session);
            UserModel user = session.users().getUserByUsername(realm,"testuser");

            List<Workflow> registeredWorkflows = manager.getWorkflows();
            assertEquals(1, registeredWorkflows.size());

            Workflow workflow = registeredWorkflows.get(0);
            assertEquals(2, manager.getSteps(workflow.getId()).size());
            WorkflowStep notifyStep = manager.getSteps(workflow.getId()).get(0);

            WorkflowStateProvider stateProvider = session.getProvider(WorkflowStateProvider.class);
            ScheduledStep scheduledStep = stateProvider.getScheduledStep(workflow.getId(), user.getId());
            assertNotNull(scheduledStep, "A step should have been scheduled for the user " + user.getUsername());
            assertEquals(notifyStep.getId(), scheduledStep.stepId());

            try {
                // Simulate the user being 12 days old, making them eligible for both steps' time conditions.
                Time.setOffset(Math.toIntExact(Duration.ofDays(12).toSeconds()));
                manager.runScheduledSteps();

                user = session.users().getUserById(realm, user.getId());

                // Verify that the next step was scheduled for the user
                WorkflowStep disableStep = manager.getSteps(workflow.getId()).get(1);
                scheduledStep = stateProvider.getScheduledStep(workflow.getId(), user.getId());
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

        managedRealm.admin().workflows().create(WorkflowRepresentation.create()
                .of(UserCreationTimeWorkflowProviderFactory.ID)
                .onEvent(ResourceOperationType.USER_FEDERATED_IDENTITY_ADD.name())
                .onConditions(WorkflowConditionRepresentation.create()
                        .of(IdentityProviderWorkflowConditionFactory.ID)
                        .withConfig(IdentityProviderWorkflowConditionFactory.EXPECTED_ALIASES, "someidp")
                        .build())
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
            RealmModel realm = configureSessionContext(session);
            WorkflowsManager workflowsManager = new WorkflowsManager(session);
            List<Workflow> registeredWorkflows = workflowsManager.getWorkflows();
            assertEquals(1, registeredWorkflows.size());
            Workflow workflow = registeredWorkflows.get(0);

            assertEquals(2, workflowsManager.getSteps(workflow.getId()).size());
            WorkflowStep notifyStep = workflowsManager.getSteps(workflow.getId()).get(0);

            // check no workflows are yet attached to the previous users, only to the ones created after the workflow was in place
            WorkflowStateProvider stateProvider = session.getKeycloakSessionFactory().getProviderFactory(WorkflowStateProvider.class).create(session);
            List<ScheduledStep> scheduledSteps = stateProvider.getScheduledStepsByWorkflow(workflow);
            assertEquals(3, scheduledSteps.size());
            scheduledSteps.forEach(scheduledStep -> {
                assertEquals(notifyStep.getId(), scheduledStep.stepId());
                UserModel user = session.users().getUserById(realm, scheduledStep.resourceId());
                assertNotNull(user);
                assertTrue(user.getUsername().startsWith("new-idp-user-"));
            });

            try {
                // let's run the schedule steps for the new users so they transition to the next one.
                Time.setOffset(Math.toIntExact(Duration.ofDays(6).toSeconds()));
                workflowsManager.runScheduledSteps();

                // check the same users are now scheduled to run the second step.
                WorkflowStep disableStep = workflowsManager.getSteps(workflow.getId()).get(1);
                scheduledSteps = stateProvider.getScheduledStepsByWorkflow(workflow);
                assertEquals(3, scheduledSteps.size());
                scheduledSteps.forEach(scheduledStep -> {
                    assertEquals(disableStep.getId(), scheduledStep.stepId());
                    UserModel user = session.users().getUserById(realm, scheduledStep.resourceId());
                    assertNotNull(user);
                    assertTrue(user.getUsername().startsWith("new-idp-user-"));
                });

                // assign the workflow to the eligible users - i.e. only users from the same idp who are not yet assigned to the workflow.
                workflowsManager.scheduleAllEligibleResources(workflow);

                // check workflow was correctly assigned to the old users, not affecting users already associated with the workflow.
                scheduledSteps = stateProvider.getScheduledStepsByWorkflow(workflow);
                assertEquals(13, scheduledSteps.size());

                List<ScheduledStep> scheduledToNotify = scheduledSteps.stream()
                        .filter(step -> notifyStep.getId().equals(step.stepId())).toList();
                assertEquals(10, scheduledToNotify.size());
                scheduledToNotify.forEach(scheduledStep -> {
                    UserModel user = session.users().getUserById(realm, scheduledStep.resourceId());
                    assertNotNull(user);
                    assertTrue(user.getUsername().startsWith("idp-user-"));
                });

                List<ScheduledStep> scheduledToDisable = scheduledSteps.stream()
                        .filter(step -> disableStep.getId().equals(step.stepId())).toList();
                assertEquals(3, scheduledToDisable.size());
                scheduledToDisable.forEach(scheduledStep -> {
                    UserModel user = session.users().getUserById(realm, scheduledStep.resourceId());
                    assertNotNull(user);
                    assertTrue(user.getUsername().startsWith("new-idp-user-"));
                });

            } finally {
                Time.setOffset(0);
            }
        });
    }

    @Test
    public void testDisableWorkflow() {
        // create a test workflow
        managedRealm.admin().workflows().create(WorkflowRepresentation.create()
                .of(UserCreationTimeWorkflowProviderFactory.ID)
                .onEvent(ResourceOperationType.USER_ADD.toString())
                .name("test-workflow")
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build()
                ).build()).close();

        WorkflowsResource workflows = managedRealm.admin().workflows();
        List<WorkflowRepresentation> actualWorkflows = workflows.list();
        assertThat(actualWorkflows, Matchers.hasSize(1));
        WorkflowRepresentation workflow = actualWorkflows.get(0);
        assertThat(workflow.getName(), is("test-workflow"));

        // create a new user - should bind the user to the workflow and setup the first step
        managedRealm.admin().users().create(UserConfigBuilder.create().username("testuser").email("testuser@example.com").build()).close();

        runOnServer.run((RunOnServer) session -> {
            RealmModel realm = configureSessionContext(session);
            WorkflowsManager manager = new WorkflowsManager(session);

            try {
                // Advance time so the user is eligible for the first step, then run the scheduled steps so they transition to the next one.
                Time.setOffset(Math.toIntExact(Duration.ofDays(6).toSeconds()));
                manager.runScheduledSteps();

                UserModel user = session.users().getUserByUsername(realm, "testuser");
                assertTrue(user.isEnabled(), "The second step (disable) should NOT have run.");
            } finally {
                Time.setOffset(0);
            }
        });

        // Verify that the first step (notify) was executed by checking email was sent
        MimeMessage testUserMessage = findEmailByRecipient(mailServer, "testuser@example.com");
        assertNotNull(testUserMessage, "The first step (notify) should have sent an email.");

        mailServer.runCleanup();

        // disable the workflow - scheduled steps should be paused and workflow should not activate for new users
        workflow.setEnabled(false);
        managedRealm.admin().workflows().workflow(workflow.getId()).update(workflow).close();

        // create another user - should NOT bind the user to the workflow as it is disabled
        managedRealm.admin().users().create(UserConfigBuilder.create().username("anotheruser").build()).close();

        runOnServer.run((RunOnServer) session -> {
            RealmModel realm = configureSessionContext(session);
            WorkflowsManager manager = new WorkflowsManager(session);

            List<Workflow> registeredWorkflow = manager.getWorkflows();
            assertEquals(1, registeredWorkflow.size());
            WorkflowStateProvider stateProvider = session.getKeycloakSessionFactory().getProviderFactory(WorkflowStateProvider.class).create(session);
            List<ScheduledStep> scheduledSteps = stateProvider.getScheduledStepsByWorkflow(registeredWorkflow.get(0));

            // verify that there's only one scheduled step, for the first user
            assertEquals(1, scheduledSteps.size());
            UserModel scheduledStepUser = session.users().getUserById(realm, scheduledSteps.get(0).resourceId());
            assertNotNull(scheduledStepUser);
            assertTrue(scheduledStepUser.getUsername().startsWith("testuser"));

            try {
                // Advance time so the first user would be eligible for the second step, then run the scheduled steps.
                Time.setOffset(Math.toIntExact(Duration.ofDays(12).toSeconds()));
                manager.runScheduledSteps();

                UserModel user = session.users().getUserByUsername(realm, "testuser");
                // Verify that the step was NOT executed as the workflow is disabled.
                assertTrue(user.isEnabled(), "The second step (disable) should NOT have run as the workflow is disabled.");
            } finally {
                Time.setOffset(0);
            }
        });

        // re-enable the workflow - scheduled steps should resume and new users should be bound to the workflow
        workflow.getConfig().putSingle("enabled", "true");
        managedRealm.admin().workflows().workflow(workflow.getId()).update(workflow).close();

        // create a third user - should bind the user to the workflow as it is enabled again
        managedRealm.admin().users().create(UserConfigBuilder.create().username("thirduser").email("thirduser@example.com").build()).close();

        runOnServer.run((RunOnServer) session -> {
            RealmModel realm = configureSessionContext(session);
            WorkflowsManager manager = new WorkflowsManager(session);

            try {
                // Advance time so the first user would be eligible for the second step, and third user would be eligible for the first step, then run the scheduled steps.
                Time.setOffset(Math.toIntExact(Duration.ofDays(12).toSeconds()));
                manager.runScheduledSteps();

                UserModel user = session.users().getUserByUsername(realm, "testuser");
                // Verify that the step was executed as the workflow was re-enabled.
                assertFalse(user.isEnabled(), "The second step (disable) should have run as the workflow was re-enabled.");

                // Verify that the third user was bound to the workflow
                user = session.users().getUserByUsername(realm, "thirduser");
                assertTrue(user.isEnabled(), "The second step (disable) should NOT have run");
            } finally {
                Time.setOffset(0);
            }
        });

        // Verify that the first step (notify) was executed by checking email was sent
        testUserMessage = findEmailByRecipient(mailServer, "thirduser@example.com");
        assertNotNull(testUserMessage, "The first step (notify) should have sent an email.");

        mailServer.runCleanup();
    }

    @Test
    public void testRecurringWorkflow() {
        managedRealm.admin().workflows().create(WorkflowRepresentation.create()
                .of(UserCreationTimeWorkflowProviderFactory.ID)
                .onEvent(ResourceOperationType.USER_ADD.toString())
                .recurring()
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build()
                ).build()).close();

        // create a new user - should bind the user to the workflow and setup the only step in the workflow
        managedRealm.admin().users().create(UserConfigBuilder.create().username("testuser").email("testuser@example.com").build()).close();

        runOnServer.run((RunOnServer) session -> {
            RealmModel realm = configureSessionContext(session);
            WorkflowsManager manager = new WorkflowsManager(session);

            try {
                Time.setOffset(Math.toIntExact(Duration.ofDays(6).toSeconds()));
                manager.runScheduledSteps();

                UserModel user = session.users().getUserByUsername(realm, "testuser");
                Workflow workflow = manager.getWorkflows().get(0);
                WorkflowStep step = manager.getSteps(workflow.getId()).get(0);

                // Verify that the step was scheduled again for the user
                WorkflowStateProvider stateProvider = session.getProvider(WorkflowStateProvider.class);
                ScheduledStep scheduledStep = stateProvider.getScheduledStep(workflow.getId(), user.getId());
                assertNotNull(scheduledStep, "A step should have been scheduled for the user " + user.getUsername());
                assertEquals(step.getId(), scheduledStep.stepId(), "The step should have been scheduled again");

                Time.setOffset(Math.toIntExact(Duration.ofDays(12).toSeconds()));
                manager.runScheduledSteps();
            } finally {
                Time.setOffset(0);
            }
        });

        // Verify that there should be two emails sent
        assertEquals(2, findEmailsByRecipient(mailServer, "testuser@example.com").size());
        mailServer.runCleanup();
    }

    @Test
    public void testRunImmediateWorkflow() {
        // create a test workflow with no time conditions - should run immediately when scheduled
        managedRealm.admin().workflows().create(WorkflowRepresentation.create()
                .of(UserCreationTimeWorkflowProviderFactory.ID)
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
            configureSessionContext(session);
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
        WorkflowSetRepresentation workflows = WorkflowRepresentation.create()
                .of(UserCreationTimeWorkflowProviderFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create().of(SetUserAttributeStepProviderFactory.ID)
                                .after(Duration.ofDays(-5))
                                .withConfig("key", "value")
                                .build())
                .build();
        try (Response response = managedRealm.admin().workflows().create(workflows)) {
            assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
            assertThat(response.readEntity(ErrorRepresentation.class).getErrorMessage(), equalTo("Step 'after' time condition cannot be negative."));
        }
    }

    @Test
    public void testNotifyUserStepSendsEmailWithDefaultDisableMessage() {
        // Create workflow: disable at 10 days, notify 3 days before (at day 7)
        managedRealm.admin().workflows().create(WorkflowRepresentation.create()
                .of(UserCreationTimeWorkflowProviderFactory.ID)
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

        runOnServer.run(session -> {
            WorkflowsManager manager = new WorkflowsManager(session);

            try {
                // Simulate user being 7 days old (eligible for notify step)
                Time.setOffset(Math.toIntExact(Duration.ofDays(7).toSeconds()));

                manager.runScheduledSteps();
            } finally {
                Time.setOffset(0);
            }
        });

        // Verify email was sent to our test user
        MimeMessage testUserMessage = findEmailByRecipient(mailServer, "test@example.com");
        assertNotNull(testUserMessage, "No email found for test@example.com");
        verifyEmailContent(testUserMessage, "test@example.com", "Disable", "John", "3", "inactivity");

        mailServer.runCleanup();
    }

    @Test
    public void testNotifyUserStepSendsEmailWithDefaultDeleteMessage() {
        // Create workflow: delete at 30 days, notify 15 days before (at day 15)
        managedRealm.admin().workflows().create(WorkflowRepresentation.create()
                .of(UserCreationTimeWorkflowProviderFactory.ID)
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

        runOnServer.run(session -> {

            WorkflowsManager manager = new WorkflowsManager(session);

            try {
                // Simulate user being 15 days old
                Time.setOffset(Math.toIntExact(Duration.ofDays(15).toSeconds()));
                manager.runScheduledSteps();
            } finally {
                Time.setOffset(0);
            }
        });

        // Verify email was sent to our test user
        MimeMessage testUserMessage = findEmailByRecipient(mailServer, "test2@example.com");
        assertNotNull(testUserMessage, "No email found for test2@example.com");
        verifyEmailContent(testUserMessage, "test2@example.com", "Deletion", "Jane", "15", "inactivity", "permanently deleted");

        mailServer.runCleanup();
    }

    @Test
    public void testNotifyUserStepWithCustomMessageOverride() {
        // Create workflow: disable at 7 days, notify 2 days before (at day 5) with custom message
        managedRealm.admin().workflows().create(WorkflowRepresentation.create()
                .of(UserCreationTimeWorkflowProviderFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .withConfig("reason", "compliance requirement")
                                .withConfig("custom_message", "Your account requires immediate attention due to new compliance policies.")
                                .withConfig("custom_subject_key", "customComplianceSubject")
                                .build(),
                        WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(7))
                                .build()
                ).build()).close();

        managedRealm.admin().users().create(UserConfigBuilder.create().username("testuser3").email("test3@example.com").name("Bob", "").build()).close();

        runOnServer.run(session -> {
            WorkflowsManager manager = new WorkflowsManager(session);

            try {
                // Simulate user being 5 days old
                Time.setOffset(Math.toIntExact(Duration.ofDays(5).toSeconds()));
                manager.runScheduledSteps();
            } finally {
                Time.setOffset(0);
            }
        });

        // Verify email was sent to our test user
        MimeMessage testUserMessage = findEmailByRecipient(mailServer, "test3@example.com");
        assertNotNull(testUserMessage, "No email found for test3@example.com");
        verifyEmailContent(testUserMessage, "test3@example.com", "", "Bob", "2", "immediate attention due to new compliance policies");

        mailServer.runCleanup();
    }

    @Test
    public void testNotifyUserStepSkipsUsersWithoutEmailButLogsWarning() {
        managedRealm.admin().workflows().create(WorkflowRepresentation.create()
                .of(UserCreationTimeWorkflowProviderFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(10))
                                .build()
                ).build()).close();

        managedRealm.admin().users().create(UserConfigBuilder.create().username("testuser4").name("NoEmail", "").build()).close();

        runOnServer.run(session -> {
            RealmModel realm = configureSessionContext(session);
            WorkflowsManager manager = new WorkflowsManager(session);

            try {
                Time.setOffset(Math.toIntExact(Duration.ofDays(5).toSeconds()));
                manager.runScheduledSteps();

                // But should still create state record for the workflow flow
                UserModel user = session.users().getUserByUsername(realm, "testuser4");
                WorkflowStateProvider stateProvider = session.getProvider(WorkflowStateProvider.class);
                var scheduledSteps = stateProvider.getScheduledStepsByResource(user.getId());
                assertEquals(1, scheduledSteps.size());
            } finally {
                Time.setOffset(0);
            }
        });

        // Should NOT send email to user without email address
        MimeMessage testUserMessage = findEmailByRecipientContaining("testuser4");
        assertNull(testUserMessage, "No email should be sent to user without email address");
    }

    @Test
    public void testCompleteUserLifecycleWithMultipleNotifications() {
        // Create workflow: just disable at 30 days with one notification before
        managedRealm.admin().workflows().create(WorkflowRepresentation.create()
                .of(UserCreationTimeWorkflowProviderFactory.ID)
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

        runOnServer.run(session -> {
            RealmModel realm = configureSessionContext(session);
            WorkflowsManager manager = new WorkflowsManager(session);
            UserModel user = session.users().getUserByUsername(realm, "testuser5");

            try {
                // Day 15: First notification - this should run the notify step and schedule the disable step
                Time.setOffset(Math.toIntExact(Duration.ofDays(15).toSeconds()));
                manager.runScheduledSteps();

                // Check that user is still enabled after notification
                user = session.users().getUserById(realm, user.getId());
                assertTrue(user.isEnabled(), "User should still be enabled after notification");

                // Day 30 + 15 minutes: Disable user - run 15 minutes after the scheduled time to ensure it's due
                Time.setOffset(Math.toIntExact(Duration.ofDays(30).toSeconds()) + Math.toIntExact(Duration.ofMinutes(15).toSeconds()));
                manager.runScheduledSteps();

                // Verify user is disabled
                user = session.users().getUserById(realm, user.getId());
                assertNotNull(user, "User should still exist after disable");
                assertFalse(user.isEnabled(), "User should be disabled");

            } finally {
                Time.setOffset(0);
            }
        });

        // Verify notification was sent
        MimeMessage testUserMessage = findEmailByRecipient(mailServer, "testuser5@example.com");
        assertNotNull(testUserMessage, "No email found for testuser5@example.com");
        verifyEmailContent(testUserMessage, "testuser5@example.com", "Disable", "TestUser5", "15", "inactivity");

        mailServer.runCleanup();
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

    private static RealmModel configureSessionContext(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(REALM_NAME);
        session.getContext().setRealm(realm);
        return realm;
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
}
