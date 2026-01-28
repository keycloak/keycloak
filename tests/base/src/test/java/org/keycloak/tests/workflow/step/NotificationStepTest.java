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

package org.keycloak.tests.workflow.step;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import jakarta.mail.internet.MimeMessage;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.workflow.DeleteUserStepProviderFactory;
import org.keycloak.models.workflow.DisableUserStepProviderFactory;
import org.keycloak.models.workflow.NotifyUserStepProviderFactory;
import org.keycloak.representations.workflows.StepExecutionStatus;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.MailUtils;
import org.keycloak.tests.workflow.AbstractWorkflowTest;
import org.keycloak.tests.workflow.config.WorkflowsBlockingServerConfig;

import org.junit.jupiter.api.Test;

import static org.keycloak.models.workflow.ResourceOperationType.USER_CREATED;
import static org.keycloak.tests.workflow.util.EmailTestUtils.findEmailByRecipient;
import static org.keycloak.tests.workflow.util.EmailTestUtils.findEmailByRecipientContaining;
import static org.keycloak.tests.workflow.util.EmailTestUtils.verifyEmailContent;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the execution of the 'notify-user' workflow step.
 */
@KeycloakIntegrationTest(config = WorkflowsBlockingServerConfig.class)
public class NotificationStepTest extends AbstractWorkflowTest {

    @InjectMailServer
    private MailServer mailServer;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectAdminClient(ref = "managed", realmRef = "managedRealm")
    Keycloak adminClient;

    @Test
    public void testNotifyUserStepSendsEmailWithDefaultDisableMessage() {
        // Create workflow: disable at 10 days, notify 3 days before (at day 7)
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(USER_CREATED.name())
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

        // Simulate user being 7 days old (eligible for notify step)
        runScheduledSteps(Duration.ofDays(7));

        // Verify email was sent to our test user
        MimeMessage testUserMessage = findEmailByRecipient(mailServer, "test@example.com");
        assertNotNull(testUserMessage, "No email found for test@example.com");
        verifyEmailContent(testUserMessage, "test@example.com", "Disable", "John", "3", "inactivity");

        mailServer.runCleanup();
    }

    @Test
    public void testNotifyUserStepSendsEmailWithDefaultDeleteMessage() {
        // Create workflow: delete at 30 days, notify 15 days before (at day 15)
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(USER_CREATED.name())
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

        // Simulate user being 15 days old
        runScheduledSteps(Duration.ofDays(15));

        // Verify email was sent to our test user
        MimeMessage testUserMessage = findEmailByRecipient(mailServer, "test2@example.com");
        assertNotNull(testUserMessage, "No email found for test2@example.com");
        verifyEmailContent(testUserMessage, "test2@example.com", "Deletion", "Jane", "15", "inactivity", "permanently deleted");

        mailServer.runCleanup();
    }

    @Test
    public void testNotifyUserStepSkipsUsersWithoutEmailButLogsWarning() {
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(USER_CREATED.name())
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build(),
                        WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(10))
                                .build()
                ).build()).close();

        String userId;
        try (Response response = managedRealm.admin().users().create(UserConfigBuilder.create().username("testuser4").name("NoEmail", "").build())) {
            userId = ApiUtil.getCreatedId(response);
        }

        runScheduledSteps(Duration.ofDays(5));
        List<WorkflowRepresentation> scheduledWorkflows = managedRealm.admin().workflows().getScheduledWorkflows(userId);
        assertThat(scheduledWorkflows, hasSize(1));
        List<WorkflowStepRepresentation> steps = scheduledWorkflows.get(0).getSteps();
        assertThat(steps, hasSize(2));
        assertThat(steps.get(0).getExecutionStatus(), is(StepExecutionStatus.COMPLETED));
        assertThat(steps.get(1).getExecutionStatus(), is(StepExecutionStatus.PENDING));
        // pending step should the disable step
        assertEquals(DisableUserStepProviderFactory.ID, steps.get(1).getUses());

        // Should NOT send email to user without email address
        MimeMessage testUserMessage = findEmailByRecipientContaining(mailServer, "testuser4");
        assertNull(testUserMessage, "No email should be sent to user without email address");
    }

    @Test
    public void testCompleteUserLifecycleWithMultipleNotifications() {
        // Create workflow: just disable at 30 days with one notification before
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(USER_CREATED.name())
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

        // Day 15: First notification - this should run the notify step and schedule the disable step
        runScheduledSteps(Duration.ofDays(15));

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, "testuser5");
            // Check that user is still enabled after notification
            user = session.users().getUserById(realm, user.getId());
            assertTrue(user.isEnabled(), "User should still be enabled after notification");
        });

        // Day 30 + 15 minutes: Disable user - run 15 minutes after the scheduled time to ensure it's due
        runScheduledSteps(Duration.ofDays(30).plus(Duration.ofMinutes(15)));

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, "testuser5");
            // Verify user is disabled
            user = session.users().getUserById(realm, user.getId());
            assertNotNull(user, "User should still exist after disable");
            assertFalse(user.isEnabled(), "User should be disabled");
        });

        // Verify notification was sent
        MimeMessage testUserMessage = findEmailByRecipient(mailServer, "testuser5@example.com");
        assertNotNull(testUserMessage, "No email found for testuser5@example.com");
        verifyEmailContent(testUserMessage, "testuser5@example.com", "Disable", "TestUser5", "15", "inactivity");

        mailServer.runCleanup();
    }


    @Test
    public void testNotifyUserStepWithCustomMessageOverride() throws IOException {
        // Create workflow: disable at 7 days, notify 2 days before (at day 5) with custom message
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(USER_CREATED.name())
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .withConfig("message", "<p>Dear ${user.firstName} ${user.lastName}, </p>\n" +
                                        "\n" +
                                        "        <p>Welcome to ${realm.name}!</p>\n" +
                                        "        <p>The next step is scheduled to ${workflow.daysUntilNextStep} days.</p>\n" +
                                        "\n" +
                                        "        <p>\n" +
                                        "           Best regards,<br/>\n" +
                                        "           ${realm.name} team\n" +
                                        "        </p> ")
                                .withConfig("subject", "customComplianceSubject")
                                .build(),
                        WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(7))
                                .build()
                ).build()).close();

        try {
            managedRealm.admin().users().create(
                    UserConfigBuilder.create()
                            .username("testuser3")
                            .email("test3@example.com")
                            .name("Bob", "Doe")
                            .build()
            ).close();

            MimeMessage message = mailServer.getLastReceivedMessage();
            assertNotNull(message);

            MailUtils.EmailBody body = MailUtils.getBody(message);

            for (String content : List.of(body.getText(), body.getHtml())) {
                assertTrue(content.contains("Dear Bob Doe,"));
                assertTrue(content.contains("Welcome to " + managedRealm.getName() + "!"));
                assertTrue(content.contains("The next step is scheduled to 7 days."));
            }
        } finally {
            mailServer.runCleanup();
        }
    }

    @Test
    public void testNotifyUserStepWithSendToConfiguration() throws Exception {
        // Create workflow: notify immediately with send_to
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(USER_CREATED.name())
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .withConfig("to", "admin@example.com")
                                .withConfig("reason", "manual-review")
                                .build(),
                        WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(7))
                                .build()
                ).build()).close();

        managedRealm.admin().users().create(UserConfigBuilder.create().username("userXYZ").email("user@example.com").name("User", "XYZ").build()).close();

        // Verify email was sent to admin@example.com
        MimeMessage message = mailServer.getLastReceivedMessage();
        assertNotNull(message, "Email should be sent");

        assertEquals("admin@example.com", message.getRecipients(jakarta.mail.Message.RecipientType.TO)[0].toString());

        // Use helper to verify content - note that we pass admin email as recipient check
        verifyEmailContent(message, "admin@example.com", "Disable", "User", "7", "manual-review");

        mailServer.runCleanup();
    }
}
