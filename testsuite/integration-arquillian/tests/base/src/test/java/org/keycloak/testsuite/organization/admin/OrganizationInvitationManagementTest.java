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

package org.keycloak.testsuite.organization.admin;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.representations.idm.OrganizationInvitationRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.util.MailServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
import static org.junit.Assert.fail;

/**
 * Integration tests for Organization Invitation Management functionality
 * 
 */
public class OrganizationInvitationManagementTest extends AbstractOrganizationTest {

    private OrganizationResource organization;
    private String organizationId;

    @Before
    public void setUp() {
        MailServer.start();
        OrganizationRepresentation orgRep = createOrganization("test-org", "test-org.com");
        organizationId = orgRep.getId();
        organization = testRealm().organizations().get(organizationId);
    }

    @After
    public void tearDown() {
        MailServer.stop();
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        Map<String, String> smtpConfig = testRealm.getSmtpServer();
        super.configureTestRealm(testRealm);
        testRealm.setRegistrationAllowed(true);
        testRealm.setSmtpServer(smtpConfig);
    }

    @Test
    public void testCreateAndListInvitations() {
        // Create an invitation
        sendInvitation("user1@test-org.com", "John", "Doe");
        sendInvitation("user2@test-org.com", "Jane", "Smith");

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
        assertThat(invitation1.getOrganizationId(), equalTo(organizationId));
        assertThat(invitation1.getSentDate(), notNullValue());
        assertThat(invitation1.getExpiresAt(), notNullValue());
    }

    @Test
    public void testGetInvitationById() {
        // Create invitation
        sendInvitation("user@test-org.com", "Test", "User");
        
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
        // Create invitation
        sendInvitation("user@test-org.com", "Test", "User");
        
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
        // Create invitation
        sendInvitation("user@test-org.com", "Test", "User");
        
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
        try (Response response = organization.invitations().delete("non-existent-id")) {
            assertThat(response.getStatus(), equalTo(404));
        }
    }

    @Test
    public void testInvitationPagination() {
        // Create multiple invitations
        for (int i = 1; i <= 15; i++) {
            sendInvitation("user" + i + "@test-org.com", "User", "Number" + i);
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
        // Create invitations with different statuses
        sendInvitation("pending@test-org.com", "Pending", "User");
        
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
            setTimeOffset(Math.toIntExact(Duration.ofDays(2).toSeconds()));
            invitations =
                    organization.invitations().list("EXPIRED", null, null, null);
            assertThat(invitations, hasSize(1));
        } finally {
            setTimeOffset(0);
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
        // Create invitations
        sendInvitation("john.doe@test-org.com", "John", "Doe");
        sendInvitation("jane.smith@test-org.com", "Jane", "Smith");
        sendInvitation("admin@test-org.com", "Admin", "User");
        
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
        // Create invitations with SQL wildcard characters in email
        sendInvitation("john_doe@test-org.com", "John", "Doe");
        sendInvitation("johnadoe@test-org.com", "Johna", "Doe");
        sendInvitation("johnbdoe@test-org.com", "Johnb", "Doe");

        // Search by email with underscore - should match literally
        List<OrganizationInvitationRepresentation> invitations =
            organization.invitations().list(null, "john_", null, null, null, null, null);
        assertThat(invitations, hasSize(1));
        assertThat(invitations.get(0).getEmail(), equalTo("john_doe@test-org.com"));

        // Create invitations with percent character
        sendInvitation("50%@test-org.com", "Fifty", "Percent");
        sendInvitation("500@test-org.com", "Five", "Hundred");
        sendInvitation("50abc@test-org.com", "Fiftyabc", "Test");

        // Search by email with percent - should match literally
        invitations = organization.invitations().list(null, "50%", null, null, null, null, null);
        assertThat(invitations, hasSize(1));
        assertThat(invitations.get(0).getEmail(), equalTo("50%@test-org.com"));

        // Test search by first name with SQL wildcards
        sendInvitation("test_fn@test-org.com", "Test_Name", "LastName");
        sendInvitation("testafn@test-org.com", "TestaName", "LastName");

        invitations = organization.invitations().list(null, null, "Test_", null, null, null, null);
        assertThat(invitations, hasSize(1));
        assertThat(invitations.get(0).getFirstName(), equalTo("Test_Name"));

        // Test search by last name with SQL wildcards
        sendInvitation("test_ln@test-org.com", "FirstName", "50%_Last");
        sendInvitation("test_ln2@test-org.com", "FirstName", "50a_Last");

        invitations = organization.invitations().list(null, null, null, null, "50%_", null, null);
        assertThat(invitations, hasSize(1));
        assertThat(invitations.get(0).getLastName(), equalTo("50%_Last"));
    }

    @Test
    public void testCrossOrganizationInvitationAccess() {
        // Create second organization
        OrganizationRepresentation org2Rep = createOrganization("test-org-2", "test-org-2.com");
        OrganizationResource organization2 = testRealm().organizations().get(org2Rep.getId());

        // Create invitation in org1
        sendInvitation("user@test-org.com", "User", "One");
        String org1InvitationId = organization.invitations().list().get(0).getId();

        // Create invitation in org2
        sendInvitationToOrganization(organization2, "user@test-org-2.com", "User", "Two");
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
        // Create second organization
        OrganizationRepresentation org2Rep = createOrganization("test-org-2", "test-org-2.com");
        OrganizationResource organization2 = testRealm().organizations().get(org2Rep.getId());

        // Create invitations in both organizations
        sendInvitation("user@test-org.com", "User", "One");
        sendInvitationToOrganization(organization2, "user@test-org-2.com", "User", "Two");
        
        // Verify isolation
        List<OrganizationInvitationRepresentation> org1Invitations = organization.invitations().list();
        List<OrganizationInvitationRepresentation> org2Invitations = organization2.invitations().list();
        
        assertThat(org1Invitations, hasSize(1));
        assertThat(org1Invitations.get(0).getEmail(), equalTo("user@test-org.com"));
        assertThat(org1Invitations.get(0).getOrganizationId(), equalTo(organizationId));
        
        assertThat(org2Invitations, hasSize(1));
        assertThat(org2Invitations.get(0).getEmail(), equalTo("user@test-org-2.com"));
        assertThat(org2Invitations.get(0).getOrganizationId(), equalTo(org2Rep.getId()));
    }

    private void sendInvitation(String email, String firstName, String lastName) {
        sendInvitationToOrganization(organization, email, firstName, lastName);
    }
    
    private void sendInvitationToOrganization(OrganizationResource org, String email, String firstName, String lastName) {
        try (Response response = org.members().inviteUser(email, firstName, lastName)) {
            assertThat(response.getStatus(), equalTo(204));
        }
    }
}
