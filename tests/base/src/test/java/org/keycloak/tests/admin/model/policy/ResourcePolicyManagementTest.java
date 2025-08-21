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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.keycloak.common.util.Time;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.policy.DisableUserActionProviderFactory;
import org.keycloak.models.policy.NotifyUserActionProviderFactory;
import org.keycloak.models.policy.ResourceAction;
import org.keycloak.models.policy.ResourcePolicy;
import org.keycloak.models.policy.ResourcePolicyManager;
import org.keycloak.models.policy.ResourcePolicyStateEntity;
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

        runOnServer.run(session -> {
            EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();

            RealmModel realm = configureSessionContext(session);
            ResourcePolicyManager manager = new ResourcePolicyManager(session);
            ResourcePolicyStateProvider stateProvider = session.getProvider(ResourcePolicyStateProvider.class);
            UserModel user = session.users().addUser(realm, "test");

            // Create a policy with two actions
            ResourcePolicy policy = manager.addPolicy(new ResourcePolicy(UserCreationTimeResourcePolicyProviderFactory.ID));
            ResourceAction notify = UserActionBuilder.builder(NotifyUserActionProviderFactory.ID).after(Duration.ofDays(5)).build();
            ResourceAction disable = UserActionBuilder.builder(DisableUserActionProviderFactory.ID).after(Duration.ofDays(10)).build();
            manager.updateActions(policy, List.of(notify, disable));

            // Get the created actions to access their IDs
            List<ResourceAction> createdActions = manager.getActions(policy);
            ResourceAction createdNotifyAction = createdActions.get(0);
            ResourceAction createdDisableAction = createdActions.get(1);

            // --- SIMULATE USER PROGRESS ---
            // Manually set the user's state to have completed 'notify'
            stateProvider.update(policy.getId(), policy.getProviderId(), List.of(user.getId()), createdNotifyAction.getId());
            ResourcePolicyStateEntity.PrimaryKey pk = new ResourcePolicyStateEntity.PrimaryKey(user.getId(), policy.getId());
            assertNotNull(em.find(ResourcePolicyStateEntity.class, pk), "State should exist before the update.");

            // Admin deletes 'notify' by updating the policy with only 'disable'
            manager.updateActions(policy, List.of(createdDisableAction));

            //need to flush and clear the persistance context cache to get correct result in next call
            em.flush();
            em.clear();

            // The user's state record should have been deleted because its last_completed_action (notify) no longer exists.
            assertNull(em.find(ResourcePolicyStateEntity.class, pk), "State record should be deleted when its completed action is removed.");
        });
    }

    @Test
    public void testPolicyDoesNotFallThroughActionsInSingleRun() {
        runOnServer.run(session -> {
            EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();

            RealmModel realm = configureSessionContext(session);
            ResourcePolicyManager manager = new ResourcePolicyManager(session);
            UserModel user = session.users().addUser(realm, "testuser");
            user.setEnabled(true);

            // Create a policy with notify (5 days) and disable (10 days) actions
            ResourcePolicy policy = manager.addPolicy(new ResourcePolicy(UserCreationTimeResourcePolicyProviderFactory.ID));
            ResourceAction notifyAction = UserActionBuilder.builder(NotifyUserActionProviderFactory.ID).after(Duration.ofDays(5)).build();
            ResourceAction disableAction = UserActionBuilder.builder(DisableUserActionProviderFactory.ID).after(Duration.ofDays(10)).build();
            manager.updateActions(policy, List.of(notifyAction, disableAction));

            ResourceAction createdNotifyAction = manager.getActions(policy).get(0);

            try {
                // Simulate the user being 12 days old, making them eligible for both actions' time conditions.
                Time.setOffset(Math.toIntExact(Duration.ofDays(12).toSeconds()));
                manager.runPolicies();

                user = session.users().getUserById(realm, user.getId());

                // Verify that ONLY the first action (notify) was executed.
                assertNotNull(user.getAttributes().get("message"), "The first action (notify) should have run.");
                assertTrue(user.isEnabled(), "The second action (disable) should NOT have run.");

                // Verify that the user's state is correctly paused after the first action.
                ResourcePolicyStateEntity.PrimaryKey pk = new ResourcePolicyStateEntity.PrimaryKey(user.getId(), policy.getId());
                ResourcePolicyStateEntity state = em.find(ResourcePolicyStateEntity.class, pk);

                assertNotNull(state, "A state record should have been created for the user.");
                assertEquals(createdNotifyAction.getId(), state.getScheduledActionId(), "The user's state should be at the first action.");
            } finally {
                Time.setOffset(0);
            }
        });
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
