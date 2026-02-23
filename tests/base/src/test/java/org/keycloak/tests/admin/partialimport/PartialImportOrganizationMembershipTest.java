package org.keycloak.tests.admin.partialimport;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.partialimport.PartialImportResult;
import org.keycloak.partialimport.PartialImportResults;
import org.keycloak.partialimport.ResourceType;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.MembershipType;
import org.keycloak.representations.idm.OrganizationMembershipRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = PartialImportOrganizationMembershipTest.OrganizationServerConfig.class)
public class PartialImportOrganizationMembershipTest extends AbstractPartialImportTest {

    private static final String ORG_PREFIX = "org";
    private static final int NUM_ORGS = 3;

    private void enableOrganizations() {
        managedRealm.updateWithCleanup(r -> r.organizationsEnabled(true));
    }

    @Test
    public void testAddOrganizationMemberships() {
        enableOrganizations();
        setFail();

        // First, create organizations
        List<String> orgIds = new ArrayList<>();
        for (int i = 0; i < NUM_ORGS; i++) {
            OrganizationRepresentation orgRep = new OrganizationRepresentation();
            orgRep.setName(ORG_PREFIX + i);
            orgRep.setEnabled(true);

            try (var response = managedRealm.admin().organizations().create(orgRep)) {
                String orgId = ApiUtil.getCreatedId(response);
                orgIds.add(orgId);
            }
        }

        // Add users
        addUsers();

        // Add organization memberships
        List<OrganizationMembershipRepresentation> memberships = new ArrayList<>();
        for (int i = 0; i < NUM_ENTITIES; i++) {
            OrganizationMembershipRepresentation membership = new OrganizationMembershipRepresentation();
            membership.setOrganizationId(orgIds.get(i % NUM_ORGS));
            membership.setUsername(USER_PREFIX + i);
            membership.setMembershipType(i % 2 == 0 ? MembershipType.MANAGED : MembershipType.UNMANAGED);
            memberships.add(membership);
        }

        piRep.setOrganizationMemberships(memberships);

        PartialImportResults results = doImport();

        // Users should be added
        assertTrue(results.getAdded() > 0);

        // Verify organization memberships were created
        int membershipCount = 0;
        for (PartialImportResult result : results.getResults()) {
            if (result.getResourceType() == ResourceType.ORGANIZATION_MEMBERSHIP) {
                membershipCount++;
                assertNotNull(result.getId());
                assertNotNull(result.getResourceName());
            }
        }

        assertEquals(NUM_ENTITIES, membershipCount, "All organization memberships should be created");

        // Verify the memberships exist via REST API
        for (int i = 0; i < NUM_ENTITIES; i++) {
            String username = USER_PREFIX + i;
            String orgId = orgIds.get(i % NUM_ORGS);

            // Get members of the organization and verify user is in the list
            List<MemberRepresentation> members = managedRealm.admin()
                    .organizations()
                    .get(orgId)
                    .members()
                    .getAll();

            boolean found = members.stream()
                    .anyMatch(m -> username.equals(m.getUsername()));

            assertTrue(found, "User " + username + " should be member of organization " + orgId);
        }
    }

    @Test
    public void testSkipExistingOrganizationMemberships() {
        enableOrganizations();

        setFail();

        // Create organization
        OrganizationRepresentation orgRep = new OrganizationRepresentation();
        orgRep.setName("test-org");
        orgRep.setEnabled(true);

        String orgId;
        try (var response = managedRealm.admin().organizations().create(orgRep)) {
            orgId = ApiUtil.getCreatedId(response);
        }

        // Add user
        addUsers();

        // Add first organization membership
        List<OrganizationMembershipRepresentation> memberships = new ArrayList<>();
        OrganizationMembershipRepresentation membership = new OrganizationMembershipRepresentation();
        membership.setOrganizationId(orgId);
        membership.setUsername(USER_PREFIX + "0");
        membership.setMembershipType(MembershipType.UNMANAGED);
        memberships.add(membership);

        piRep.setOrganizationMemberships(memberships);

        // First import should succeed
        PartialImportResults results = doImport();
        assertTrue(results.getAdded() > 0);

        // Set policy to SKIP
        setSkip();

        // Try to import the same membership again - should skip
        results = doImport();
        assertEquals(NUM_ENTITIES + 1, results.getSkipped(), "Users and membership should be skipped");
    }

    @Test
    public void testOverwriteOrganizationMemberships() {
        enableOrganizations();

        setFail();

        // Create organization
        OrganizationRepresentation orgRep = new OrganizationRepresentation();
        orgRep.setName("test-org");
        orgRep.setEnabled(true);

        String orgId;
        try (var response = managedRealm.admin().organizations().create(orgRep)) {
            orgId = ApiUtil.getCreatedId(response);
        }

        // Add user
        addUsers();

        // Add organization membership with UNMANAGED type
        List<OrganizationMembershipRepresentation> memberships = new ArrayList<>();
        OrganizationMembershipRepresentation membership = new OrganizationMembershipRepresentation();
        membership.setOrganizationId(orgId);
        membership.setUsername(USER_PREFIX + "0");
        membership.setMembershipType(MembershipType.UNMANAGED);
        memberships.add(membership);

        piRep.setOrganizationMemberships(memberships);

        // First import
        PartialImportResults results = doImport();
        assertTrue(results.getAdded() > 0);

        // Change to MANAGED and set OVERWRITE policy
        setOverwrite();
        membership.setMembershipType(MembershipType.MANAGED);

        // Import again - should overwrite
        results = doImport();
        assertEquals(NUM_ENTITIES + 1, results.getOverwritten(), "Users and membership should be overwritten");

        // Verify membership still exists after overwrite
        List<MemberRepresentation> members = managedRealm.admin()
                .organizations()
                .get(orgId)
                .members()
                .getAll();

        MemberRepresentation member = members.stream()
                .filter(m -> (USER_PREFIX + "0").equals(m.getUsername()))
                .findFirst()
                .orElse(null);

        assertNotNull(member);
        assertEquals(MembershipType.MANAGED, member.getMembershipType(), "Membership type should be updated to MANAGED");
    }

    @Test
    public void testSameUserInMultipleOrganizations() {
        enableOrganizations();
        setFail();

        // Create two organizations
        List<String> orgIds = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            OrganizationRepresentation orgRep = new OrganizationRepresentation();
            orgRep.setName("multi-org-" + i);
            orgRep.setEnabled(true);

            try (var response = managedRealm.admin().organizations().create(orgRep)) {
                orgIds.add(ApiUtil.getCreatedId(response));
            }
        }

        // Add users and assign the SAME user to both organizations
        addUsers();

        String username = USER_PREFIX + "0";
        List<OrganizationMembershipRepresentation> memberships = new ArrayList<>();

        OrganizationMembershipRepresentation first = new OrganizationMembershipRepresentation();
        first.setOrganizationId(orgIds.get(0));
        first.setUsername(username);
        first.setMembershipType(MembershipType.UNMANAGED);
        memberships.add(first);

        OrganizationMembershipRepresentation second = new OrganizationMembershipRepresentation();
        second.setOrganizationId(orgIds.get(1));
        second.setUsername(username);
        second.setMembershipType(MembershipType.UNMANAGED);
        memberships.add(second);

        piRep.setOrganizationMemberships(memberships);

        PartialImportResults results = doImport();

        long membershipResults = results.getResults().stream()
                .filter(r -> r.getResourceType() == ResourceType.ORGANIZATION_MEMBERSHIP)
                .count();

        assertEquals(2, membershipResults, "Same user should be assigned to both organizations");

        for (String orgId : orgIds) {
            List<MemberRepresentation> members = managedRealm.admin()
                    .organizations()
                    .get(orgId)
                    .members()
                    .getAll();

            boolean found = members.stream().anyMatch(m -> username.equals(m.getUsername()));
            assertTrue(found, "User " + username + " should be member of organization " + orgId);
        }
    }

    @Test
    public void testOrganizationMembershipWithNonExistentOrganization() {
        enableOrganizations();
        setFail();

        // Add user
        addUsers();

        // Add organization membership with non-existent organization
        List<OrganizationMembershipRepresentation> memberships = new ArrayList<>();
        OrganizationMembershipRepresentation membership = new OrganizationMembershipRepresentation();
        membership.setOrganizationId("non-existent-org-id");
        membership.setUsername(USER_PREFIX + "0");
        membership.setMembershipType(MembershipType.UNMANAGED);
        memberships.add(membership);

        piRep.setOrganizationMemberships(memberships);

        // Import should fail for the membership but users should still be imported
        PartialImportResults results = doImport();

        // With FAIL policy, import aborts and no resources should be imported
        assertEquals(0, results.getAdded());
    }

    public static class OrganizationServerConfig extends PartialImportServerConfig {
        @Override
        public org.keycloak.testframework.server.KeycloakServerConfigBuilder configure(org.keycloak.testframework.server.KeycloakServerConfigBuilder builder) {
            return super.configure(builder)
                    .option("features", "organization");
        }
    }
}
