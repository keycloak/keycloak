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

package org.keycloak.testsuite.organization.exportimport;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.dir.DirExportProviderFactory;
import org.keycloak.exportimport.dir.DirImportProviderFactory;
import org.keycloak.exportimport.singlefile.SingleFileExportProviderFactory;
import org.keycloak.exportimport.singlefile.SingleFileImportProviderFactory;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.PartialImportRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.client.resources.TestingExportImportResource;
import org.keycloak.testsuite.organization.admin.AbstractOrganizationTest;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.util.UserBuilder;

import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OrganizationExportTest extends AbstractOrganizationTest {

    @Test
    public void testExport() {
        RealmResource providerRealm = realmsResouce().realm(bc.providerRealmName());
        List<OrganizationRepresentation> expectedOrganizations = new ArrayList<>();
        Map<String, List<String>> expectedManagedMembers = new HashMap<>();
        Map<String, List<String>> expectedUnmanagedMembers = new HashMap<>();
        Map<String, String> expectedGroupIds = new HashMap<>();

        for (int i = 0; i < 2; i++) {
            IdentityProviderRepresentation broker = bc.setUpIdentityProvider();
            broker.setAlias("broker-org-" + i);
            broker.setInternalId(null);
            String domain = "org-" + i + ".org";
            OrganizationRepresentation orgRep = createOrganization(testRealm(), getCleanup(), "org-" + i, broker, domain);
            OrganizationResource organization = testRealm().organizations().get(orgRep.getId());

            orgRep.setRedirectUrl("https://0.0.0.0:8080");
            try (Response response = organization.update(orgRep)) {
                assertThat(response.getStatus(), equalTo(Response.Status.NO_CONTENT.getStatusCode()));
            }

            // Create organization groups with hierarchy
            String deptId = createTopLevelGroup(organization, "Department-" + i);
            String teamId = createTopLevelGroup(organization, "Team-" + i);
            String devId = createSubGroup(organization, deptId, "Development-" + i);
            String qaId = createSubGroup(organization, deptId, "QA-" + i);

            expectedGroupIds.put("Department-" + i, deptId);
            expectedGroupIds.put("Team-" + i, teamId);
            expectedGroupIds.put("Development-" + i, devId);
            expectedGroupIds.put("QA-" + i, qaId);

            expectedOrganizations.add(orgRep);

            for (int j = 0; j < 3; j++) {
                UserRepresentation member = addMember(organization, "realmuser-" + j + "@" + domain);
                expectedUnmanagedMembers.computeIfAbsent(orgRep.getName(), s -> new ArrayList<>()).add(member.getUsername());
            }

            UsersResource federatedUsers = providerRealm.users();

            for (int j = 0; j < 3; j++) {
                String email = "feduser" + j + "@" + domain;

                federatedUsers.create(UserBuilder.create()
                        .username(email)
                        .email(email)
                        .firstName("f")
                        .lastName("l")
                        .enabled(true)
                        .password("password")
                        .build()).close();

                expectedManagedMembers.computeIfAbsent(orgRep.getName(), s -> new ArrayList<>()).add(email);

                openIdentityFirstLoginPage(email, true, null, false, false);

                // login to the organization identity provider and run the configured first broker login flow
                loginPage.login(email, bc.getUserPassword());
                assertIsMember(email, organization);
                testRealm().logoutAll();
                providerRealm.logoutAll();
            }

            // Add members to organization groups
            List<MemberRepresentation> orgMembers = organization.members().getAll();
            organization.groups().group(deptId).addMember(orgMembers.get(0).getId());
            organization.groups().group(teamId).addMember(orgMembers.get(1).getId());
            organization.groups().group(devId).addMember(orgMembers.get(2).getId());
        }

        RealmRepresentation importedSingleFileRealm = exportRemoveImportRealm(true);

        validateImported(expectedOrganizations, expectedManagedMembers, expectedUnmanagedMembers, expectedGroupIds, importedSingleFileRealm);

        testRealm().logoutAll();
        providerRealm.logoutAll();

        RealmRepresentation importedDirRealm = exportRemoveImportRealm(false);

        validateImported(expectedOrganizations, expectedManagedMembers, expectedUnmanagedMembers, expectedGroupIds, importedDirRealm);
    }

    private void validateImported(List<OrganizationRepresentation> expectedOrganizations,
            Map<String, List<String>> expectedManagedMembers, Map<String, List<String>> expectedUnmanagedMembers,
            Map<String, String> expectedGroupIds,
            RealmRepresentation importedRealm) {
        assertTrue(importedRealm.isOrganizationsEnabled());

        List<OrganizationRepresentation> organizations = testRealm().organizations().list(-1, -1);
        assertEquals(expectedOrganizations.size(), organizations.size());
        // id, name, alias, description and redirectUrl should have all been preserved.
        assertThat(organizations.stream().map(OrganizationRepresentation::getId).toList(),
                Matchers.containsInAnyOrder(expectedOrganizations.stream().map(OrganizationRepresentation::getId).toArray()));
        assertThat(organizations.stream().map(OrganizationRepresentation::getName).toList(),
                Matchers.containsInAnyOrder(expectedOrganizations.stream().map(OrganizationRepresentation::getName).toArray()));
        assertThat(organizations.stream().map(OrganizationRepresentation::getAlias).toList(),
                Matchers.containsInAnyOrder(expectedOrganizations.stream().map(OrganizationRepresentation::getAlias).toArray()));
        assertThat(organizations.stream().map(OrganizationRepresentation::getDescription).toList(),
                Matchers.containsInAnyOrder(expectedOrganizations.stream().map(OrganizationRepresentation::getDescription).toArray()));
        assertThat(organizations.stream().map(OrganizationRepresentation::getRedirectUrl).toList(),
                Matchers.containsInAnyOrder(expectedOrganizations.stream().map(OrganizationRepresentation::getRedirectUrl).toArray()));

        // the endpoint search method returns brief representations of orgs - to get full rep we need to fetch by id.
        for (OrganizationRepresentation organization : organizations) {
            OrganizationRepresentation fullRep = testRealm().organizations().get(organization.getId()).toRepresentation();
            // attributes should have been imported.
            assertThat(fullRep.getAttributes(), notNullValue());
            assertThat(fullRep.getAttributes().keySet(), hasSize(1));
            assertThat(fullRep.getAttributes().keySet(), hasItem("key"));
            List<String> attrValues = fullRep.getAttributes().get("key");
            assertThat(attrValues, notNullValue());
            assertThat(attrValues, containsInAnyOrder("value1", "value2"));
        }

        for (OrganizationRepresentation orgRep : organizations) {
            OrganizationResource organization = testRealm().organizations().get(orgRep.getId());
            
            // Validate members
            List<String> members = organization.members().list(-1, -1).stream().map(UserRepresentation::getEmail).toList();
            assertEquals(members.size(), expectedUnmanagedMembers.get(orgRep.getName()).size() + expectedManagedMembers.get(orgRep.getName()).size());
            assertTrue(members.containsAll(expectedUnmanagedMembers.get(orgRep.getName())));
            assertTrue(members.containsAll(expectedManagedMembers.get(orgRep.getName())));
            
            // Validate organization groups and hierarchy
            validateOrganizationGroups(organization, expectedGroupIds);
        }

        // make sure a managed user can authenticate through the broker associated with an org
        String email = expectedManagedMembers.values().stream().findAny().get().get(0);
        openIdentityFirstLoginPage(email, true, null, false, false);
        // login to the organization identity provider and run the configured first broker login flow
        loginPage.login(email, bc.getUserPassword());
        assertThat(appPage.getRequestType(),is(AppPage.RequestType.AUTH_RESPONSE));

        AuthenticationManagementResource flows = testRealm().flows();
        List<AuthenticationExecutionInfoRepresentation> executions = flows.getExecutions(DefaultAuthenticationFlows.BROWSER_FLOW);
        assertThat(executions.stream().filter(e -> "Organization".equals(e.getDisplayName())).count(), is(1L));
        executions = flows.getExecutions(DefaultAuthenticationFlows.FIRST_BROKER_LOGIN_FLOW);
        assertThat(executions.stream().filter(e -> "First Broker Login - Conditional Organization".equals(e.getDisplayName())).count(), is(1L));
    }

    private void validateOrganizationGroups(OrganizationResource organization, Map<String, String> expectedGroupIds) {
        List<GroupRepresentation> topLevelGroups = organization.groups().getAll(null, null, null, null);
        assertThat(topLevelGroups, hasSize(2));

        // Validate top-level group names
        List<String> topLevelGroupNames = topLevelGroups.stream().map(GroupRepresentation::getName).toList();
        assertThat(topLevelGroupNames, hasItem(Matchers.startsWith("Department-")));
        assertThat(topLevelGroupNames, hasItem(Matchers.startsWith("Team-")));

        // Validate group IDs are preserved
        validateGroupIds(topLevelGroups, expectedGroupIds);

        // Validate subgroups
        validateSubGroups(organization, topLevelGroups, expectedGroupIds);

        // Validate group memberships are preserved
        validateGroupMemberships(organization, topLevelGroups);
    }

    private void validateSubGroups(OrganizationResource organization, List<GroupRepresentation> topLevelGroups, Map<String, String> expectedGroupIds) {
        GroupRepresentation deptGroup = topLevelGroups.stream()
                .filter(g -> g.getName().startsWith("Department-"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Department group not found"));

        List<GroupRepresentation> subGroups = organization.groups().group(deptGroup.getId())
                .getSubGroups(null, null, null, null);
        assertThat(subGroups, hasSize(2));

        // Validate subgroup names
        List<String> subGroupNames = subGroups.stream().map(GroupRepresentation::getName).toList();
        assertThat(subGroupNames, hasItem(Matchers.startsWith("Development-")));
        assertThat(subGroupNames, hasItem(Matchers.startsWith("QA-")));

        // Validate group IDs are preserved
        validateGroupIds(subGroups, expectedGroupIds);
    }

    private void validateGroupMemberships(OrganizationResource organization, List<GroupRepresentation> topLevelGroups) {
        // Each group should have exactly 1 explicit member as added in the test setup
        GroupRepresentation deptGroup = topLevelGroups.stream().filter(g -> g.getName().startsWith("Department-")).findFirst().orElseThrow();
        List<MemberRepresentation> deptMembers = organization.groups().group(deptGroup.getId()).getMembers(null, null, null);
        assertThat(deptMembers, hasSize(1));

        GroupRepresentation teamGroup = topLevelGroups.stream().filter(g -> g.getName().startsWith("Team-")).findFirst().orElseThrow();
        List<MemberRepresentation> teamMembers = organization.groups().group(teamGroup.getId()).getMembers(null, null, null);
        assertThat(teamMembers, hasSize(1));
        
        List<GroupRepresentation> subGroups = organization.groups().group(deptGroup.getId()).getSubGroups(null, null, null, null);
        GroupRepresentation devGroup = subGroups.stream().filter(g -> g.getName().startsWith("Development-")).findFirst().orElseThrow();
        List<MemberRepresentation> devMembers = organization.groups().group(devGroup.getId()).getMembers(null, null, null);
        assertThat(devMembers, hasSize(1));
    }

    private void validateGroupIds(List<GroupRepresentation> groups, Map<String, String> expectedGroupIds) {
        for (GroupRepresentation group : groups) {
            String expectedId = expectedGroupIds.get(group.getName());
            if (expectedId != null) {
                assertEquals("Group ID mismatch for group: " + group.getName(), expectedId, group.getId());
            }
        }
    }

    @Test
    public void testExportImportEmptyOrg() {
        OrganizationRepresentation orgRep = createRepresentation("acme", "acme.com");

        try (Response response = testRealm().organizations().create(orgRep)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }
        List<OrganizationRepresentation> orgs = testRealm().organizations().list(-1, -1);
        assertEquals(1, orgs.size());

        RealmRepresentation importedSingleFileRealm = exportRemoveImportRealm(true);

        assertTrue(importedSingleFileRealm.isOrganizationsEnabled());

        orgs = testRealm().organizations().list(-1, -1);
        assertEquals(1, orgs.size());
        assertEquals("acme", orgs.get(0).getName());
    }

    private RealmRepresentation exportRemoveImportRealm(boolean file) {
        TestingExportImportResource exportImport = testingClient.testing().exportImport();
        String fileOrDir;

        //export
        if (file) {
            exportImport.setProvider(SingleFileExportProviderFactory.PROVIDER_ID);
            fileOrDir = exportImport.getExportImportTestDirectory() + File.separator + "org-export.json";
            exportImport.setFile(fileOrDir);
        } else {
            exportImport.setProvider(DirExportProviderFactory.PROVIDER_ID);
            fileOrDir = exportImport.getExportImportTestDirectory();
            exportImport.setDir(fileOrDir);
        }
        exportImport.setAction(ExportImportConfig.ACTION_EXPORT);
        exportImport.setRealmName(testRealm().toRepresentation().getRealm());
        exportImport.runExport();

        // remove the realm and import it back
        testRealm().remove();
        exportImport = testingClient.testing().exportImport();
        if (file) {
            exportImport.setProvider(SingleFileImportProviderFactory.PROVIDER_ID);
            exportImport.setFile(fileOrDir);
        } else {
            exportImport.setProvider(DirImportProviderFactory.PROVIDER_ID);
            exportImport.setDir(fileOrDir);
        }
        exportImport.setAction(ExportImportConfig.ACTION_IMPORT);
        exportImport.runImport();
        getCleanup().addCleanup(() -> {
            testRealm().remove();
            getTestContext().getTestRealmReps().clear();
        });

        return testRealm().toRepresentation();
    }

    @Test
    public void testPartialExport() {
        createOrganization();
        assertPartialExportImport(false, false);
        assertPartialExportImport(true, false);
        assertPartialExportImport(true, true);
        assertPartialExportImport(false, true);
    }

    private void assertPartialExportImport(boolean exportGroupsAndRoles, boolean exportClients) {
        RealmRepresentation export = testRealm().partialExport(exportGroupsAndRoles, exportClients);
        assertTrue(Optional.ofNullable(export.getOrganizations()).orElse(List.of()).isEmpty());
        assertTrue(Optional.ofNullable(export.getIdentityProviders()).orElse(List.of()).stream().noneMatch(idp -> Objects.nonNull(idp.getOrganizationId())));
        PartialImportRepresentation rep = new PartialImportRepresentation();
        rep.setUsers(export.getUsers());
        rep.setClients(export.getClients());
        rep.setRoles(export.getRoles());
        rep.setIdentityProviders(export.getIdentityProviders());
        rep.setGroups(export.getGroups());
        testRealm().partialImport(rep).close();
    }

    private String createTopLevelGroup(OrganizationResource organization, String name) {
        GroupRepresentation group = new GroupRepresentation();
        group.setName(name);
        try (Response response = organization.groups().addTopLevelGroup(group)) {
            assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
            return ApiUtil.getCreatedId(response);
        }
    }

    private String createSubGroup(OrganizationResource organization, String parentId, String name) {
        GroupRepresentation group = new GroupRepresentation();
        group.setName(name);
        try (Response response = organization.groups().group(parentId).addSubGroup(group)) {
            assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
            return response.readEntity(GroupRepresentation.class).getId();
        }
    }
}
