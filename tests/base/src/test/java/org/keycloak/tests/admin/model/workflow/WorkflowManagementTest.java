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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Arrays;
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
import org.keycloak.models.workflow.DeleteUserActionProviderFactory;
import org.keycloak.models.workflow.DisableUserActionProviderFactory;
import org.keycloak.models.workflow.EventBasedWorkflowProviderFactory;
import org.keycloak.models.workflow.NotifyUserActionProviderFactory;
import org.keycloak.models.workflow.WorkflowAction;
import org.keycloak.models.workflow.ResourceOperationType;
import org.keycloak.models.workflow.Workflow;
import org.keycloak.models.workflow.WorkflowsManager;
import org.keycloak.models.workflow.WorkflowStateProvider;
import org.keycloak.models.workflow.WorkflowStateProvider.ScheduledAction;
import org.keycloak.models.workflow.SetUserAttributeActionProviderFactory;
import org.keycloak.models.workflow.UserCreationTimeWorkflowProviderFactory;
import org.keycloak.models.workflow.conditions.IdentityProviderWorkflowConditionFactory;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.workflows.WorkflowActionRepresentation;
import org.keycloak.representations.workflows.WorkflowConditionRepresentation;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.tests.utils.MailUtils;

@KeycloakIntegrationTest(config = WorkflowsServerConfig.class)
public class WorkflowManagementTest {

    private static final String REALM_NAME = "default";

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests")
    RunOnServerClient runOnServer;

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @InjectUser(ref = "alice", config = DefaultUserConfig.class, lifecycle = LifeCycle.METHOD)
    private ManagedUser userAlice;

    @InjectMailServer
    private MailServer mailServer;

    @Test
    public void testCreate() {
        List<WorkflowRepresentation> expectedWorkflows = WorkflowRepresentation.create()
                .of(UserCreationTimeWorkflowProviderFactory.ID)
                .withActions(
                        WorkflowActionRepresentation.create().of(NotifyUserActionProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        WorkflowActionRepresentation.create().of(DisableUserActionProviderFactory.ID)
                                .after(Duration.ofDays(10))
                                .build()
                ).build();

        WorkflowsResource workflows = managedRealm.admin().workflows();

        try (Response response = workflows.create(expectedWorkflows)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        }

        List<WorkflowRepresentation> actualWorkflows = workflows.list();
        assertThat(actualWorkflows, Matchers.hasSize(1));

        assertThat(actualWorkflows.get(0).getProviderId(), is(UserCreationTimeWorkflowProviderFactory.ID));
        assertThat(actualWorkflows.get(0).getActions(), Matchers.hasSize(2));
        assertThat(actualWorkflows.get(0).getActions().get(0).getProviderId(), is(NotifyUserActionProviderFactory.ID));
        assertThat(actualWorkflows.get(0).getActions().get(1).getProviderId(), is(DisableUserActionProviderFactory.ID));
    }

    @Test
    public void testCreateWithNoConditions() {
        List<WorkflowRepresentation> expectedWorkflows = WorkflowRepresentation.create()
                .of(EventBasedWorkflowProviderFactory.ID)
                .withActions(
                        WorkflowActionRepresentation.create().of(NotifyUserActionProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        WorkflowActionRepresentation.create().of(DisableUserActionProviderFactory.ID)
                                .after(Duration.ofDays(10))
                                .build()
                ).build();

        expectedWorkflows.get(0).setConditions(null);

        try (Response response = managedRealm.admin().workflows().create(expectedWorkflows)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        }
    }

    @Test
    public void testDelete() {
        WorkflowsResource workflows = managedRealm.admin().workflows();

        workflows.create(WorkflowRepresentation.create()
                .of(UserCreationTimeWorkflowProviderFactory.ID)
                .onEvent(ResourceOperationType.CREATE.toString())
                .recurring()
                .withActions(
                        WorkflowActionRepresentation.create().of(NotifyUserActionProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build()
                ).of(EventBasedWorkflowProviderFactory.ID)
                .onEvent(ResourceOperationType.LOGIN.toString())
                .recurring()
                .withActions(
                        WorkflowActionRepresentation.create().of(NotifyUserActionProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build()
                ).build()).close();

        // create a new user - should bind the user to the workflow and setup the only action in the workflow
        managedRealm.admin().users().create(UserConfigBuilder.create().username("testuser").email("testuser@example.com").build()).close();

        List<WorkflowRepresentation> actualWorkflows = workflows.list();
        assertThat(actualWorkflows, Matchers.hasSize(2));

        WorkflowRepresentation workflow = actualWorkflows.stream().filter(p -> UserCreationTimeWorkflowProviderFactory.ID.equals(p.getProviderId())).findAny().orElse(null);
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
            List<ScheduledAction> actions = stateProvider.getScheduledActionsByWorkflow(id);
            assertTrue(actions.isEmpty());
        });
    }

    @Test
    public void testUpdate() {
        List<WorkflowRepresentation> expectedWorkflows = WorkflowRepresentation.create()
                .of(UserCreationTimeWorkflowProviderFactory.ID)
                .name("test-workflow")
                .withActions(
                        WorkflowActionRepresentation.create().of(NotifyUserActionProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        WorkflowActionRepresentation.create().of(DisableUserActionProviderFactory.ID)
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
    }

    @Test
    public void testWorkflowDoesNotFallThroughActionsInSingleRun() {
        managedRealm.admin().workflows().create(WorkflowRepresentation.create()
                .of(UserCreationTimeWorkflowProviderFactory.ID)
                .onEvent(ResourceOperationType.CREATE.toString())
                .withActions(
                        WorkflowActionRepresentation.create().of(NotifyUserActionProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        WorkflowActionRepresentation.create().of(DisableUserActionProviderFactory.ID)
                                .after(Duration.ofDays(10))
                                .build()
                ).build()).close();

        // create a new user - should bind the user to the workflow and setup the first action
        managedRealm.admin().users().create(UserConfigBuilder.create().username("testuser").email("testuser@example.com").build()).close();

        runOnServer.run((RunOnServer) session -> {
            RealmModel realm = configureSessionContext(session);
            WorkflowsManager manager = new WorkflowsManager(session);
            UserModel user = session.users().getUserByUsername(realm,"testuser");

            List<Workflow> registeredWorkflows = manager.getWorkflows();
            assertEquals(1, registeredWorkflows.size());

            Workflow workflow = registeredWorkflows.get(0);
            assertEquals(2, manager.getActions(workflow.getId()).size());
            WorkflowAction notifyAction = manager.getActions(workflow.getId()).get(0);

            WorkflowStateProvider stateProvider = session.getProvider(WorkflowStateProvider.class);
            WorkflowStateProvider.ScheduledAction scheduledAction = stateProvider.getScheduledAction(workflow.getId(), user.getId());
            assertNotNull(scheduledAction, "An action should have been scheduled for the user " + user.getUsername());
            assertEquals(notifyAction.getId(), scheduledAction.actionId());

            try {
                // Simulate the user being 12 days old, making them eligible for both actions' time conditions.
                Time.setOffset(Math.toIntExact(Duration.ofDays(12).toSeconds()));
                manager.runScheduledActions();

                user = session.users().getUserById(realm, user.getId());

                // Verify that the next action was scheduled for the user
                WorkflowAction disableAction = manager.getActions(workflow.getId()).get(1);
                scheduledAction = stateProvider.getScheduledAction(workflow.getId(), user.getId());
                assertNotNull(scheduledAction, "An action should have been scheduled for the user " + user.getUsername());
                assertEquals(disableAction.getId(), scheduledAction.actionId(), "The second action should have been scheduled");
            } finally {
                Time.setOffset(0);
            }
        });

        // Verify that the first action (notify) was executed by checking email was sent
        MimeMessage testUserMessage = findEmailByRecipient(mailServer, "testuser@example.com");
        assertNotNull(testUserMessage, "The first action (notify) should have sent an email.");
        
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
                .onEvent(ResourceOperationType.ADD_FEDERATED_IDENTITY.name())
                .onConditions(WorkflowConditionRepresentation.create()
                        .of(IdentityProviderWorkflowConditionFactory.ID)
                        .withConfig(IdentityProviderWorkflowConditionFactory.EXPECTED_ALIASES, "someidp")
                        .build())
                .withActions(
                        WorkflowActionRepresentation.create().of(NotifyUserActionProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        WorkflowActionRepresentation.create().of(DisableUserActionProviderFactory.ID)
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

            assertEquals(2, workflowsManager.getActions(workflow.getId()).size());
            WorkflowAction notifyAction = workflowsManager.getActions(workflow.getId()).get(0);

            // check no workflows are yet attached to the previous users, only to the ones created after the workflow was in place
            WorkflowStateProvider stateProvider = session.getKeycloakSessionFactory().getProviderFactory(WorkflowStateProvider.class).create(session);
            List<WorkflowStateProvider.ScheduledAction> scheduledActions = stateProvider.getScheduledActionsByWorkflow(workflow);
            assertEquals(3, scheduledActions.size());
            scheduledActions.forEach(scheduledAction -> {
                assertEquals(notifyAction.getId(), scheduledAction.actionId());
                UserModel user = session.users().getUserById(realm, scheduledAction.resourceId());
                assertNotNull(user);
                assertTrue(user.getUsername().startsWith("new-idp-user-"));
            });

            try {
                // let's run the schedule actions for the new users so they transition to the next one.
                Time.setOffset(Math.toIntExact(Duration.ofDays(6).toSeconds()));
                workflowsManager.runScheduledActions();

                // check the same users are now scheduled to run the second action.
                WorkflowAction disableAction = workflowsManager.getActions(workflow.getId()).get(1);
                scheduledActions = stateProvider.getScheduledActionsByWorkflow(workflow);
                assertEquals(3, scheduledActions.size());
                scheduledActions.forEach(scheduledAction -> {
                    assertEquals(disableAction.getId(), scheduledAction.actionId());
                    UserModel user = session.users().getUserById(realm, scheduledAction.resourceId());
                    assertNotNull(user);
                    assertTrue(user.getUsername().startsWith("new-idp-user-"));
                });

                // assign the workflow to the eligible users - i.e. only users from the same idp who are not yet assigned to the workflow.
                workflowsManager.scheduleAllEligibleResources(workflow);

                // check workflow was correctly assigned to the old users, not affecting users already associated with the workflow.
                scheduledActions = stateProvider.getScheduledActionsByWorkflow(workflow);
                assertEquals(13, scheduledActions.size());

                List<WorkflowStateProvider.ScheduledAction> scheduledToNotify = scheduledActions.stream()
                        .filter(action -> notifyAction.getId().equals(action.actionId())).toList();
                assertEquals(10, scheduledToNotify.size());
                scheduledToNotify.forEach(scheduledAction -> {
                    UserModel user = session.users().getUserById(realm, scheduledAction.resourceId());
                    assertNotNull(user);
                    assertTrue(user.getUsername().startsWith("idp-user-"));
                });

                List<WorkflowStateProvider.ScheduledAction> scheduledToDisable = scheduledActions.stream()
                        .filter(action -> disableAction.getId().equals(action.actionId())).toList();
                assertEquals(3, scheduledToDisable.size());
                scheduledToDisable.forEach(scheduledAction -> {
                    UserModel user = session.users().getUserById(realm, scheduledAction.resourceId());
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
                .onEvent(ResourceOperationType.CREATE.toString())
                .name("test-workflow")
                .withActions(
                        WorkflowActionRepresentation.create().of(NotifyUserActionProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        WorkflowActionRepresentation.create().of(DisableUserActionProviderFactory.ID)
                                .after(Duration.ofDays(10))
                                .build()
                ).build()).close();

        WorkflowsResource workflows = managedRealm.admin().workflows();
        List<WorkflowRepresentation> actualWorkflows = workflows.list();
        assertThat(actualWorkflows, Matchers.hasSize(1));
        WorkflowRepresentation workflow = actualWorkflows.get(0);
        assertThat(workflow.getName(), is("test-workflow"));

        // create a new user - should bind the user to the workflow and setup the first action
        managedRealm.admin().users().create(UserConfigBuilder.create().username("testuser").email("testuser@example.com").build()).close();

        runOnServer.run((RunOnServer) session -> {
            RealmModel realm = configureSessionContext(session);
            WorkflowsManager manager = new WorkflowsManager(session);

            try {
                // Advance time so the user is eligible for the first action, then run the scheduled actions so they transition to the next one.
                Time.setOffset(Math.toIntExact(Duration.ofDays(6).toSeconds()));
                manager.runScheduledActions();

                UserModel user = session.users().getUserByUsername(realm, "testuser");
                assertTrue(user.isEnabled(), "The second action (disable) should NOT have run.");
            } finally {
                Time.setOffset(0);
            }
        });

        // Verify that the first action (notify) was executed by checking email was sent
        MimeMessage testUserMessage = findEmailByRecipient(mailServer, "testuser@example.com");
        assertNotNull(testUserMessage, "The first action (notify) should have sent an email.");

        mailServer.runCleanup();

        // disable the workflow - scheduled actions should be paused and workflow should not activate for new users
        workflow.getConfig().putSingle("enabled", "false");
        managedRealm.admin().workflows().workflow(workflow.getId()).update(workflow).close();

        // create another user - should NOT bind the user to the workflow as it is disabled
        managedRealm.admin().users().create(UserConfigBuilder.create().username("anotheruser").build()).close();

        runOnServer.run((RunOnServer) session -> {
            RealmModel realm = configureSessionContext(session);
            WorkflowsManager manager = new WorkflowsManager(session);

            List<Workflow> registeredWorkflow = manager.getWorkflows();
            assertEquals(1, registeredWorkflow.size());
            WorkflowStateProvider stateProvider = session.getKeycloakSessionFactory().getProviderFactory(WorkflowStateProvider.class).create(session);
            List<WorkflowStateProvider.ScheduledAction> scheduledActions = stateProvider.getScheduledActionsByWorkflow(registeredWorkflow.get(0));

            // verify that there's only one scheduled action, for the first user
            assertEquals(1, scheduledActions.size());
            UserModel scheduledActionUser = session.users().getUserById(realm, scheduledActions.get(0).resourceId());
            assertNotNull(scheduledActionUser);
            assertTrue(scheduledActionUser.getUsername().startsWith("testuser"));

            try {
                // Advance time so the first user would be eligible for the second action, then run the scheduled actions.
                Time.setOffset(Math.toIntExact(Duration.ofDays(12).toSeconds()));
                manager.runScheduledActions();

                UserModel user = session.users().getUserByUsername(realm, "testuser");
                // Verify that the action was NOT executed as the workflow is disabled.
                assertTrue(user.isEnabled(), "The second action (disable) should NOT have run as the workflow is disabled.");
            } finally {
                Time.setOffset(0);
            }
        });

        // re-enable the workflow - scheduled actions should resume and new users should be bound to the workflow
        workflow.getConfig().putSingle("enabled", "true");
        managedRealm.admin().workflows().workflow(workflow.getId()).update(workflow).close();

        // create a third user - should bind the user to the workflow as it is enabled again
        managedRealm.admin().users().create(UserConfigBuilder.create().username("thirduser").email("thirduser@example.com").build()).close();

        runOnServer.run((RunOnServer) session -> {
            RealmModel realm = configureSessionContext(session);
            WorkflowsManager manager = new WorkflowsManager(session);

            try {
                // Advance time so the first user would be eligible for the second action, and third user would be eligible for the first action, then run the scheduled actions.
                Time.setOffset(Math.toIntExact(Duration.ofDays(12).toSeconds()));
                manager.runScheduledActions();

                UserModel user = session.users().getUserByUsername(realm, "testuser");
                // Verify that the action was executed as the workflow was re-enabled.
                assertFalse(user.isEnabled(), "The second action (disable) should have run as the workflow was re-enabled.");

                // Verify that the third user was bound to the workflow
                user = session.users().getUserByUsername(realm, "thirduser");
                assertTrue(user.isEnabled(), "The second action (disable) should NOT have run");
            } finally {
                Time.setOffset(0);
            }
        });

        // Verify that the first action (notify) was executed by checking email was sent
        testUserMessage = findEmailByRecipient(mailServer, "thirduser@example.com");
        assertNotNull(testUserMessage, "The first action (notify) should have sent an email.");

        mailServer.runCleanup();
    }

    @Test
    public void testRecurringWorkflow() {
        managedRealm.admin().workflows().create(WorkflowRepresentation.create()
                .of(UserCreationTimeWorkflowProviderFactory.ID)
                .onEvent(ResourceOperationType.CREATE.toString())
                .recurring()
                .withActions(
                        WorkflowActionRepresentation.create().of(NotifyUserActionProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build()
                ).build()).close();

        // create a new user - should bind the user to the workflow and setup the only action in the workflow
        managedRealm.admin().users().create(UserConfigBuilder.create().username("testuser").email("testuser@example.com").build()).close();

        runOnServer.run((RunOnServer) session -> {
            RealmModel realm = configureSessionContext(session);
            WorkflowsManager manager = new WorkflowsManager(session);

            try {
                Time.setOffset(Math.toIntExact(Duration.ofDays(6).toSeconds()));
                manager.runScheduledActions();

                UserModel user = session.users().getUserByUsername(realm, "testuser");
                Workflow workflow = manager.getWorkflows().get(0);
                WorkflowAction action = manager.getActions(workflow.getId()).get(0);

                // Verify that the action was scheduled again for the user
                WorkflowStateProvider stateProvider = session.getProvider(WorkflowStateProvider.class);
                WorkflowStateProvider.ScheduledAction scheduledAction = stateProvider.getScheduledAction(workflow.getId(), user.getId());
                assertNotNull(scheduledAction, "An action should have been scheduled for the user " + user.getUsername());
                assertEquals(action.getId(), scheduledAction.actionId(), "The action should have been scheduled again");

                Time.setOffset(Math.toIntExact(Duration.ofDays(12).toSeconds()));
                manager.runScheduledActions();
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
                .immediate()
                .withActions(
                        WorkflowActionRepresentation.create().of(SetUserAttributeActionProviderFactory.ID)
                                .after(Duration.ofDays(1))
                                .withConfig("message", "message")
                                .build(),
                        WorkflowActionRepresentation.create().of(DisableUserActionProviderFactory.ID)
                                .after(Duration.ofDays(2))
                                .build()
                ).build()).close();

        // create a new user - should be bound to the new workflow and all actions should run right away
        managedRealm.admin().users().create(UserConfigBuilder.create().username("testuser").build()).close();

        // check the user has the attribute set and is disabled
        runOnServer.run(session -> {
            configureSessionContext(session);
            UserModel user = session.users().getUserByUsername(session.getContext().getRealm(), "testuser");
            assertEquals("message", user.getAttributes().get("message").get(0));
            assertFalse(user.isEnabled());
        });
    }

    @Test
    public void testNotifyUserActionSendsEmailWithDefaultDisableMessage() {
        // Create workflow: disable at 10 days, notify 3 days before (at day 7)
        managedRealm.admin().workflows().create(WorkflowRepresentation.create()
                .of(UserCreationTimeWorkflowProviderFactory.ID)
                .withActions(
                        WorkflowActionRepresentation.create().of(NotifyUserActionProviderFactory.ID)
                                .after(Duration.ofDays(7))
                                .withConfig("reason", "inactivity")
                                .build(),
                        WorkflowActionRepresentation.create().of(DisableUserActionProviderFactory.ID)
                                .after(Duration.ofDays(10))
                                .build()
                ).build()).close();

        managedRealm.admin().users().create(UserConfigBuilder.create().username("testuser").email("test@example.com").name("John", "").build()).close();

        runOnServer.run(session -> {
            WorkflowsManager manager = new WorkflowsManager(session);

            try {
                // Simulate user being 7 days old (eligible for notify action)
                Time.setOffset(Math.toIntExact(Duration.ofDays(7).toSeconds()));

                manager.runScheduledActions();
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
    public void testNotifyUserActionSendsEmailWithDefaultDeleteMessage() {
        // Create workflow: delete at 30 days, notify 15 days before (at day 15)
        managedRealm.admin().workflows().create(WorkflowRepresentation.create()
                .of(UserCreationTimeWorkflowProviderFactory.ID)
                .withActions(
                        WorkflowActionRepresentation.create().of(NotifyUserActionProviderFactory.ID)
                                .after(Duration.ofDays(15))
                                .withConfig("reason", "inactivity")
                                .build(),
                        WorkflowActionRepresentation.create().of(DeleteUserActionProviderFactory.ID)
                                .after(Duration.ofDays(30))
                                .build()
                ).build()).close();

        managedRealm.admin().users().create(UserConfigBuilder.create().username("testuser2").email("test2@example.com").name("Jane", "").build()).close();

        runOnServer.run(session -> {

            WorkflowsManager manager = new WorkflowsManager(session);

            try {
                // Simulate user being 15 days old
                Time.setOffset(Math.toIntExact(Duration.ofDays(15).toSeconds()));
                manager.runScheduledActions();
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
    public void testNotifyUserActionWithCustomMessageOverride() {
        // Create workflow: disable at 7 days, notify 2 days before (at day 5) with custom message
        managedRealm.admin().workflows().create(WorkflowRepresentation.create()
                .of(UserCreationTimeWorkflowProviderFactory.ID)
                .withActions(
                        WorkflowActionRepresentation.create().of(NotifyUserActionProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .withConfig("reason", "compliance requirement")
                                .withConfig("custom_message", "Your account requires immediate attention due to new compliance policies.")
                                .withConfig("custom_subject_key", "customComplianceSubject")
                                .build(),
                        WorkflowActionRepresentation.create().of(DisableUserActionProviderFactory.ID)
                                .after(Duration.ofDays(7))
                                .build()
                ).build()).close();

        managedRealm.admin().users().create(UserConfigBuilder.create().username("testuser3").email("test3@example.com").name("Bob", "").build()).close();

        runOnServer.run(session -> {
            WorkflowsManager manager = new WorkflowsManager(session);

            try {
                // Simulate user being 5 days old
                Time.setOffset(Math.toIntExact(Duration.ofDays(5).toSeconds()));
                manager.runScheduledActions();
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
    public void testNotifyUserActionSkipsUsersWithoutEmailButLogsWarning() {
        managedRealm.admin().workflows().create(WorkflowRepresentation.create()
                .of(UserCreationTimeWorkflowProviderFactory.ID)
                .withActions(
                        WorkflowActionRepresentation.create().of(NotifyUserActionProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        WorkflowActionRepresentation.create().of(DisableUserActionProviderFactory.ID)
                                .after(Duration.ofDays(10))
                                .build()
                ).build()).close();

        managedRealm.admin().users().create(UserConfigBuilder.create().username("testuser4").name("NoEmail", "").build()).close();

        runOnServer.run(session -> {
            RealmModel realm = configureSessionContext(session);
            WorkflowsManager manager = new WorkflowsManager(session);

            try {
                Time.setOffset(Math.toIntExact(Duration.ofDays(5).toSeconds()));
                manager.runScheduledActions();

                // But should still create state record for the workflow flow
                UserModel user = session.users().getUserByUsername(realm, "testuser4");
                WorkflowStateProvider stateProvider = session.getProvider(WorkflowStateProvider.class);
                var scheduledActions = stateProvider.getScheduledActionsByResource(user.getId());
                assertEquals(1, scheduledActions.size());
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
                .withActions(
                        WorkflowActionRepresentation.create().of(NotifyUserActionProviderFactory.ID)
                                .after(Duration.ofDays(15))
                                .withConfig("reason", "inactivity")
                                .build(),
                        WorkflowActionRepresentation.create().of(DisableUserActionProviderFactory.ID)
                                .after(Duration.ofDays(30))
                                .build()
                ).build()).close();

        managedRealm.admin().users().create(UserConfigBuilder.create().username("testuser5").email("testuser5@example.com").name("TestUser5", "").build()).close();

        runOnServer.run(session -> {
            RealmModel realm = configureSessionContext(session);
            WorkflowsManager manager = new WorkflowsManager(session);
            UserModel user = session.users().getUserByUsername(realm, "testuser5");

            try {
                // Day 15: First notification - this should run the notify action and schedule the disable action
                Time.setOffset(Math.toIntExact(Duration.ofDays(15).toSeconds()));
                manager.runScheduledActions();

                // Check that user is still enabled after notification
                user = session.users().getUserById(realm, user.getId());
                assertTrue(user.isEnabled(), "User should still be enabled after notification");

                // Day 30 + 15 minutes: Disable user - run 15 minutes after the scheduled time to ensure it's due
                Time.setOffset(Math.toIntExact(Duration.ofDays(30).toSeconds()) + Math.toIntExact(Duration.ofMinutes(15).toSeconds()));
                manager.runScheduledActions();

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
            assertTrue(message.getSubject().contains(subjectContains),
                    "Subject should contain '" + subjectContains + "'");

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
