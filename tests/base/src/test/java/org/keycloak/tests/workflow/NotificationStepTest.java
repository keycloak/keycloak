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

package org.keycloak.tests.workflow;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import jakarta.mail.internet.MimeMessage;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.models.workflow.DisableUserStepProviderFactory;
import org.keycloak.models.workflow.NotifyUserStepProviderFactory;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.tests.utils.MailUtils;

import org.junit.jupiter.api.Test;

import static org.keycloak.models.workflow.ResourceOperationType.USER_CREATED;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = WorkflowsBlockingServerConfig.class)
public class NotificationStepTest extends AbstractWorkflowTest {

    @InjectMailServer
    private MailServer mailServer;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectAdminClient(ref = "managed", realmRef = "managedRealm")
    Keycloak adminClient;

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
}
