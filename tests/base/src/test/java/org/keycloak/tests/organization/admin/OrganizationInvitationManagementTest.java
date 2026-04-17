/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

import java.time.Duration;
import java.util.List;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.representations.idm.OrganizationInvitationRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.representations.idm.OrganizationInvitationRepresentation.Status.PENDING;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Integration tests for Organization Invitation Management functionality.
 */
@KeycloakIntegrationTest
public class OrganizationInvitationManagementTest extends AbstractOrganizationTest {

    @InjectMailServer
    MailServer mailServer;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    private OrganizationResource organization;
    private String organizationId;

    @BeforeEach
    public void setUp() {
        OrganizationRepresentation orgRep = createOrganization("test-org", "test-org.com");
        organizationId = orgRep.getId();
        organization = realm.admin().organizations().get(organizationId);
    }

    @Test
    public void testCreateAndListInvitations() {
        sendInvitation("user1@test-org.com", "John", "Doe");
        sendInvitation("user2@test-org.com", "Jane", "Smith");

        List<OrganizationInvitationRepresentation> invitations = organization.invitations().list();

        assertThat(invitations, hasSize(2));

        OrganizationInvitationRepresentation invitation1 = invitations.stream()
                .filter(inv -> "user1@test-org.com".equals(inv.getEmail()))
                .findFirst()
                .orElse(null);

        assertThat(invitation1, notNullValue());
        assertThat(invitation1.getEmail(), equalTo("user1@test-org.com"));
        assertThat(invitation1.getFirstName(), equalTo("John"));
        assertThat(invitation1.getLastName(), equalTo("Doe"));
        assertThat(invitation1.getStatus(), equalTo(PENDING));
        assertThat(invitation1.getOrganizationId(), equalTo(organizationId));
        assertThat(invitation1.getSentDate(), notNullValue());
        assertThat(invitation1.getExpiresAt(), notNullValue());
    }

    @Test
    public void testGetInvitationById() {
        sendInvitation("user@test-org.com", "Test", "User");

        List<OrganizationInvitationRepresentation> invitations = organization.invitations().list();
        assertThat(invitations, hasSize(1));

        String invitationId = invitations.get(0).getId();
        OrganizationInvitationRepresentation invitation = organization.invitations().get(invitationId);

        assertThat(invitation, notNullValue());
        assertThat(invitation.getId(), equalTo(invitationId));
        assertThat(invitation.getEmail(), equalTo("user@test-org.com"));
        assertThat(invitation.getStatus(), equalTo(PENDING));
    }

    @Test
    public void testGetNonExistentInvitation() {
        try {
            OrganizationInvitationRepresentation invitation = organization.invitations().get("non-existent-id");
            assertThat(invitation, nullValue());
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("404"));
        }
    }

    @Test
    public void testResendInvitation() {
        sendInvitation("user@test-org.com", "Test", "User");

        List<OrganizationInvitationRepresentation> invitations = organization.invitations().list();
        String invitationId = invitations.get(0).getId();

        try (Response response = organization.invitations().resend(invitationId)) {
            assertThat(response.getStatus(), equalTo(204));
        }

        try {
            organization.invitations().get(invitationId);
            fail("Expected NotFoundException");
        } catch (NotFoundException expected) {
        }

        invitations = organization.invitations().list();
        assertThat(invitations, hasSize(1));
        assertThat(invitations.get(0).getId(), not(equalTo(invitationId)));
    }

    @Test
    public void testDeleteInvitation() {
        sendInvitation("user@test-org.com", "Test", "User");

        List<OrganizationInvitationRepresentation> invitations = organization.invitations().list();
        String invitationId = invitations.get(0).getId();

        try (Response response = organization.invitations().delete(invitationId)) {
            assertThat(response.getStatus(), equalTo(204));
        }

        try {
            OrganizationInvitationRepresentation invitation = organization.invitations().get(invitationId);
            assertThat(invitation, nullValue());
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("404"));
        }

        List<OrganizationInvitationRepresentation> updatedInvitations = organization.invitations().list();
        assertThat(updatedInvitations, empty());
    }

    @Test
    public void testDeleteNonExistentInvitation() {
        try (Response response = organization.invitations().delete("non-existent-id")) {
            assertThat(response.getStatus(), equalTo(404));
        }
    }

    @Test
    public void testInvitationPagination() {
        for (int i = 1; i <= 15; i++) {
            sendInvitation("user" + i + "@test-org.com", "User", "Number" + i);
        }

        List<OrganizationInvitationRepresentation> firstPage = organization.invitations().list(0, 10);
        List<OrganizationInvitationRepresentation> secondPage = organization.invitations().list(10, 10);

        assertThat(firstPage, hasSize(10));
        assertThat(secondPage, hasSize(5));

        List<String> firstPageIds = firstPage.stream()
                .map(OrganizationInvitationRepresentation::getId)
                .toList();
        List<String> secondPageIds = secondPage.stream()
                .map(OrganizationInvitationRepresentation::getId)
                .toList();

        assertThat(firstPageIds.stream().noneMatch(secondPageIds::contains), is(true));
    }

    @Test
    public void testInvitationFiltering() {
        sendInvitation("pending@test-org.com", "Pending", "User");

        List<OrganizationInvitationRepresentation> invitations =
                organization.invitations().list("PENDING", null, null, null);
        assertThat(invitations, hasSize(1));
        assertThat(invitations.get(0).getStatus(), equalTo(PENDING));
        assertThat(invitations.get(0).getEmail(), equalTo("pending@test-org.com"));

        invitations = organization.invitations().list("EXPIRED", null, null, null);
        assertThat(invitations, empty());

        try {
            timeOffSet.set(Duration.ofDays(2));
            invitations = organization.invitations().list("EXPIRED", null, null, null);
            assertThat(invitations, hasSize(1));
        } finally {
            timeOffSet.set(0);
        }

        invitations = organization.invitations().list(null, null, "test", null, null, null, null);
        assertThat(invitations, hasSize(1));

        invitations = organization.invitations().list(null, null, "none", null, null, null, null);
        assertThat(invitations, hasSize(0));
    }

    @Test
    public void testInvitationEmailSearch() {
        sendInvitation("john.doe@test-org.com", "John", "Doe");
        sendInvitation("jane.smith@test-org.com", "Jane", "Smith");
        sendInvitation("admin@test-org.com", "Admin", "User");

        List<OrganizationInvitationRepresentation> johnInvitations =
                organization.invitations().list(null, "john.doe", null, null, null, null, null);

        assertThat(johnInvitations, hasSize(1));
        assertThat(johnInvitations.get(0).getEmail(), equalTo("john.doe@test-org.com"));

        List<OrganizationInvitationRepresentation> adminInvitations =
                organization.invitations().list(null, "admin", null, null, null, null, null);

        assertThat(adminInvitations, hasSize(1));
        assertThat(adminInvitations.get(0).getEmail(), equalTo("admin@test-org.com"));
    }

    @Test
    public void testInvitationSearchWithSqlWildcards() {
        sendInvitation("john_doe@test-org.com", "John", "Doe");
        sendInvitation("johnadoe@test-org.com", "Johna", "Doe");
        sendInvitation("johnbdoe@test-org.com", "Johnb", "Doe");

        List<OrganizationInvitationRepresentation> invitations =
                organization.invitations().list(null, "john_", null, null, null, null, null);
        assertThat(invitations, hasSize(1));
        assertThat(invitations.get(0).getEmail(), equalTo("john_doe@test-org.com"));

        sendInvitation("50%@test-org.com", "Fifty", "Percent");
        sendInvitation("500@test-org.com", "Five", "Hundred");
        sendInvitation("50abc@test-org.com", "Fiftyabc", "Test");

        invitations = organization.invitations().list(null, "50%", null, null, null, null, null);
        assertThat(invitations, hasSize(1));
        assertThat(invitations.get(0).getEmail(), equalTo("50%@test-org.com"));

        sendInvitation("test_fn@test-org.com", "Test_Name", "LastName");
        sendInvitation("testafn@test-org.com", "TestaName", "LastName");

        invitations = organization.invitations().list(null, null, "Test_", null, null, null, null);
        assertThat(invitations, hasSize(1));
        assertThat(invitations.get(0).getFirstName(), equalTo("Test_Name"));

        sendInvitation("test_ln@test-org.com", "FirstName", "50%_Last");
        sendInvitation("test_ln2@test-org.com", "FirstName", "50a_Last");

        invitations = organization.invitations().list(null, null, null, null, "50%_", null, null);
        assertThat(invitations, hasSize(1));
        assertThat(invitations.get(0).getLastName(), equalTo("50%_Last"));
    }

    @Test
    public void testCrossOrganizationInvitationAccess() {
        OrganizationRepresentation org2Rep = createOrganization("test-org-2", "test-org-2.com");
        OrganizationResource organization2 = realm.admin().organizations().get(org2Rep.getId());

        sendInvitation("user@test-org.com", "User", "One");
        String org1InvitationId = organization.invitations().list().get(0).getId();

        sendInvitationToOrganization(organization2, "user@test-org-2.com", "User", "Two");
        String org2InvitationId = organization2.invitations().list().get(0).getId();

        try {
            organization2.invitations().get(org1InvitationId);
            fail("Should not be able to get invitation from another organization");
        } catch (NotFoundException expected) {
        }

        try (Response response = organization2.invitations().delete(org1InvitationId)) {
            assertThat(response.getStatus(), equalTo(404));
        }

        try (Response response = organization2.invitations().resend(org1InvitationId)) {
            assertThat(response.getStatus(), equalTo(404));
        }

        assertThat(organization.invitations().list(), hasSize(1));
        assertThat(organization2.invitations().list(), hasSize(1));

        assertThat(organization.invitations().get(org1InvitationId).getEmail(), equalTo("user@test-org.com"));
        assertThat(organization2.invitations().get(org2InvitationId).getEmail(), equalTo("user@test-org-2.com"));
    }

    @Test
    public void testMultipleOrganizationInvitationIsolation() {
        OrganizationRepresentation org2Rep = createOrganization("test-org-2", "test-org-2.com");
        OrganizationResource organization2 = realm.admin().organizations().get(org2Rep.getId());

        sendInvitation("user@test-org.com", "User", "One");
        sendInvitationToOrganization(organization2, "user@test-org-2.com", "User", "Two");

        List<OrganizationInvitationRepresentation> org1Invitations = organization.invitations().list();
        List<OrganizationInvitationRepresentation> org2Invitations = organization2.invitations().list();

        assertThat(org1Invitations, hasSize(1));
        assertThat(org1Invitations.get(0).getEmail(), equalTo("user@test-org.com"));
        assertThat(org1Invitations.get(0).getOrganizationId(), equalTo(organizationId));

        assertThat(org2Invitations, hasSize(1));
        assertThat(org2Invitations.get(0).getEmail(), equalTo("user@test-org-2.com"));
        assertThat(org2Invitations.get(0).getOrganizationId(), equalTo(org2Rep.getId()));
    }

    @Test
    public void testSendInvitationToDisabledOrganization() {
        Runnable restore = setOrganizationEnabled(organization, false);
        try {
            try (Response response = organization.members().inviteUser("user@test-org.com", "John", "Doe")) {
                assertThat(response.getStatus(), equalTo(400));
                assertThat(response.readEntity(String.class), containsString("Organization is disabled"));
            }
        } finally {
            restore.run();
        }
    }

    @Test
    public void testResendInvitationToDisabledOrganization() {
        sendInvitation("user@test-org.com", "John", "Doe");

        List<OrganizationInvitationRepresentation> invitations = organization.invitations().list();
        assertThat(invitations, hasSize(1));
        String invitationId = invitations.get(0).getId();

        Runnable restore = setOrganizationEnabled(organization, false);
        try {
            try (Response response = organization.invitations().resend(invitationId)) {
                assertThat(response.getStatus(), equalTo(400));
                assertThat(response.readEntity(String.class), containsString("Organization is disabled"));
            }
        } finally {
            restore.run();
        }
    }

    @Test
    public void testInvitationWorksAfterReEnablingOrganization() {
        Runnable restore = setOrganizationEnabled(organization, false);
        try {
            try (Response response = organization.members().inviteUser("user@test-org.com", "John", "Doe")) {
                assertThat(response.getStatus(), equalTo(400));
            }
        } finally {
            restore.run();
        }

        sendInvitation("user@test-org.com", "John", "Doe");
        List<OrganizationInvitationRepresentation> invitations = organization.invitations().list();
        assertThat(invitations, hasSize(1));
        assertThat(invitations.get(0).getEmail(), equalTo("user@test-org.com"));
    }

    private void sendInvitation(String email, String firstName, String lastName) {
        sendInvitationToOrganization(organization, email, firstName, lastName);
    }

    private void sendInvitationToOrganization(OrganizationResource org, String email, String firstName, String lastName) {
        try (Response response = org.members().inviteUser(email, firstName, lastName)) {
            assertThat(response.getStatus(), equalTo(204));
        }
    }

    private static Runnable setOrganizationEnabled(OrganizationResource org, boolean enabled) {
        OrganizationRepresentation rep = org.toRepresentation();
        boolean original = rep.isEnabled();
        rep.setEnabled(enabled);
        org.update(rep);
        return () -> {
            OrganizationRepresentation current = org.toRepresentation();
            current.setEnabled(original);
            org.update(current);
        };
    }
}
