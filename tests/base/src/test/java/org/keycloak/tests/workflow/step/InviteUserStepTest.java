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
import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.mail.internet.MimeMessage;
import jakarta.ws.rs.core.Response;

import org.keycloak.common.util.Base64Url;
import org.keycloak.models.workflow.InviteUserStepProvider;
import org.keycloak.models.workflow.InviteUserStepProviderFactory;
import org.keycloak.models.workflow.events.UserCreatedWorkflowEventFactory;
import org.keycloak.representations.idm.ClientRepresentation;
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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
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

    @Test
    public void testCreateFailsWhenClientDoesNotExist() {
        try (Response response = managedRealm.admin().workflows().create(WorkflowRepresentation.withName("invite-bad")
                .onEvent(UserCreatedWorkflowEventFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create().of(InviteUserStepProviderFactory.ID)
                                .withConfig(InviteUserStepProvider.CONFIG_CLIENT_ID, "does-not-exist")
                                .build()
                ).build())) {
            assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        }
    }

    @Test
    public void testCreateFailsWhenRedirectUriInvalidForClient() {
        createClient("invite-client", "https://app.example.com/*");

        try (Response response = managedRealm.admin().workflows().create(WorkflowRepresentation.withName("invite-bad")
                .onEvent(UserCreatedWorkflowEventFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create().of(InviteUserStepProviderFactory.ID)
                                .withConfig(InviteUserStepProvider.CONFIG_CLIENT_ID, "invite-client")
                                .withConfig(InviteUserStepProvider.CONFIG_REDIRECT_URI, "https://not-allowed.example.com/")
                                .build()
                ).build())) {
            assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        }
    }

    @Test
    public void testInviteSkippedForDisabledUser() {
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("invite")
                .onEvent(UserCreatedWorkflowEventFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create().of(InviteUserStepProviderFactory.ID).build()
                ).build()).close();

        managedRealm.admin().users().create(UserBuilder.create()
                .username("disabled").email("disabled@example.com").enabled(false).build()).close();

        assertNull(findEmailByRecipientContaining(mailServer, "disabled@example.com"));
    }

    @Test
    public void testInviteEmailWithClientAndRedirectUri() throws IOException {
        createClient("invite-client", "https://app.example.com/*");

        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("invite")
                .onEvent(UserCreatedWorkflowEventFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create().of(InviteUserStepProviderFactory.ID)
                                .withConfig(InviteUserStepProvider.CONFIG_CLIENT_ID, "invite-client")
                                .withConfig(InviteUserStepProvider.CONFIG_REDIRECT_URI, "https://app.example.com/welcome")
                                .build()
                ).build()).close();

        managedRealm.admin().users().create(UserBuilder.create()
                .username("invitee").email("invitee@example.com").build()).close();

        mailServer.waitForIncomingEmail(10_000, 1);
        MimeMessage message = findEmailByRecipient(mailServer, "invitee@example.com");
        assertNotNull(message);

        String link = MailUtils.getLink(MailUtils.getBody(message).getHtml());
        String tokenPayload = decodeActionTokenPayload(link);
        // 'azp' carries the token's issuedFor client, 'reduri' the post-action redirect.
        assertThat(tokenPayload, containsString("\"azp\":\"invite-client\""));
        assertThat(tokenPayload, containsString("https://app.example.com/welcome"));
    }

    @Test
    public void testInviteEmailWithCustomActions() throws IOException {
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("invite")
                .onEvent(UserCreatedWorkflowEventFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create().of(InviteUserStepProviderFactory.ID)
                                .withConfig(InviteUserStepProvider.CONFIG_ACTIONS, "VERIFY_EMAIL")
                                .build()
                ).build()).close();

        managedRealm.admin().users().create(UserBuilder.create()
                .username("customactions").email("customactions@example.com").build()).close();

        mailServer.waitForIncomingEmail(10_000, 1);
        MimeMessage message = findEmailByRecipient(mailServer, "customactions@example.com");
        assertNotNull(message);

        String tokenPayload = decodeActionTokenPayload(MailUtils.getLink(MailUtils.getBody(message).getHtml()));
        // Only the configured action must be present - the UPDATE_PASSWORD default must not leak in.
        assertThat(tokenPayload, containsString("VERIFY_EMAIL"));
        assertThat(tokenPayload, not(containsString("UPDATE_PASSWORD")));
    }

    private void createClient(String clientId, String redirectUri) {
        ClientRepresentation rep = new ClientRepresentation();
        rep.setClientId(clientId);
        rep.setName(clientId);
        rep.setProtocol("openid-connect");
        rep.setEnabled(true);
        rep.setStandardFlowEnabled(true);
        rep.setRedirectUris(List.of(redirectUri));
        managedRealm.admin().clients().create(rep).close();
    }

    /**
     * Extracts the {@code key} action-token from an invitation link and decodes the JWT
     * payload (the middle, unencrypted segment) into its JSON string form so tests can
     * assert on the claims the step put into the token.
     */
    private static String decodeActionTokenPayload(String link) {
        int keyIndex = link.indexOf("key=");
        assertThat(keyIndex, is(not(-1)));
        String token = link.substring(keyIndex + "key=".length());
        int ampersand = token.indexOf('&');
        if (ampersand != -1) {
            token = token.substring(0, ampersand);
        }
        String[] segments = token.split("\\.");
        assertThat(segments.length, greaterThanOrEqualTo(2));
        return new String(Base64Url.decode(segments[1]), StandardCharsets.UTF_8);
    }
}
