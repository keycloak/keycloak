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

import jakarta.mail.internet.MimeMessage;
import jakarta.ws.rs.core.Response;

import org.keycloak.models.workflow.InviteUserStepProvider;
import org.keycloak.models.workflow.InviteUserStepProviderFactory;
import org.keycloak.models.workflow.events.UserCreatedWorkflowEventFactory;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.tests.utils.MailUtils;
import org.keycloak.tests.workflow.AbstractWorkflowTest;
import org.keycloak.tests.workflow.config.InviteUserStepServerConfig;

import org.junit.jupiter.api.Test;

import static org.keycloak.tests.workflow.util.EmailTestUtils.findEmailByRecipient;
import static org.keycloak.tests.workflow.util.EmailTestUtils.findEmailByRecipientContaining;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@KeycloakIntegrationTest(config = InviteUserStepServerConfig.class)
public class InviteUserStepTest extends AbstractWorkflowTest {

    @InjectMailServer
    private MailServer mailServer;

    @Test
    public void testInviteEmailSentOnUserCreation() throws IOException {
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("invite")
                .onEvent(UserCreatedWorkflowEventFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create().of(InviteUserStepProviderFactory.ID).build()
                ).build()).close();

        managedRealm.admin().users().create(UserBuilder.create()
                .username("newuser").email("newuser@example.com").build()).close();

        mailServer.waitForIncomingEmail(10_000, 1);
        MimeMessage message = findEmailByRecipient(mailServer, "newuser@example.com");
        assertNotNull(message);

        String link = MailUtils.getLink(MailUtils.getBody(message).getHtml());
        assertThat(link, startsWith(InviteUserStepServerConfig.HOSTNAME_URL
                + "/realms/" + managedRealm.getName() + "/login-actions/action-token"));
        assertThat(link, containsString("key="));
    }

    @Test
    public void testInviteSkippedForUserWithoutEmail() {
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("invite")
                .onEvent(UserCreatedWorkflowEventFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create().of(InviteUserStepProviderFactory.ID).build()
                ).build()).close();

        managedRealm.admin().users().create(UserBuilder.create().username("noemail").build()).close();

        assertNull(findEmailByRecipientContaining(mailServer, "noemail"));
    }

    @Test
    public void testCreateFailsWhenActionUnknown() {
        try (Response response = managedRealm.admin().workflows().create(WorkflowRepresentation.withName("invite-bad")
                .onEvent(UserCreatedWorkflowEventFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create().of(InviteUserStepProviderFactory.ID)
                                .withConfig(InviteUserStepProvider.CONFIG_ACTIONS, "NOT_AN_ACTION")
                                .build()
                ).build())) {
            assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        }
    }

    @Test
    public void testCreateFailsWhenRedirectUriHasNoClient() {
        try (Response response = managedRealm.admin().workflows().create(WorkflowRepresentation.withName("invite-bad")
                .onEvent(UserCreatedWorkflowEventFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create().of(InviteUserStepProviderFactory.ID)
                                .withConfig(InviteUserStepProvider.CONFIG_REDIRECT_URI, "https://app.example.com/")
                                .build()
                ).build())) {
            assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        }
    }
}
