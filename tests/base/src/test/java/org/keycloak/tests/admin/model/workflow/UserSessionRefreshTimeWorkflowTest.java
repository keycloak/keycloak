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

import java.time.Duration;
import java.util.List;

import jakarta.mail.internet.MimeMessage;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.workflow.DisableUserStepProviderFactory;
import org.keycloak.models.workflow.NotifyUserStepProviderFactory;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.realm.UserConfigBuilder;

import org.junit.jupiter.api.Test;

import static org.keycloak.models.workflow.ResourceOperationType.USER_ADDED;
import static org.keycloak.models.workflow.ResourceOperationType.USER_LOGGED_IN;
import static org.keycloak.tests.admin.model.workflow.WorkflowManagementTest.findEmailByRecipient;
import static org.keycloak.tests.admin.model.workflow.WorkflowManagementTest.findEmailsByRecipient;
import static org.keycloak.tests.admin.model.workflow.WorkflowManagementTest.verifyEmailContent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = WorkflowsBlockingServerConfig.class)
public class UserSessionRefreshTimeWorkflowTest extends AbstractWorkflowTest {

    @InjectUser(ref = "alice", config = DefaultUserConfig.class, lifecycle = LifeCycle.METHOD, realmRef = DEFAULT_REALM_NAME)
    private ManagedUser userAlice;

    @InjectMailServer
    private MailServer mailServer;

    @Test
    public void testDisabledUserAfterInactivityPeriod() {
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(USER_ADDED.toString(), USER_LOGGED_IN.toString())
                .concurrency().cancelIfRunning() // this setting enables restarting the workflow
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build()
                ).build()).close();

        // login with alice - this will attach the workflow to the user and schedule the first step
        oauth.openLoginForm();
        String username = userAlice.getUsername();
        loginPage.fillLogin(username, userAlice.getPassword());
        loginPage.submit();
        assertTrue(driver.getPageSource() != null && driver.getPageSource().contains("Happy days"));

        // test running the scheduled steps
        runOnServer.run((session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, username);
            assertTrue(user.isEnabled());
        }));

        // running the scheduled tasks now shouldn't pick up any step as none are due to run yet
        runScheduledSteps(Duration.ZERO);

        runOnServer.run((session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, username);
            assertTrue(user.isEnabled());
        }));

        // set offset to 6 days - notify step should run now
        runScheduledSteps(Duration.ofDays(5));

        runOnServer.run((session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, username);
            assertTrue(user.isEnabled());
        }));

        // Verify that the notify step was executed by checking email was sent
        MimeMessage testUserMessage = findEmailByRecipient(mailServer, "master-admin@email.org");
        assertNotNull(testUserMessage, "The first step (notify) should have sent an email.");

        mailServer.runCleanup();

        // trigger a login event that should reset the flow of the workflow
        oauth.openLoginForm();

        // setting the offset to 11 days should not run the second step as we re-started the flow on login
        runScheduledSteps(Duration.ofDays(11));

        runOnServer.run((session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, username);
            assertTrue(user.isEnabled());
        }));

        // first step has run and the next step should be triggered after 5 more days (time difference between the steps)
        runScheduledSteps(Duration.ofDays(17));

        runOnServer.run((session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, username);
            // second step should have run and the user should be disabled now
            assertFalse(user.isEnabled());
        }));
    }

    @Test
    public void testMultipleWorkflows() {
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(USER_ADDED.toString(), USER_LOGGED_IN.toString())
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .withConfig("custom_subject_key", "notifier1_subject")
                                .withConfig("custom_message", "notifier1_message")
                                .build())
                .build()).close();
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow_2")
                .onEvent(USER_ADDED.toString(), USER_LOGGED_IN.toString())
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(10))
                                .withConfig("custom_subject_key", "notifier2_subject")
                                .withConfig("custom_message", "notifier2_message")
                                .build())
                .build()).close();

        // perform a login to associate the workflows with the new user.
        oauth.openLoginForm();
        String username = userAlice.getUsername();
        loginPage.fillLogin(username, userAlice.getPassword());
        loginPage.submit();
        assertTrue(driver.getPageSource() != null && driver.getPageSource().contains("Happy days"));

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, username);
            assertTrue(user.isEnabled());
        });

        runScheduledSteps(Duration.ofDays(7));

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, username);
            assertTrue(user.isEnabled());
        });

        // Verify that the first notify step was executed by checking email was sent
        List<MimeMessage> testUserMessages = findEmailsByRecipient(mailServer, "master-admin@email.org");
        // Only one notify message should be sent
        assertEquals(1, testUserMessages.size());
        assertNotNull(testUserMessages.get(0), "The first step (notify) should have sent an email.");
        verifyEmailContent(testUserMessages.get(0), "master-admin@email.org", "notifier1_subject", "notifier1_message");

        mailServer.runCleanup();

        runScheduledSteps(Duration.ofDays(11));

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, username);
            assertTrue(user.isEnabled());
        });

        // Verify that the second notify step was executed by checking email was sent
        testUserMessages = findEmailsByRecipient(mailServer, "master-admin@email.org");
        // Only one notify message should be sent
        assertEquals(1, testUserMessages.size());
        assertNotNull(testUserMessages.get(0), "The second step (notify) should have sent an email.");
        verifyEmailContent(testUserMessages.get(0), "master-admin@email.org", "notifier2_subject", "notifier2_message");
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
