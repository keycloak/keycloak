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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.keycloak.tests.admin.model.policy.ResourcePolicyManagementTest.findEmailByRecipient;
import static org.keycloak.tests.admin.model.policy.ResourcePolicyManagementTest.findEmailsByRecipient;
import static org.keycloak.tests.admin.model.policy.ResourcePolicyManagementTest.verifyEmailContent;

import java.time.Duration;
import java.util.List;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.policy.DisableUserActionProviderFactory;
import org.keycloak.models.policy.NotifyUserActionProviderFactory;
import org.keycloak.models.policy.ResourceOperationType;
import org.keycloak.models.policy.ResourcePolicyManager;
import org.keycloak.models.policy.UserSessionRefreshTimeResourcePolicyProviderFactory;
import org.keycloak.representations.resources.policies.ResourcePolicyActionRepresentation;
import org.keycloak.representations.resources.policies.ResourcePolicyRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.realm.UserConfigBuilder;
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

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectWebDriver
    WebDriver driver;

    @InjectPage
    LoginPage loginPage;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectMailServer
    private MailServer mailServer;

    @BeforeEach
    public void onBefore() {
        oauth.realm("default");

        runOnServer.run(session -> {
            ResourcePolicyManager manager = new ResourcePolicyManager(session);
            manager.removePolicies();
        });
    }

    @Test
    public void testDisabledUserAfterInactivityPeriod() {
        managedRealm.admin().resources().policies().create(ResourcePolicyRepresentation.create()
                .of(UserSessionRefreshTimeResourcePolicyProviderFactory.ID)
                .onEvent(ResourceOperationType.LOGIN.toString())
                .withActions(
                        ResourcePolicyActionRepresentation.create().of(NotifyUserActionProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        ResourcePolicyActionRepresentation.create().of(DisableUserActionProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build()
                ).build()).close();

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

            // running the scheduled tasks now shouldn't pick up any action as none are due to run yet
            manager.runScheduledActions();
            user = session.users().getUserByUsername(realm, username);
            assertTrue(user.isEnabled());

            try {
                // set offset to 6 days - notify action should run now
                Time.setOffset(Math.toIntExact(Duration.ofDays(5).toSeconds()));
                manager.runScheduledActions();
                user = session.users().getUserByUsername(realm, username);
                assertTrue(user.isEnabled());
            } finally {
                Time.setOffset(0);
            }
        }));

        // Verify that the notify action was executed by checking email was sent
        MimeMessage testUserMessage = findEmailByRecipient(mailServer, "master-admin@email.org");
        assertNotNull(testUserMessage, "The first action (notify) should have sent an email.");

        mailServer.runCleanup();

        // trigger a login event that should reset the flow of the policy
        oauth.openLoginForm();

        runOnServer.run((session -> {
            try {
                // setting the offset to 11 days should not run the second action as we re-started the flow on login
                RealmModel realm = configureSessionContext(session);
                Time.setOffset(Math.toIntExact(Duration.ofDays(11).toSeconds()));
                ResourcePolicyManager manager = new ResourcePolicyManager(session);
                manager.runScheduledActions();
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
                manager.runScheduledActions();
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
        managedRealm.admin().resources().policies().create(ResourcePolicyRepresentation.create()
                .of(UserSessionRefreshTimeResourcePolicyProviderFactory.ID)
                .onEvent(ResourceOperationType.LOGIN.toString())
                .withActions(
                        ResourcePolicyActionRepresentation.create().of(NotifyUserActionProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .withConfig("custom_subject_key", "notifier1_subject")
                                .withConfig("custom_message", "notifier1_message")
                                .build()
                ).of(UserSessionRefreshTimeResourcePolicyProviderFactory.ID)
                .onEvent(ResourceOperationType.LOGIN.toString())
                .withActions(
                        ResourcePolicyActionRepresentation.create().of(NotifyUserActionProviderFactory.ID)
                                .after(Duration.ofDays(10))
                                .withConfig("custom_subject_key", "notifier2_subject")
                                .withConfig("custom_message", "notifier2_message")
                                .build())
                .build()).close();

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

            try {
                Time.setOffset(Math.toIntExact(Duration.ofDays(7).toSeconds()));
                manager.runScheduledActions();
                user = users.getUserByUsername(realm, username);
                assertTrue(user.isEnabled());
            } finally {
                Time.setOffset(0);
            }
        });

        // Verify that the first notify action was executed by checking email was sent
        List<MimeMessage> testUserMessages = findEmailsByRecipient(mailServer, "master-admin@email.org");
        // Only one notify message should be sent
        assertEquals(1, testUserMessages.size());
        assertNotNull(testUserMessages.get(0), "The first action (notify) should have sent an email.");
        verifyEmailContent(testUserMessages.get(0), "master-admin@email.org", "notifier1_subject", "notifier1_message");

        mailServer.runCleanup();

        runOnServer.run(session -> {
            RealmModel realm = configureSessionContext(session);
            ResourcePolicyManager manager = new ResourcePolicyManager(session);

            UserModel user = session.users().getUserByUsername(realm, username);
            try {
                Time.setOffset(Math.toIntExact(Duration.ofDays(11).toSeconds()));
                manager.runScheduledActions();
                user = session.users().getUserByUsername(realm, username);
                assertTrue(user.isEnabled());
            } finally {
                Time.setOffset(0);
            }
        });

        // Verify that the second notify action was executed by checking email was sent
        testUserMessages = findEmailsByRecipient(mailServer, "master-admin@email.org");
        // Only one notify message should be sent
        assertEquals(1, testUserMessages.size());
        assertNotNull(testUserMessages.get(0), "The second action (notify) should have sent an email.");
        verifyEmailContent(testUserMessages.get(0), "master-admin@email.org", "notifier2_subject", "notifier2_message");
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
