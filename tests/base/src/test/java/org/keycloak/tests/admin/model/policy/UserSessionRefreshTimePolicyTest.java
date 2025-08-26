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
import org.junit.jupiter.api.Test;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.policy.DisableUserActionProviderFactory;
import org.keycloak.models.policy.NotifyUserActionProviderFactory;
import org.keycloak.models.policy.ResourcePolicyManager;
import org.keycloak.models.policy.UserActionBuilder;
import org.keycloak.models.policy.UserSessionRefreshTimeResourcePolicyProviderFactory;
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
    public void testDisabledUserAfterInactivityPeriod() {
        runOnServer.run((RunOnServer) session -> {
            configureSessionContext(session);
            PolicyBuilder.create()
                    .of(UserSessionRefreshTimeResourcePolicyProviderFactory.ID)
                    .withActions(
                            UserActionBuilder.builder(NotifyUserActionProviderFactory.ID)
                                    .after(Duration.ofDays(5))
                                    .build(),
                            UserActionBuilder.builder(DisableUserActionProviderFactory.ID)
                                    .after(Duration.ofDays(10))
                                    .build()
                    ).build(session);
        });

        // login with alice - this will attach the policy to the user and schedule the first action
        oauth.openLoginForm();
        String username = userAlice.getUsername();
        loginPage.fillLogin(username, userAlice.getPassword());
        loginPage.submit();
        assertTrue(driver.getPageSource() != null && driver.getPageSource().contains("Happy days"));

        // test running the scheduled actions
        runOnServer.run((session -> {
            RealmModel realm = configureSessionContext(session);
            ResourcePolicyManager manager = new ResourcePolicyManager(session);

            UserModel user = session.users().getUserByUsername(realm, username);
            assertTrue(user.isEnabled());
            assertNull(user.getAttributes().get("message"));

            // running the scheduled tasks now shouldn't pick up any action as none are due to run yet
            manager.runScheduledTasks();
            user = session.users().getUserByUsername(realm, username);
            assertTrue(user.isEnabled());
            assertNull(user.getAttributes().get("message"));

            try {
                // set offset to 6 days - notify action should run now
                Time.setOffset(Math.toIntExact(Duration.ofDays(5).toSeconds()));
                manager.runScheduledTasks();
                user = session.users().getUserByUsername(realm, username);
                assertTrue(user.isEnabled());
                assertNotNull(user.getAttributes().get("message"));
            } finally {
                Time.setOffset(0);
            }
        }));

        // trigger a login event that should reset the flow of the policy
        oauth.openLoginForm();

        runOnServer.run((session -> {
            try {
                // setting the offset to 11 days should not run the second action as we re-started the flow on login
                RealmModel realm = configureSessionContext(session);
                Time.setOffset(Math.toIntExact(Duration.ofDays(11).toSeconds()));
                ResourcePolicyManager manager = new ResourcePolicyManager(session);
                manager.runScheduledTasks();
                UserModel user = session.users().getUserByUsername(realm, username);
                assertTrue(user.isEnabled());
            } finally {
                Time.setOffset(0);
            }

            try {
                // first action has run and the next action should be triggered after 5 more days (time difference between the actions)
                RealmModel realm = configureSessionContext(session);
                Time.setOffset(Math.toIntExact(Duration.ofDays(17).toSeconds()));
                ResourcePolicyManager manager = new ResourcePolicyManager(session);
                manager.runScheduledTasks();
                UserModel user = session.users().getUserByUsername(realm, username);
                // second action should have run and the user should be disabled now
                assertFalse(user.isEnabled());
            } finally {
                Time.setOffset(0);
            }
        }));
    }

    @Test
    public void testMultiplePolicies() {
        runOnServer.run(session -> {
                PolicyBuilder.create()
                    .of(UserSessionRefreshTimeResourcePolicyProviderFactory.ID)
                    .withActions(
                            UserActionBuilder.builder(NotifyUserActionProviderFactory.ID)
                                    .after(Duration.ofDays(5))
                                    .withConfig("message_key", "notifier1")
                                    .build()
                    )
                    .build(session);
                PolicyBuilder.create()
                    .of(UserSessionRefreshTimeResourcePolicyProviderFactory.ID)
                    .withActions(
                            UserActionBuilder.builder(NotifyUserActionProviderFactory.ID)
                                    .after(Duration.ofDays(10))
                                    .withConfig("message_key", "notifier2")
                                    .build())
                    .build(session);
        });

        // perform a login to associate the policies with the new user.
        oauth.openLoginForm();
        String username = userAlice.getUsername();
        loginPage.fillLogin(username, userAlice.getPassword());
        loginPage.submit();
        assertTrue(driver.getPageSource() != null && driver.getPageSource().contains("Happy days"));

        runOnServer.run(session -> {
            RealmModel realm = configureSessionContext(session);
            ResourcePolicyManager manager = new ResourcePolicyManager(session);

            UserProvider users = session.users();
            UserModel user = users.getUserByUsername(realm, username);
            assertTrue(user.isEnabled());
            assertNull(user.getFirstAttribute("notifier1"));
            assertNull(user.getFirstAttribute("notifier2"));

            try {
                Time.setOffset(Math.toIntExact(Duration.ofDays(7).toSeconds()));
                manager.runScheduledTasks();
                user = users.getUserByUsername(realm, username);
                assertTrue(user.isEnabled());
                assertNotNull(user.getFirstAttribute("notifier1"));
                assertNull(user.getFirstAttribute("notifier2"));
                user.removeAttribute("notifier1");
            } finally {
                Time.setOffset(0);
            }

            try {
                Time.setOffset(Math.toIntExact(Duration.ofDays(11).toSeconds()));
                manager.runScheduledTasks();
                user = users.getUserByUsername(realm, username);
                assertTrue(user.isEnabled());
                assertNotNull(user.getFirstAttribute("notifier2"));
                assertNull(user.getFirstAttribute("notifier1"));
                user.removeAttribute("notifier2");
            } finally {
                Time.setOffset(0);
            }

            try {
                manager.runScheduledTasks();
                assertNull(user.getFirstAttribute("notifier1"));
                assertNull(user.getFirstAttribute("notifier2"));
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
