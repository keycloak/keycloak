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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.policy.DisableUserActionProviderFactory;
import org.keycloak.models.policy.NotifyUserActionProviderFactory;
import org.keycloak.models.policy.ResourceAction;
import org.keycloak.models.policy.ResourcePolicy;
import org.keycloak.models.policy.ResourcePolicyManager;
import org.keycloak.models.policy.ResourcePolicyStateProvider;
import org.keycloak.models.policy.UserActionBuilder;
import org.keycloak.models.policy.UserCreationTimeResourcePolicyProviderFactory;
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
    public void testCreatePolicy() {
        runOnServer.run(session -> {
            RealmModel realm = configureSessionContext(session);
            ResourcePolicyManager manager = new ResourcePolicyManager(session);

            ResourcePolicy created = manager.addPolicy(new ResourcePolicy(UserCreationTimeResourcePolicyProviderFactory.ID));
            assertNotNull(created.getId());

            List<ResourcePolicy> policies = manager.getPolicies();

            assertEquals(1, policies.size());

            ResourcePolicy policy = policies.get(0);

            assertNotNull(policy.getId());
            assertEquals(created.getId(), policy.getId());
            assertNotNull(realm.getComponent(policy.getId()));
            assertEquals(UserCreationTimeResourcePolicyProviderFactory.ID, policy.getProviderId());
        });
    }

    @Test
    public void testCreateAction() {
        runOnServer.run(session -> {
            RealmModel realm = configureSessionContext(session);
            ResourcePolicyManager manager = new ResourcePolicyManager(session);
            ResourcePolicy policy = manager.addPolicy(new ResourcePolicy(UserCreationTimeResourcePolicyProviderFactory.ID));

            int expectedActionsSize = 5;

            List<ResourceAction> expectedActions = new ArrayList<>();
            for (int i = 0; i < expectedActionsSize; i++) {
                expectedActions.add(UserActionBuilder.builder(DisableUserActionProviderFactory.ID)
                        .after(Duration.ofDays(i + 1))
                        .build());
            }
            manager.updateActions(policy, expectedActions);

            List<ResourceAction> actions = manager.getActions(policy);

            assertEquals(expectedActionsSize, actions.size());

            ResourceAction action = actions.get(0);

            assertNotNull(action.getId());
            assertNotNull(realm.getComponent(action.getId()));
            assertEquals(DisableUserActionProviderFactory.ID, action.getProviderId());
        });
    }

    @Test
    @Disabled("We need to flush component removals to make this test pass. For that, we need to evaluate a flush as per the TODO in the body of this method")
    public void testDeleteActionResetsOrphanedState() {
        //TODO: Evaluate and change org.keycloak.models.jpa.RealmAdapter.removeComponents to flush changes in the persistence context:
        // if (getEntity().getComponents().removeIf(sameParent)) {
        //    em.flush();
        // }

    }

    @Test
    public void testTimeVsPriorityConflictingActions() {
        runOnServer.run(session -> {
            configureSessionContext(session);
            ResourcePolicyManager manager = new ResourcePolicyManager(session);
            ResourcePolicy policy = manager.addPolicy(UserCreationTimeResourcePolicyProviderFactory.ID);

            ResourceAction action1 = UserActionBuilder.builder(DisableUserActionProviderFactory.ID)
                    .after(Duration.ofDays(10))
                    .build();

            ResourceAction action2 = UserActionBuilder.builder(DisableUserActionProviderFactory.ID)
                    .after(Duration.ofDays(5))
                    .build();

            try {
                manager.updateActions(policy, List.of(action1, action2));
                fail("Expected exception was not thrown");
            } catch (BadRequestException expected) {}
        });
    }

    @Test
    public void testPolicyDoesNotFallThroughActionsInSingleRun() {
        // register policy to notify user in 5 days and disable in 10 days
        runOnServer.run((RunOnServer) session -> {
            configureSessionContext(session);
            PolicyBuilder.create()
                    .of(UserCreationTimeResourcePolicyProviderFactory.ID)
                    .withActions(
                            UserActionBuilder.builder(NotifyUserActionProviderFactory.ID)
                                    .after(Duration.ofDays(5))
                                    .build(),
                            UserActionBuilder.builder(DisableUserActionProviderFactory.ID)
                                    .after(Duration.ofDays(10))
                                    .build()
                    ).build(session);
        });

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

            ResourcePolicyStateProvider stateProvider = session.getKeycloakSessionFactory().getProviderFactory(ResourcePolicyStateProvider.class).create(session);
            ResourcePolicyStateProvider.ScheduledAction scheduledAction = stateProvider.getScheduledAction(policy.getId(), user.getId());
            assertNotNull(scheduledAction, "An action should have been scheduled for the user " + user.getUsername());
            assertEquals(notifyAction.getId(), scheduledAction.actionId());

            try {
                // Simulate the user being 12 days old, making them eligible for both actions' time conditions.
                Time.setOffset(Math.toIntExact(Duration.ofDays(12).toSeconds()));
                manager.runScheduledTasks();

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

        // register a policy to notify user in 5 days
        runOnServer.run((RunOnServer) session -> {
            configureSessionContext(session);
            PolicyBuilder.create()
                    .of(UserCreationTimeResourcePolicyProviderFactory.ID)
                    .withConfig("broker-aliases", "someidp")
                    .withActions(
                            UserActionBuilder.builder(NotifyUserActionProviderFactory.ID)
                                    .after(Duration.ofDays(5))
                                    .build(),
                            UserActionBuilder.builder(DisableUserActionProviderFactory.ID)
                                    .after(Duration.ofDays(10))
                                    .build()
                    ).build(session);
        });

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
                policyManager.runScheduledTasks();

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
