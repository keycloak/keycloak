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

package org.keycloak.tests.organization.admin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.representations.idm.OrganizationInvitationExistingUserRequest;
import org.keycloak.representations.idm.OrganizationInvitationRepresentation;
import org.keycloak.representations.idm.OrganizationInvitationUserRequest;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@KeycloakIntegrationTest
public class OrganizationInvitationAttributeTest extends AbstractOrganizationTest {

    @InjectMailServer
    MailServer mailServer;

    private OrganizationResource organization;

    @BeforeEach
    public void setUp() {
        OrganizationRepresentation orgRep = createOrganization("test-org", "test-org.com");
        organization = realm.admin().organizations().get(orgRep.getId());
    }

    @Test
    public void testCreateInvitationWithAttributes() {
        OrganizationInvitationUserRequest req = new OrganizationInvitationUserRequest();
        req.setEmail("attr-user@test-org.com");
        req.setFirstName("Attr");
        req.setLastName("User");
        req.setAttributes(Map.of(
                "role", List.of("admin"),
                "department", List.of("engineering")
        ));

        try (Response response = organization.members().inviteUser(req)) {
            assertThat(response.getStatus(), equalTo(204));
        }

        List<OrganizationInvitationRepresentation> invitations = organization.invitations().list();
        assertThat(invitations, hasSize(1));

        OrganizationInvitationRepresentation invitation = invitations.get(0);
        assertThat(invitation.getEmail(), equalTo("attr-user@test-org.com"));
        assertThat(invitation.getAttributes(), notNullValue());
        assertThat(invitation.getAttributes().get("role"), equalTo(List.of("admin")));
        assertThat(invitation.getAttributes().get("department"), equalTo(List.of("engineering")));
    }

    @Test
    public void testInvitationWithoutAttributesReturnsNullAttributes() {
        sendInvitation("no-attr@test-org.com", "No", "Attr");

        List<OrganizationInvitationRepresentation> invitations = organization.invitations().list();
        assertThat(invitations, hasSize(1));
        assertThat(invitations.get(0).getAttributes(), nullValue());
    }

    @Test
    public void testResendInvitationPreservesAttributes() {
        OrganizationInvitationUserRequest req = new OrganizationInvitationUserRequest();
        req.setEmail("resend-attr@test-org.com");
        req.setFirstName("Resend");
        req.setLastName("Attr");
        req.setAttributes(Map.of("role", List.of("viewer")));

        try (Response response = organization.members().inviteUser(req)) {
            assertThat(response.getStatus(), equalTo(204));
        }

        List<OrganizationInvitationRepresentation> invitations = organization.invitations().list();
        String invitationId = invitations.get(0).getId();

        try (Response response = organization.invitations().resend(invitationId)) {
            assertThat(response.getStatus(), equalTo(204));
        }

        invitations = organization.invitations().list();
        assertThat(invitations, hasSize(1));
        assertThat(invitations.get(0).getId(), not(equalTo(invitationId)));
        assertThat(invitations.get(0).getAttributes(), notNullValue());
        assertThat(invitations.get(0).getAttributes().get("role"), equalTo(List.of("viewer")));
    }

    @Test
    public void testInvitationAttributesMultiValued() {
        OrganizationInvitationUserRequest req = new OrganizationInvitationUserRequest();
        req.setEmail("multi-val@test-org.com");
        req.setFirstName("Multi");
        req.setLastName("Val");
        req.setAttributes(Map.of("tags", List.of("tag1", "tag2", "tag3")));

        try (Response response = organization.members().inviteUser(req)) {
            assertThat(response.getStatus(), equalTo(204));
        }

        OrganizationInvitationRepresentation invitation = organization.invitations().list().get(0);
        assertThat(invitation.getAttributes().get("tags"), hasSize(3));
    }

    @Test
    public void testInviteExistingUserWithAttributes() {
        UserRepresentation user = createRealmUser("existing-user", "existing-user@test-org.com");

        OrganizationInvitationExistingUserRequest req = new OrganizationInvitationExistingUserRequest();
        req.setId(user.getId());
        req.setAttributes(Map.of(
                "role", List.of("admin"),
                "department", List.of("engineering")
        ));

        try (Response response = organization.members().inviteExistingUser(req)) {
            assertThat(response.getStatus(), equalTo(204));
        }

        List<OrganizationInvitationRepresentation> invitations = organization.invitations().list();
        assertThat(invitations, hasSize(1));

        OrganizationInvitationRepresentation invitation = invitations.get(0);
        assertThat(invitation.getEmail(), equalTo("existing-user@test-org.com"));
        assertThat(invitation.getAttributes(), notNullValue());
        assertThat(invitation.getAttributes().get("role"), equalTo(List.of("admin")));
        assertThat(invitation.getAttributes().get("department"), equalTo(List.of("engineering")));
    }

    @Test
    public void testInviteExistingUserWithoutAttributes() {
        UserRepresentation user = createRealmUser("no-attr-user", "no-attr-user@test-org.com");

        try (Response response = organization.members().inviteExistingUser(user.getId())) {
            assertThat(response.getStatus(), equalTo(204));
        }

        List<OrganizationInvitationRepresentation> invitations = organization.invitations().list();
        assertThat(invitations, hasSize(1));
        assertThat(invitations.get(0).getEmail(), equalTo("no-attr-user@test-org.com"));
        assertThat(invitations.get(0).getAttributes(), nullValue());
    }

    private UserRepresentation createRealmUser(String username, String email) {
        UserRepresentation user = UserBuilder.create()
                .username(username)
                .email(email)
                .firstName(username)
                .lastName("Test")
                .password("password")
                .enabled(true)
                .build();
        try (Response response = realm.admin().users().create(user)) {
            user.setId(ApiUtil.getCreatedId(response));
        }
        String userId = user.getId();
        realm.cleanup().add(r -> r.users().get(userId).remove());
        return user;
    }

    @Test
    public void testDeleteInvitationCascadesAttributes() {
        OrganizationInvitationUserRequest req = new OrganizationInvitationUserRequest();
        req.setEmail("cascade-del@test-org.com");
        req.setFirstName("Cascade");
        req.setLastName("Del");
        req.setAttributes(Map.of("role", List.of("admin")));

        try (Response response = organization.members().inviteUser(req)) {
            assertThat(response.getStatus(), equalTo(204));
        }

        List<OrganizationInvitationRepresentation> invitations = organization.invitations().list();
        assertThat(invitations, hasSize(1));
        String invitationId = invitations.get(0).getId();

        try (Response response = organization.invitations().delete(invitationId)) {
            assertThat(response.getStatus(), equalTo(204));
        }

        assertThat(organization.invitations().list(), hasSize(0));
    }

    @Test
    public void testCreateInvitationWithEmptyAttributes() {
        OrganizationInvitationUserRequest req = new OrganizationInvitationUserRequest();
        req.setEmail("empty-attr@test-org.com");
        req.setFirstName("Empty");
        req.setLastName("Attr");
        req.setAttributes(Map.of());

        try (Response response = organization.members().inviteUser(req)) {
            assertThat(response.getStatus(), equalTo(204));
        }

        List<OrganizationInvitationRepresentation> invitations = organization.invitations().list();
        assertThat(invitations, hasSize(1));
        assertThat(invitations.get(0).getAttributes(), nullValue());
    }

    @Test
    public void testInviteUserWithEmptyJsonBodyReturnsBadRequest() {
        try (Response response = organization.members().inviteUser((OrganizationInvitationUserRequest) null)) {
            assertThat(response.getStatus(), equalTo(400));
        }
    }

    @Test
    public void testInviteExistingUserWithEmptyJsonBodyReturnsBadRequest() {
        try (Response response = organization.members().inviteExistingUser((OrganizationInvitationExistingUserRequest) null)) {
            assertThat(response.getStatus(), equalTo(400));
        }
    }

    @Test
    public void testCreateInvitationIgnoresAttributeWithNullValues() {
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("role", List.of("admin"));
        attributes.put("ignored", null);

        OrganizationInvitationUserRequest req = new OrganizationInvitationUserRequest();
        req.setEmail("null-list@test-org.com");
        req.setAttributes(attributes);

        try (Response response = organization.members().inviteUser(req)) {
            assertThat(response.getStatus(), equalTo(204));
        }

        assertThat(organization.invitations().list().get(0).getAttributes(), equalTo(Map.of("role", List.of("admin"))));
    }

    @Test
    public void testCreateInvitationIgnoresNullAttributeValue() {
        OrganizationInvitationUserRequest req = new OrganizationInvitationUserRequest();
        req.setEmail("null-value@test-org.com");
        req.setAttributes(Map.of("role", Arrays.asList("admin", null)));

        try (Response response = organization.members().inviteUser(req)) {
            assertThat(response.getStatus(), equalTo(204));
        }

        assertThat(organization.invitations().list().get(0).getAttributes(), equalTo(Map.of("role", List.of("admin"))));
    }

    @Test
    public void testCreateInvitationIgnoresBlankAttributeNames() {
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("role", List.of("admin"));
        attributes.put(" ", List.of("ignored"));

        OrganizationInvitationUserRequest req = new OrganizationInvitationUserRequest();
        req.setEmail("blank-name@test-org.com");
        req.setAttributes(attributes);

        try (Response response = organization.members().inviteUser(req)) {
            assertThat(response.getStatus(), equalTo(204));
        }

        assertThat(organization.invitations().list().get(0).getAttributes(), equalTo(Map.of("role", List.of("admin"))));
    }

    private void sendInvitation(String email, String firstName, String lastName) {
        try (Response response = organization.members().inviteUser(email, firstName, lastName)) {
            assertThat(response.getStatus(), equalTo(204));
        }
    }
}
