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

package org.keycloak.tests.admin.model.policy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.RealmResourcePolicies;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.policy.DisableUserActionProviderFactory;
import org.keycloak.models.policy.NotifyUserActionProviderFactory;
import org.keycloak.models.policy.ResourceAction;
import org.keycloak.models.policy.ResourceOperationType;
import org.keycloak.models.policy.ResourcePolicy;
import org.keycloak.models.policy.ResourcePolicyManager;
import org.keycloak.models.policy.ResourcePolicyStateProvider;
import org.keycloak.models.policy.UserCreationTimeResourcePolicyProviderFactory;
import org.keycloak.models.policy.UserSessionRefreshTimeResourcePolicyProviderFactory;
import org.keycloak.models.policy.conditions.IdentityProviderPolicyConditionFactory;
import org.keycloak.representations.resources.policies.ResourcePolicyActionRepresentation;
import org.keycloak.representations.resources.policies.ResourcePolicyConditionRepresentation;
import org.keycloak.representations.resources.policies.ResourcePolicyRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;

@KeycloakIntegrationTest(config = RLMServerConfig.class)
public class ResourcePolicyManagementTest {

    private static final String REALM_NAME = "default";

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests")
    RunOnServerClient runOnServer;

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @InjectUser(ref = "alice", config = DefaultUserConfig.class, lifecycle = LifeCycle.METHOD)
    private ManagedUser userAlice;

    @Test
    public void testCreate() {
        List<ResourcePolicyRepresentation> expectedPolicies = ResourcePolicyRepresentation.create()
                .of(UserCreationTimeResourcePolicyProviderFactory.ID)
                .withActions(
                        ResourcePolicyActionRepresentation.create().of(NotifyUserActionProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        ResourcePolicyActionRepresentation.create().of(DisableUserActionProviderFactory.ID)
                                .after(Duration.ofDays(10))
                                .build()
                ).build();

        RealmResourcePolicies policies = managedRealm.admin().resources().policies();

        try (Response response = policies.create(expectedPolicies)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        }

        List<ResourcePolicyRepresentation> actualPolicies = policies.list();
        assertThat(actualPolicies, Matchers.hasSize(1));

        assertThat(actualPolicies.get(0).getProviderId(), is(UserCreationTimeResourcePolicyProviderFactory.ID));
        assertThat(actualPolicies.get(0).getActions(), Matchers.hasSize(2));
        assertThat(actualPolicies.get(0).getActions().get(0).getProviderId(), is(NotifyUserActionProviderFactory.ID));
        assertThat(actualPolicies.get(0).getActions().get(1).getProviderId(), is(DisableUserActionProviderFactory.ID));
    }

    @Test
    public void testDelete() {
        List<ResourcePolicyRepresentation> expectedPolicies = ResourcePolicyRepresentation.create()
                .of(UserCreationTimeResourcePolicyProviderFactory.ID)
                .withActions(
                        ResourcePolicyActionRepresentation.create().of(NotifyUserActionProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        ResourcePolicyActionRepresentation.create().of(DisableUserActionProviderFactory.ID)
                                .after(Duration.ofDays(10))
                                .build()
                ).of(UserSessionRefreshTimeResourcePolicyProviderFactory.ID)
                .withActions(
                        ResourcePolicyActionRepresentation.create().of(NotifyUserActionProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        ResourcePolicyActionRepresentation.create().of(DisableUserActionProviderFactory.ID)
                                .after(Duration.ofDays(10))
                                .build())
                .build();

        RealmResourcePolicies policies = managedRealm.admin().resources().policies();

        try (Response response = policies.create(expectedPolicies)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        }

        List<ResourcePolicyRepresentation> actualPolicies = policies.list();
        assertThat(actualPolicies, Matchers.hasSize(2));

        ResourcePolicyRepresentation policy = actualPolicies.get(0);
        managedRealm.admin().resources().policies().policy(policy.getId()).delete().close();
        actualPolicies = policies.list();
        assertThat(actualPolicies, Matchers.hasSize(1));
    }

    @Test
    public void testUpdate() {
        List<ResourcePolicyRepresentation> expectedPolicies = ResourcePolicyRepresentation.create()
                .of(UserCreationTimeResourcePolicyProviderFactory.ID)
                .name("test-policy")
                .withActions(
                        ResourcePolicyActionRepresentation.create().of(NotifyUserActionProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        ResourcePolicyActionRepresentation.create().of(DisableUserActionProviderFactory.ID)
                                .after(Duration.ofDays(10))
                                .build()
                ).build();

        RealmResourcePolicies policies = managedRealm.admin().resources().policies();

        try (Response response = policies.create(expectedPolicies)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        }

        List<ResourcePolicyRepresentation> actualPolicies = policies.list();
        assertThat(actualPolicies, Matchers.hasSize(1));
        ResourcePolicyRepresentation policy = actualPolicies.get(0);
        assertThat(policy.getName(), is("test-policy"));

        policy.setName("changed");
        managedRealm.admin().resources().policies().policy(policy.getId()).update(policy).close();
        actualPolicies = policies.list();
        policy = actualPolicies.get(0);
        assertThat(policy.getName(), is("changed"));
    }

    @Test
    public void testTimeVsPriorityConflictingActions() {
        List<ResourcePolicyRepresentation> expectedPolicies = ResourcePolicyRepresentation.create()
                .of(UserCreationTimeResourcePolicyProviderFactory.ID)
                .withActions(
                        ResourcePolicyActionRepresentation.create().of(NotifyUserActionProviderFactory.ID)
                                .after(Duration.ofDays(10))
                                .build(),
                        ResourcePolicyActionRepresentation.create().of(DisableUserActionProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build()
                ).build();

        RealmResourcePolicies policies = managedRealm.admin().resources().policies();

        try (Response response = policies.create(expectedPolicies)) {
            assertThat(response.getStatus(), is(Status.BAD_REQUEST.getStatusCode()));
        }
    }

    @Test
    public void testPolicyDoesNotFallThroughActionsInSingleRun() {
        managedRealm.admin().resources().policies().create(ResourcePolicyRepresentation.create()
                .of(UserCreationTimeResourcePolicyProviderFactory.ID)
                .onEvent(ResourceOperationType.CREATE.toString())
                .withActions(
                        ResourcePolicyActionRepresentation.create().of(NotifyUserActionProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        ResourcePolicyActionRepresentation.create().of(DisableUserActionProviderFactory.ID)
                                .after(Duration.ofDays(10))
                                .build()
                ).build()).close();

        // create a new user - should bind the user to the policy and setup the first action
        managedRealm.admin().users().create(UserConfigBuilder.create().username("testuser").build());

        runOnServer.run((RunOnServer) session -> {
            RealmModel realm = configureSessionContext(session);
            ResourcePolicyManager manager = new ResourcePolicyManager(session);
            UserModel user = session.users().getUserByUsername(realm,"testuser");

            List<ResourcePolicy> registeredPolicies = manager.getPolicies();
            assertEquals(1, registeredPolicies.size());

            ResourcePolicy policy = registeredPolicies.get(0);
            assertEquals(2, manager.getActions(policy).size());
            ResourceAction notifyAction = manager.getActions(policy).get(0);

            ResourcePolicyStateProvider stateProvider = session.getProvider(ResourcePolicyStateProvider.class);
            ResourcePolicyStateProvider.ScheduledAction scheduledAction = stateProvider.getScheduledAction(policy.getId(), user.getId());
            assertNotNull(scheduledAction, "An action should have been scheduled for the user " + user.getUsername());
            assertEquals(notifyAction.getId(), scheduledAction.actionId());

            try {
                // Simulate the user being 12 days old, making them eligible for both actions' time conditions.
                Time.setOffset(Math.toIntExact(Duration.ofDays(12).toSeconds()));
                manager.runScheduledActions();

                user = session.users().getUserById(realm, user.getId());
                // Verify that ONLY the first action (notify) was executed.
                assertNotNull(user.getAttributes().get("message"), "The first action (notify) should have run.");
                assertTrue(user.isEnabled(), "The second action (disable) should NOT have run.");

                // Verify that the next action was scheduled for the user
                ResourceAction disableAction = manager.getActions(policy).get(1);
                scheduledAction = stateProvider.getScheduledAction(policy.getId(), user.getId());
                assertNotNull(scheduledAction, "An action should have been scheduled for the user " + user.getUsername());
                assertEquals(disableAction.getId(), scheduledAction.actionId(), "The second action should have been scheduled");
            } finally {
                Time.setOffset(0);
            }
        });
    }

    @Test
    public void testAssignPolicyToExistingResources() {
        // create some realm users
        for (int i = 0; i < 10; i++) {
            managedRealm.admin().users().create(UserConfigBuilder.create().username("user-" + i).build());
        }

        // create some users associated with a federated identity
        for (int i = 0; i < 10; i++) {
            managedRealm.admin().users().create(UserConfigBuilder.create().username("idp-user-" + i)
                    .federatedLink("someidp", UUID.randomUUID().toString(), "idp-user-" + i).build());
        }

        managedRealm.admin().resources().policies().create(ResourcePolicyRepresentation.create()
                .of(UserCreationTimeResourcePolicyProviderFactory.ID)
                .onEvent(ResourceOperationType.ADD_FEDERATED_IDENTITY.name())
                .onCoditions(ResourcePolicyConditionRepresentation.create()
                        .of(IdentityProviderPolicyConditionFactory.ID)
                        .withConfig(IdentityProviderPolicyConditionFactory.EXPECTED_ALIASES, "someidp")
                        .build())
                .withActions(
                        ResourcePolicyActionRepresentation.create().of(NotifyUserActionProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        ResourcePolicyActionRepresentation.create().of(DisableUserActionProviderFactory.ID)
                                .after(Duration.ofDays(10))
                                .build()
                ).build()).close();

        // now with the policy in place, let's create a couple more idp users - these will be attached to the policy on
        // creation.
        for (int i = 0; i < 3; i++) {
            managedRealm.admin().users().create(UserConfigBuilder.create().username("new-idp-user-" + i)
                    .federatedLink("someidp", UUID.randomUUID().toString(), "new-idp-user-" + i).build());
        }

        // new realm users created after the policy - these should not be attached to the policy because they are not idp users.
        for (int i = 0; i < 3; i++) {
            managedRealm.admin().users().create(UserConfigBuilder.create().username("new-user-" + i).build());
        }

        runOnServer.run((RunOnServer) session -> {
            RealmModel realm = configureSessionContext(session);
            ResourcePolicyManager policyManager = new ResourcePolicyManager(session);
            List<ResourcePolicy> registeredPolicies = policyManager.getPolicies();
            assertEquals(1, registeredPolicies.size());
            ResourcePolicy policy = registeredPolicies.get(0);

            assertEquals(2, policyManager.getActions(policy).size());
            ResourceAction notifyAction = policyManager.getActions(policy).get(0);

            // check no policies are yet attached to the previous users, only to the ones created after the policy was in place
            ResourcePolicyStateProvider stateProvider = session.getKeycloakSessionFactory().getProviderFactory(ResourcePolicyStateProvider.class).create(session);
            List<ResourcePolicyStateProvider.ScheduledAction> scheduledActions = stateProvider.getScheduledActionsByPolicy(policy);
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
                policyManager.runScheduledActions();

                // check the same users are now scheduled to run the second action.
                ResourceAction disableAction = policyManager.getActions(policy).get(1);
                scheduledActions = stateProvider.getScheduledActionsByPolicy(policy);
                assertEquals(3, scheduledActions.size());
                scheduledActions.forEach(scheduledAction -> {
                    assertEquals(disableAction.getId(), scheduledAction.actionId());
                    UserModel user = session.users().getUserById(realm, scheduledAction.resourceId());
                    assertNotNull(user);
                    assertTrue(user.getUsername().startsWith("new-idp-user-"));
                });

                // assign the policy to the eligible users - i.e. only users from the same idp who are not yet assigned to the policy.
                policyManager.scheduleAllEligibleResources(policy);

                // check policy was correctly assigned to the old users, not affecting users already associated with the policy.
                scheduledActions = stateProvider.getScheduledActionsByPolicy(policy);
                assertEquals(13, scheduledActions.size());

                List<ResourcePolicyStateProvider.ScheduledAction> scheduledToNotify = scheduledActions.stream()
                        .filter(action -> notifyAction.getId().equals(action.actionId())).toList();
                assertEquals(10, scheduledToNotify.size());
                scheduledToNotify.forEach(scheduledAction -> {
                    UserModel user = session.users().getUserById(realm, scheduledAction.resourceId());
                    assertNotNull(user);
                    assertTrue(user.getUsername().startsWith("idp-user-"));
                });

                List<ResourcePolicyStateProvider.ScheduledAction> scheduledToDisable = scheduledActions.stream()
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
    public void testDisableResourcePolicy() {
        // create a test policy
        managedRealm.admin().resources().policies().create(ResourcePolicyRepresentation.create()
                .of(UserCreationTimeResourcePolicyProviderFactory.ID)
                .onEvent(ResourceOperationType.CREATE.toString())
                .name("test-policy")
                .withConfig("enabled", "true")
                .withActions(
                        ResourcePolicyActionRepresentation.create().of(NotifyUserActionProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        ResourcePolicyActionRepresentation.create().of(DisableUserActionProviderFactory.ID)
                                .after(Duration.ofDays(10))
                                .build()
                ).build()).close();

        RealmResourcePolicies policies = managedRealm.admin().resources().policies();
        List<ResourcePolicyRepresentation> actualPolicies = policies.list();
        assertThat(actualPolicies, Matchers.hasSize(1));
        ResourcePolicyRepresentation policy = actualPolicies.get(0);
        assertThat(policy.getName(), is("test-policy"));

        // create a new user - should bind the user to the policy and setup the first action
        managedRealm.admin().users().create(UserConfigBuilder.create().username("testuser").build());

        runOnServer.run((RunOnServer) session -> {
            RealmModel realm = configureSessionContext(session);
            ResourcePolicyManager manager = new ResourcePolicyManager(session);

            try {
                // Advance time so the user is eligible for the first action, then run the scheduled actions so they transition to the next one.
                Time.setOffset(Math.toIntExact(Duration.ofDays(6).toSeconds()));
                manager.runScheduledActions();

                UserModel user = session.users().getUserByUsername(realm, "testuser");
                // Verify that ONLY the first action (notify) was executed.
                assertNotNull(user.getAttributes().get("message"), "The first action (notify) should have run.");
                assertTrue(user.isEnabled(), "The second action (disable) should NOT have run.");
            } finally {
                Time.setOffset(0);
            }
        });

        // disable the policy - scheduled actions should be paused and policy should not activate for new users
        policy.getConfig().putSingle("enabled", "false");
        managedRealm.admin().resources().policies().policy(policy.getId()).update(policy).close();

        // create another user - should NOT bind the user to the policy as it is disabled
        managedRealm.admin().users().create(UserConfigBuilder.create().username("anotheruser").build());

        runOnServer.run((RunOnServer) session -> {
            RealmModel realm = configureSessionContext(session);
            ResourcePolicyManager manager = new ResourcePolicyManager(session);

            List<ResourcePolicy> registeredPolicies = manager.getPolicies();
            assertEquals(1, registeredPolicies.size());
            ResourcePolicyStateProvider stateProvider = session.getKeycloakSessionFactory().getProviderFactory(ResourcePolicyStateProvider.class).create(session);
            List<ResourcePolicyStateProvider.ScheduledAction> scheduledActions = stateProvider.getScheduledActionsByPolicy(registeredPolicies.get(0));

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
                // Verify that the action was NOT executed as the policy is disabled.
                assertTrue(user.isEnabled(), "The second action (disable) should NOT have run as the policy is disabled.");
            } finally {
                Time.setOffset(0);
            }
        });

        // re-enable the policy - scheduled actions should resume and new users should be bound to the policy
        policy.getConfig().putSingle("enabled", "true");
        managedRealm.admin().resources().policies().policy(policy.getId()).update(policy).close();

        // create a third user - should bind the user to the policy as it is enabled again
        managedRealm.admin().users().create(UserConfigBuilder.create().username("thirduser").build());

        runOnServer.run((RunOnServer) session -> {
            RealmModel realm = configureSessionContext(session);
            ResourcePolicyManager manager = new ResourcePolicyManager(session);

            try {
                // Advance time so the first user would be eligible for the second action, and third user would be eligible for the first action, then run the scheduled actions.
                Time.setOffset(Math.toIntExact(Duration.ofDays(12).toSeconds()));
                manager.runScheduledActions();

                UserModel user = session.users().getUserByUsername(realm, "testuser");
                // Verify that the action was executed as the policy was re-enabled.
                assertFalse(user.isEnabled(), "The second action (disable) should have run as the policy was re-enabled.");

                // Verify that the third user was bound to the policy and had the first action executed.
                user = session.users().getUserByUsername(realm, "thirduser");
                assertNotNull(user.getAttributes().get("message"), "The first action (notify) should have run.");
                assertTrue(user.isEnabled(), "The second action (disable) should NOT have run");
            } finally {
                Time.setOffset(0);
            }
        });
    }

    @Test
    public void testRecurringPolicy() {
        managedRealm.admin().resources().policies().create(ResourcePolicyRepresentation.create()
                .of(UserCreationTimeResourcePolicyProviderFactory.ID)
                .onEvent(ResourceOperationType.CREATE.toString())
                .withConfig("recurring", "true")
                .withActions(
                        ResourcePolicyActionRepresentation.create().of(NotifyUserActionProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build()
                ).build()).close();

        // create a new user - should bind the user to the policy and setup the only action in the policy
        managedRealm.admin().users().create(UserConfigBuilder.create().username("testuser").build());

        runOnServer.run((RunOnServer) session -> {
            RealmModel realm = configureSessionContext(session);
            ResourcePolicyManager manager = new ResourcePolicyManager(session);

            try {
                Time.setOffset(Math.toIntExact(Duration.ofDays(6).toSeconds()));
                manager.runScheduledActions();

                UserModel user = session.users().getUserByUsername(realm, "testuser");
                // Verify that the action (notify) was executed.
                assertNotNull(user.getAttributes().get("message"), "The action (notify) should have run.");
                user.removeAttribute("message");
                ResourcePolicy policy = manager.getPolicies().get(0);
                ResourceAction action = manager.getActions(policy).get(0);

                // Verify that the action was scheduled again for the user
                ResourcePolicyStateProvider stateProvider = session.getProvider(ResourcePolicyStateProvider.class);
                ResourcePolicyStateProvider.ScheduledAction scheduledAction = stateProvider.getScheduledAction(policy.getId(), user.getId());
                assertNotNull(scheduledAction, "An action should have been scheduled for the user " + user.getUsername());
                assertEquals(action.getId(), scheduledAction.actionId(), "The action should have been scheduled again");

                Time.setOffset(Math.toIntExact(Duration.ofDays(12).toSeconds()));
                manager.runScheduledActions();
                user = session.users().getUserByUsername(realm, "testuser");
                assertNotNull(user.getAttributes().get("message"), "The action (notify) should have run again.");
            } finally {
                Time.setOffset(0);
            }
        });
    }

    private static RealmModel configureSessionContext(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(REALM_NAME);
        session.getContext().setRealm(realm);
        return realm;
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
