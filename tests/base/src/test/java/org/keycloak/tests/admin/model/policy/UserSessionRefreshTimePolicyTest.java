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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.policy.DisableUserActionProviderFactory;
import org.keycloak.models.policy.NotifyUserActionProviderFactory;
import org.keycloak.models.policy.ResourcePolicyManager;
import org.keycloak.models.policy.UserActionBuilder;
import org.keycloak.models.policy.UserLastSessionRefreshTimeResourcePolicyProviderFactory;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.openqa.selenium.WebDriver;

@KeycloakIntegrationTest(config = RLMServerConfig.class)
public class UserSessionRefreshTimePolicyTest {

    private static final String REALM_NAME = "default";

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests")
    RunOnServerClient runOnServer;

    @InjectUser(ref = "alice", config = DefaultUserConfig.class, lifecycle = LifeCycle.METHOD)
    private ManagedUser userAlice;

    @InjectWebDriver
    WebDriver driver;

    @InjectPage
    LoginPage loginPage;

    @InjectOAuthClient
    OAuthClient oauth;

    @BeforeEach
    public void onBefore() {
        oauth.realm("default");
    }

    @Test
    @Disabled
    public void testDisabledUserAfterInactivityPeriod() {
        runOnServer.run((RunOnServer) session -> {
            configureSessionContext(session);
            PolicyBuilder.create()
                    .of(UserLastSessionRefreshTimeResourcePolicyProviderFactory.ID)
                    .withActions(
                            UserActionBuilder.builder(NotifyUserActionProviderFactory.ID)
                                    .after(Duration.ofDays(5))
                                    .build(),
                            UserActionBuilder.builder(DisableUserActionProviderFactory.ID)
                                    .after(Duration.ofDays(10))
                                    .build()
                    ).build(session);
        });

        oauth.openLoginForm();
        loginPage.fillLogin("alice", "alice");
        loginPage.submit();
        assertTrue(driver.getPageSource().contains("Happy days"));

        // test run policy
        runOnServer.run((session -> {
            RealmModel realm = configureSessionContext(session);
            ResourcePolicyManager manager = new ResourcePolicyManager(session);

            UserModel user = session.users().getUserByUsername(realm, "alice");
            assertTrue(user.isEnabled());
            assertNull(user.getAttributes().get("message"));

            manager.runScheduledTasks();
            user = session.users().getUserByUsername(realm, "alice");
            assertTrue(user.isEnabled());
            assertNull(user.getAttributes().get("message"));

            try {
                manager = new ResourcePolicyManager(session);
                Time.setOffset(Math.toIntExact(Duration.ofDays(7).toSeconds()));
                manager.runScheduledTasks();
                user = session.users().getUserByUsername(realm, "alice");
                assertTrue(user.isEnabled());
                assertNotNull(user.getAttributes().get("message"));
            } finally {
                Time.setOffset(0);
            }
        }));

        oauth.openLoginForm();

        // test run policy
        runOnServer.run((session -> {
            try {
                RealmModel realm = configureSessionContext(session);
                Time.setOffset(Math.toIntExact(Duration.ofDays(11).toSeconds()));
                ResourcePolicyManager manager = new ResourcePolicyManager(session);
                manager.runScheduledTasks();
                UserModel user = session.users().getUserByUsername(realm, "alice");
                assertTrue(user.isEnabled());
            } finally {
                Time.setOffset(0);
            }
        }));

        // test run policy
        runOnServer.run((session -> {
            try {
                RealmModel realm = configureSessionContext(session);
                Time.setOffset(Math.toIntExact(Duration.ofDays(21).toSeconds()));
                ResourcePolicyManager manager = new ResourcePolicyManager(session);
                manager.runScheduledTasks();
                UserModel user = session.users().getUserByUsername(realm, "alice");
                assertFalse(user.isEnabled());
            } finally {
                Time.setOffset(0);
            }
        }));
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
