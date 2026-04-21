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
 * Integration tests for Organization Invitation Management functionality
 * 
 */
@KeycloakIntegrationTest
public class OrganizationInvitationManagementTest extends AbstractOrganizationTest {

    @InjectMailServer
    MailServer mailServer;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @Test
    public void testCreateAndListInvitations() {
        OrganizationRepresentation orgRep = createOrganization("test-org", "test-org.com");
        OrganizationResource organization = realm.admin().organizations().get(orgRep.getId());
        
        // Create an invitation
        sendInvitation(organization, "user1@test-org.com", "John", "Doe");
        sendInvitation(organization, "user2@test-org.com", "Jane", "Smith");
        
        // List invitations
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
        assertThat(invitation1.getOrganizationId(), equalTo(orgRep.getId()));
        assertThat(invitation1.getSentDate(), notNullValue());
        assertThat(invitation1.getExpiresAt(), notNullValue());
    }

    @Test
    public void testGetInvitationById() {
        OrganizationResource organization = realm.admin().organizations()
                .get(createOrganization("test-org", "test-org.com").getId());
        
        // Create invitation
        sendInvitation(organization, "user@test-org.com", "Test", "User");
        
        // Get invitations list
        List<OrganizationInvitationRepresentation> invitations = organization.invitations().list();
        assertThat(invitations, hasSize(1));
        
        String invitationId = invitations.get(0).getId();
        
        // Get invitation by ID
        OrganizationInvitationRepresentation invitation = organization.invitations().get(invitationId);
        
        assertThat(invitation, notNullValue());
        assertThat(invitation.getId(), equalTo(invitationId));
        assertThat(invitation.getEmail(), equalTo("user@test-org.com"));
        assertThat(invitation.getStatus(), equalTo(PENDING));
    }

    @Test
    public void testGetNonExistentInvitation() {
        OrganizationResource organization = realm.admin().organizations()
                .get(createOrganization("test-org", "test-org.com").getId());
        
        // Try to get non-existent invitation - should throw an exception or return null
        try {
            OrganizationInvitationRepresentation invitation = organization.invitations().get("non-existent-id");
            // If we get here, the invitation should be null or we expect a 404
            assertThat(invitation, nullValue());
        } catch (Exception e) {
            // Expected - 404 or similar error
            assertThat(e.getMessage(), containsString("404"));
        }
    }

    @Test
    public void testResendInvitation() {
        OrganizationResource organization = realm.admin().organizations()
                .get(createOrganization("test-org", "test-org.com").getId());
        
        // Create invitation
        sendInvitation(organization, "user@test-org.com", "Test", "User");
        
        List<OrganizationInvitationRepresentation> invitations = organization.invitations().list();
        String invitationId = invitations.get(0).getId();
        
        // Resend invitation
        try (Response response = organization.invitations().resend(invitationId)) {
            assertThat(response.getStatus(), equalTo(204));
        }
        
        // Verify invitation is still pending
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
        OrganizationResource organization = realm.admin().organizations()
                .get(createOrganization("test-org", "test-org.com").getId());
        
        // Create invitation
        sendInvitation(organization, "user@test-org.com", "Test", "User");
        
        List<OrganizationInvitationRepresentation> invitations = organization.invitations().list();
        String invitationId = invitations.get(0).getId();
        
        // Delete invitation
        try (Response response = organization.invitations().delete(invitationId)) {
            assertThat(response.getStatus(), equalTo(204));
        }
        
        // Verify invitation is deleted
        try {
            OrganizationInvitationRepresentation invitation = organization.invitations().get(invitationId);
            assertThat(invitation, nullValue());
        } catch (Exception e) {
            // Expected - invitation should not be found
            assertThat(e.getMessage(), containsString("404"));
        }
        
        // Verify it's not in the list
        List<OrganizationInvitationRepresentation> updatedInvitations = organization.invitations().list();
        assertThat(updatedInvitations, empty());
    }

    @Test
    public void testDeleteNonExistentInvitation() {
        OrganizationResource organization = realm.admin().organizations()
                .get(createOrganization("test-org", "test-org.com").getId());
        
        try (Response response = organization.invitations().delete("non-existent-id")) {
            assertThat(response.getStatus(), equalTo(404));
        }
    }

    @Test
    public void testInvitationPagination() {
        OrganizationResource organization = realm.admin().organizations()
                .get(createOrganization("test-org", "test-org.com").getId());
        
        // Create multiple invitations
        for (int i = 1; i <= 15; i++) {
            sendInvitation(organization, "user" + i + "@test-org.com", "User", "Number" + i);
        }
        
        // Test pagination
        List<OrganizationInvitationRepresentation> firstPage =
            organization.invitations().list(0, 10);
        List<OrganizationInvitationRepresentation> secondPage =
            organization.invitations().list(10, 10);
        
        assertThat(firstPage, hasSize(10));
        assertThat(secondPage, hasSize(5));
        
        // Verify no duplicates between pages
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
        OrganizationResource organization = realm.admin().organizations()
                .get(createOrganization("test-org", "test-org.com").getId());
        
        // Create invitations with different statuses
        sendInvitation(organization, "pending@test-org.com", "Pending", "User");
        
        // Filter by status - pending
        List<OrganizationInvitationRepresentation> invitations =
            organization.invitations().list("PENDING", null, null, null);
        assertThat(invitations, hasSize(1));
        assertThat(invitations.get(0).getStatus(), equalTo(PENDING));
        assertThat(invitations.get(0).getEmail(), equalTo("pending@test-org.com"));
        
        // Filter by status - expired
        invitations =
            organization.invitations().list("EXPIRED", null, null, null);
        assertThat(invitations, empty());
        
        try {
            timeOffSet.set(Duration.ofDays(2));
            invitations =
                    organization.invitations().list("EXPIRED", null, null, null);
            assertThat(invitations, hasSize(1));
        } finally {
            timeOffSet.set(0);
        }
        
        invitations =
                organization.invitations().list(null, null, "test", null, null, null, null);
        assertThat(invitations, hasSize(1));
        
        invitations =
                organization.invitations().list(null, null, "none", null, null, null, null);
        assertThat(invitations, hasSize(0));
    }

    @Test
    public void testInvitationEmailSearch() {
        OrganizationResource organization = realm.admin().organizations()
                .get(createOrganization("test-org", "test-org.com").getId());
        
        // Create invitations
        sendInvitation(organization, "john.doe@test-org.com", "John", "Doe");
        sendInvitation(organization, "jane.smith@test-org.com", "Jane", "Smith");
        sendInvitation(organization, "admin@test-org.com", "Admin", "User");
        
        // Search by email
        List<OrganizationInvitationRepresentation> johnInvitations =
            organization.invitations().list(null, "john.doe", null, null, null, null, null);
        
        assertThat(johnInvitations, hasSize(1));
        assertThat(johnInvitations.get(0).getEmail(), equalTo("john.doe@test-org.com"));
        
        // Search by partial email
        List<OrganizationInvitationRepresentation> adminInvitations =
            organization.invitations().list(null, "admin", null, null, null, null, null);
        
        assertThat(adminInvitations, hasSize(1));
        assertThat(adminInvitations.get(0).getEmail(), equalTo("admin@test-org.com"));
    }

    @Test
    public void testInvitationSearchWithSqlWildcards() {
        OrganizationResource organization = realm.admin().organizations()
                .get(createOrganization("test-org", "test-org.com").getId());
        
        // Create invitations with SQL wildcard characters in email
        sendInvitation(organization, "john_doe@test-org.com", "John", "Doe");
        sendInvitation(organization, "johnadoe@test-org.com", "Johna", "Doe");
        sendInvitation(organization, "johnbdoe@test-org.com", "Johnb", "Doe");
        
        // Search by email with underscore - should match literally
        List<OrganizationInvitationRepresentation> invitations =
            organization.invitations().list(null, "john_", null, null, null, null, null);
        assertThat(invitations, hasSize(1));
        assertThat(invitations.get(0).getEmail(), equalTo("john_doe@test-org.com"));
        
        // Create invitations with percent character
        sendInvitation(organization, "50%@test-org.com", "Fifty", "Percent");
        sendInvitation(organization, "500@test-org.com", "Five", "Hundred");
        sendInvitation(organization, "50abc@test-org.com", "Fiftyabc", "Test");
        
        // Search by email with percent - should match literally
        invitations = organization.invitations().list(null, "50%", null, null, null, null, null);
        assertThat(invitations, hasSize(1));
        assertThat(invitations.get(0).getEmail(), equalTo("50%@test-org.com"));
        
        // Test search by first name with SQL wildcards
        sendInvitation(organization, "test_fn@test-org.com", "Test_Name", "LastName");
        sendInvitation(organization, "testafn@test-org.com", "TestaName", "LastName");
        
        invitations = organization.invitations().list(null, null, "Test_", null, null, null, null);
        assertThat(invitations, hasSize(1));
        assertThat(invitations.get(0).getFirstName(), equalTo("Test_Name"));
        
        // Test search by last name with SQL wildcards
        sendInvitation(organization, "test_ln@test-org.com", "FirstName", "50%_Last");
        sendInvitation(organization, "test_ln2@test-org.com", "FirstName", "50a_Last");
        
        invitations = organization.invitations().list(null, null, null, null, "50%_", null, null);
        assertThat(invitations, hasSize(1));
        assertThat(invitations.get(0).getLastName(), equalTo("50%_Last"));
    }

    @Test
    public void testCrossOrganizationInvitationAccess() {
        OrganizationResource organization = realm.admin().organizations()
                .get(createOrganization("test-org", "test-org.com").getId());
        
        // Create second organization
        OrganizationRepresentation org2Rep = createOrganization("test-org-2", "test-org-2.com");
        OrganizationResource organization2 = realm.admin().organizations().get(org2Rep.getId());
        
        // Create invitation in org1
        sendInvitation(organization, "user@test-org.com", "User", "One");
        String org1InvitationId = organization.invitations().list().get(0).getId();
        
        // Create invitation in org2
        sendInvitation(organization2, "user@test-org-2.com", "User", "Two");
        String org2InvitationId = organization2.invitations().list().get(0).getId();
        
        // Try to get org1's invitation via org2 - should return 404
        try {
            organization2.invitations().get(org1InvitationId);
            fail("Should not be able to get invitation from another organization");
        } catch (NotFoundException expected) {
        }
        
        // Try to delete org1's invitation via org2 - should return 404
        try (Response response = organization2.invitations().delete(org1InvitationId)) {
            assertThat(response.getStatus(), equalTo(404));
        }
        
        // Try to resend org1's invitation via org2 - should return 404
        try (Response response = organization2.invitations().resend(org1InvitationId)) {
            assertThat(response.getStatus(), equalTo(404));
        }
        
        // Verify the invitations are still intact in their respective orgs
        assertThat(organization.invitations().list(), hasSize(1));
        assertThat(organization2.invitations().list(), hasSize(1));
        
        // Verify accessing own invitations still works
        assertThat(organization.invitations().get(org1InvitationId).getEmail(), equalTo("user@test-org.com"));
        assertThat(organization2.invitations().get(org2InvitationId).getEmail(), equalTo("user@test-org-2.com"));
    }

    @Test
    public void testMultipleOrganizationInvitationIsolation() {
        OrganizationRepresentation orgRep = createOrganization("test-org", "test-org.com");
        OrganizationResource organization = realm.admin().organizations().get(orgRep.getId());
        
        // Create second organization
        OrganizationRepresentation org2Rep = createOrganization("test-org-2", "test-org-2.com");
        OrganizationResource organization2 = realm.admin().organizations().get(org2Rep.getId());
        
        // Create invitations in both organizations
        sendInvitation(organization, "user@test-org.com", "User", "One");
        sendInvitation(organization2, "user@test-org-2.com", "User", "Two");
        
        // Verify isolation
        List<OrganizationInvitationRepresentation> org1Invitations = organization.invitations().list();
        List<OrganizationInvitationRepresentation> org2Invitations = organization2.invitations().list();
        
        assertThat(org1Invitations, hasSize(1));
        assertThat(org1Invitations.get(0).getEmail(), equalTo("user@test-org.com"));
        assertThat(org1Invitations.get(0).getOrganizationId(), equalTo(orgRep.getId()));
        
        assertThat(org2Invitations, hasSize(1));
        assertThat(org2Invitations.get(0).getEmail(), equalTo("user@test-org-2.com"));
        assertThat(org2Invitations.get(0).getOrganizationId(), equalTo(org2Rep.getId()));
    }

    @Test
    public void testSendInvitationToDisabledOrganization() {
        OrganizationResource organization = realm.admin().organizations()
                .get(createOrganization("test-org", "test-org.com").getId());
        
        OrganizationRepresentation rep = organization.toRepresentation();
        rep.setEnabled(false);
        organization.update(rep);
        
        try (Response response = organization.members().inviteUser("user@test-org.com", "John", "Doe")) {
            assertThat(response.getStatus(), equalTo(400));
            assertThat(response.readEntity(String.class), containsString("Organization is disabled"));
        }
    }

    @Test
    public void testResendInvitationToDisabledOrganization() {
        OrganizationResource organization = realm.admin().organizations()
                .get(createOrganization("test-org", "test-org.com").getId());
        
        sendInvitation(organization, "user@test-org.com", "John", "Doe");
        
        List<OrganizationInvitationRepresentation> invitations = organization.invitations().list();
        assertThat(invitations, hasSize(1));
        String invitationId = invitations.get(0).getId();
        
        OrganizationRepresentation rep = organization.toRepresentation();
        rep.setEnabled(false);
        organization.update(rep);
        
        try (Response response = organization.invitations().resend(invitationId)) {
            assertThat(response.getStatus(), equalTo(400));
            assertThat(response.readEntity(String.class), containsString("Organization is disabled"));
        }
    }

    @Test
    public void testInvitationWorksAfterReEnablingOrganization() {
        OrganizationResource organization = realm.admin().organizations()
                .get(createOrganization("test-org", "test-org.com").getId());
        
        OrganizationRepresentation rep = organization.toRepresentation();
        rep.setEnabled(false);
        organization.update(rep);
        
        try (Response response = organization.members().inviteUser("user@test-org.com", "John", "Doe")) {
            assertThat(response.getStatus(), equalTo(400));
        }
        
        rep.setEnabled(true);
        organization.update(rep);
        
        // After re-enabling, invitation should work
        sendInvitation(organization, "user@test-org.com", "John", "Doe");
        List<OrganizationInvitationRepresentation> invitations = organization.invitations().list();
        assertThat(invitations, hasSize(1));
        assertThat(invitations.get(0).getEmail(), equalTo("user@test-org.com"));
    }

    private void sendInvitation(OrganizationResource org, String email, String firstName, String lastName) {
        try (Response response = org.members().inviteUser(email, firstName, lastName)) {
            assertThat(response.getStatus(), equalTo(204));
        }
    }
}
