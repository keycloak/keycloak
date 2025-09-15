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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.policy.DisableUserActionProviderFactory;
import org.keycloak.models.policy.ResourcePolicyActionRunnerSuccessEvent;
import org.keycloak.models.policy.SetUserAttributeActionProviderFactory;
import org.keycloak.models.policy.UserCreationTimeResourcePolicyProviderFactory;
import org.keycloak.provider.ProviderEventListener;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.resources.policies.ResourcePolicyActionRepresentation;
import org.keycloak.representations.resources.policies.ResourcePolicyRepresentation;
import org.keycloak.storage.UserStoragePrivateUtil;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;

@KeycloakIntegrationTest(config = RLMScheduledTaskServerConfig.class)
public class ActionRunnerScheduledTaskTest {

    private static final String REALM_NAME = "default";

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests")
    RunOnServerClient runOnServer;

    @InjectAdminClient(mode = InjectAdminClient.Mode.BOOTSTRAP)
    Keycloak adminClient;

    @Test
    public void testActionRunnerScheduledTask() {
        for (int i = 0; i < 2; i++) {
            RealmRepresentation realm = new RealmRepresentation();

            realm.setRealm(REALM_NAME.concat("-").concat(String.valueOf(i)));
            realm.setEnabled(true);

            adminClient.realms().create(realm);

            assertActionRuns(realm.getRealm());
        }
    }

    private void assertActionRuns(String realmName) {
        RealmResource realm = adminClient.realm(realmName);

        realm.resources().policies().create(ResourcePolicyRepresentation.create()
                .of(UserCreationTimeResourcePolicyProviderFactory.ID)
                .withActions(
                        ResourcePolicyActionRepresentation.create().of(SetUserAttributeActionProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .withConfig("message", "message")
                                .build(),
                        ResourcePolicyActionRepresentation.create().of(DisableUserActionProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build()
                ).build()).close();

        realm.users().create(UserConfigBuilder.create()
                .username("alice")
                .email("alice@keycloak.org")
                .name("alice", "wonderland")
                .build())
                .close();

        runOnServer.run((session -> {
            KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
            CountDownLatch count = new CountDownLatch(2);

            ProviderEventListener listener = event -> {
                if (event instanceof ResourcePolicyActionRunnerSuccessEvent e) {
                    KeycloakSession s = e.getSession();
                    RealmModel r = s.getContext().getRealm();

                    if (!realmName.equals(r.getName())) {
                        return;
                    }

                    UserProvider provider = UserStoragePrivateUtil.userLocalStorage(s);
                    UserModel user = provider.getUserByUsername(r, "alice");
                    if (user.isEnabled() && user.getAttributes().containsKey("message")) {
                        // notified
                        count.countDown();
                        // force execution of next action
                        user.removeAttribute("message");
                        Time.setOffset(Math.toIntExact(Duration.ofDays(20).toSeconds()));
                    } else if (!user.isEnabled()) {
                        // disabled
                        count.countDown();
                    }
                }
            };

            try {
                sessionFactory.register(listener);
                Time.setOffset(Math.toIntExact(Duration.ofDays(12).toSeconds()));
                System.out.println("Waiting for actions to be run for realm " + realmName);
                assertTrue(count.await(15, TimeUnit.SECONDS));
                System.out.println("... actions run for realm " + realmName);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                sessionFactory.unregister(listener);
                Time.setOffset(0);
            }
        }));
    }
}
