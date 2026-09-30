/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.organization.admin;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import jakarta.mail.internet.MimeMessage;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.OrganizationInvitationRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.ProceedPage;
import org.keycloak.testframework.ui.page.RegisterPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.MailUtils;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@KeycloakIntegrationTest
public class OrganizationInvitationRoleTest extends AbstractOrganizationTest {

    @InjectMailServer
    MailServer mailServer;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectPage
    ProceedPage proceedPage;

    @InjectPage
    RegisterPage registerPage;

    @Test
    public void testInviteExistingUserGrantsRoles() throws IOException {
        OrganizationResource organization = realm.admin().organizations().get(createOrganization().getId());
        RoleRepresentation realmRole = createRealmRole("invited-realm-role");
        RoleRepresentation clientRole = createClientRole("invitation-app", "invited-client-role");
        UserRepresentation user = createUser("existing@invitation.org");

        try (Response response = organization.members().inviteExistingUser(user.getId(), List.of(realmRole.getId(), clientRole.getId()))) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        OrganizationInvitationRepresentation invitation = organization.invitations().list().get(0);
        assertThat(roleIds(invitation), containsInAnyOrder(realmRole.getId(), clientRole.getId()));

        acceptInvitation();

        assertNotNull(organization.members().member(user.getId()).toRepresentation());
        assertThat(realmRoleNames(user.getId()), hasItem(realmRole.getName()));
        assertThat(clientRoleNames(user.getId(), "invitation-app"), contains(clientRole.getName()));
        assertThat(organization.invitations().list(), empty());
    }

    @Test
    public void testInviteNewUserGrantsRolesOnRegistration() throws IOException {
        OrganizationResource organization = realm.admin().organizations().get(createOrganization().getId());
        RoleRepresentation realmRole = createRealmRole("registered-realm-role");
        String email = "registered@invitation.org";

        try (Response response = organization.members().inviteUser(email, "Registered", "User", null, List.of(realmRole.getId()))) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        driver.open(getInvitationLink());
        registerPage.assertCurrent();
        registerPage.register("Registered", "User", email, "registered-invitee", "password");

        List<UserRepresentation> users = realm.admin().users().searchByEmail(email, true);
        assertThat(users, not(empty()));
        String userId = users.get(0).getId();
        realm.cleanup().add(r -> removeUser(r.users(), userId));

        assertNotNull(organization.members().member(userId).toRepresentation());
        assertThat(realmRoleNames(userId), hasItem(realmRole.getName()));
    }

    @Test
    public void testInviteWithUnknownRole() {
        OrganizationResource organization = realm.admin().organizations().get(createOrganization().getId());
        int sentEmails = mailServer.getReceivedMessages().length;

        try (Response response = organization.members().inviteUser("unknown-role@invitation.org", null, null, null, List.of(UUID.randomUUID().toString()))) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }

        assertThat(organization.invitations().list(), empty());
        assertEquals(sentEmails, mailServer.getReceivedMessages().length);
    }

    @Test
    public void testRoleRemovedBeforeAcceptanceIsSkipped() throws IOException {
        OrganizationResource organization = realm.admin().organizations().get(createOrganization().getId());
        RoleRepresentation kept = createRealmRole("kept-realm-role");
        RoleRepresentation removed = createRealmRole("removed-realm-role");
        UserRepresentation user = createUser("removed-role@invitation.org");

        try (Response response = organization.members().inviteExistingUser(user.getId(), List.of(kept.getId(), removed.getId()))) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        realm.admin().roles().deleteRole(removed.getName());

        assertThat(roleIds(organization.invitations().list().get(0)), contains(kept.getId()));

        acceptInvitation();

        assertNotNull(organization.members().member(user.getId()).toRepresentation());
        assertThat(realmRoleNames(user.getId()), hasItem(kept.getName()));
        assertThat(realmRoleNames(user.getId()), not(hasItem(removed.getName())));
    }

    @Test
    public void testResendKeepsRoles() throws IOException {
        OrganizationResource organization = realm.admin().organizations().get(createOrganization().getId());
        RoleRepresentation realmRole = createRealmRole("resent-realm-role");
        UserRepresentation user = createUser("resent@invitation.org");

        try (Response response = organization.members().inviteExistingUser(user.getId(), List.of(realmRole.getId()))) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        String invitationId = organization.invitations().list().get(0).getId();

        try (Response response = organization.invitations().resend(invitationId)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        OrganizationInvitationRepresentation resent = organization.invitations().list().get(0);
        assertThat(resent.getId(), not(invitationId));
        assertThat(roleIds(resent), contains(realmRole.getId()));

        acceptInvitation();

        assertThat(realmRoleNames(user.getId()), hasItem(realmRole.getName()));
    }

    private void acceptInvitation() throws IOException {
        driver.open(getInvitationLink());
        proceedPage.assertCurrent();
        proceedPage.clickProceedLink();
    }

    private String getInvitationLink() throws IOException {
        MimeMessage message = mailServer.getLastReceivedMessage();
        assertNotNull(message);
        return MailUtils.getLink(MailUtils.getBody(message).getHtml()).trim();
    }

    private RoleRepresentation createRealmRole(String name) {
        RoleRepresentation role = new RoleRepresentation();
        role.setName(name);
        realm.admin().roles().create(role);
        realm.cleanup().add(r -> {
            try {
                r.roles().deleteRole(name);
            } catch (NotFoundException ignored) {
            }
        });
        return realm.admin().roles().get(name).toRepresentation();
    }

    private RoleRepresentation createClientRole(String clientId, String name) {
        String clientUuid;

        try (Response response = realm.admin().clients().create(ClientBuilder.create(clientId).build())) {
            clientUuid = ApiUtil.getCreatedId(response);
        }

        realm.cleanup().add(r -> {
            try {
                r.clients().get(clientUuid).remove();
            } catch (NotFoundException ignored) {
            }
        });

        RoleRepresentation role = new RoleRepresentation();
        role.setName(name);
        realm.admin().clients().get(clientUuid).roles().create(role);
        return realm.admin().clients().get(clientUuid).roles().get(name).toRepresentation();
    }

    private UserRepresentation createUser(String email) {
        UserRepresentation user = UserBuilder.create()
                .username(email)
                .email(email)
                .name("Invited", "User")
                .emailVerified(true)
                .build();

        try (Response response = realm.admin().users().create(user)) {
            user.setId(ApiUtil.getCreatedId(response));
        }

        String userId = user.getId();
        realm.cleanup().add(r -> removeUser(r.users(), userId));
        return user;
    }

    private static void removeUser(UsersResource users, String userId) {
        try {
            users.get(userId).remove();
        } catch (NotFoundException ignored) {
        }
    }

    private List<String> roleIds(OrganizationInvitationRepresentation invitation) {
        return invitation.getRoles().stream().map(RoleRepresentation::getId).toList();
    }

    private List<String> realmRoleNames(String userId) {
        return realm.admin().users().get(userId).roles().realmLevel().listAll().stream().map(RoleRepresentation::getName).toList();
    }

    private List<String> clientRoleNames(String userId, String clientId) {
        String clientUuid = realm.admin().clients().findByClientId(clientId).get(0).getId();
        return realm.admin().users().get(userId).roles().clientLevel(clientUuid).listAll().stream().map(RoleRepresentation::getName).toList();
    }
}
