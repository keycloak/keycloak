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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
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
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.MembershipType;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.PartialImportRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.organization.admin.AbstractOrganizationTest;
import org.keycloak.testsuite.util.runonserver.ExportImportHelper;
import org.keycloak.util.JsonSerialization;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OrganizationExportTest extends AbstractOrganizationTest {

    @Test
    public void testExport() {
        RealmResource providerRealm = realmsResouce().realm(bc.providerRealmName());
        List<OrganizationRepresentation> expectedOrganizations = new ArrayList<>();
        Map<String, List<String>> expectedManagedMembers = new HashMap<>();
        Map<String, List<String>> expectedUnmanagedMembers = new HashMap<>();
        Map<String, String> expectedGroupIds = new HashMap<>();
        Map<String, String> expectedDefaultRoleNames = new HashMap<>();
        Map<String, String> expectedCustomRoleNames = new HashMap<>();
        Map<String, String> expectedCustomRoleIds = new HashMap<>();
        Map<String, String> expectedCustomRoleMembers = new HashMap<>();

        // Create realm role for org group role mapping
        RoleRepresentation realmRole = new RoleRepresentation("org-export-realm-role", "Realm role for export test", false);
        managedRealm.admin().roles().create(realmRole);
        RoleRepresentation createdRealmRole = managedRealm.admin().roles().get("org-export-realm-role").toRepresentation();

        // Create client with a role for org group role mapping
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId("org-export-test-client");
        clientRep.setEnabled(true);
        String clientUuid;
        try (Response response = managedRealm.admin().clients().create(clientRep)) {
            assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
            clientUuid = ApiUtil.getCreatedId(response);
        }
        RoleRepresentation clientRole = new RoleRepresentation("org-export-client-role", "Client role for export test", false);
        managedRealm.admin().clients().get(clientUuid).roles().create(clientRole);
        RoleRepresentation createdClientRole = managedRealm.admin().clients().get(clientUuid).roles().get("org-export-client-role").toRepresentation();

        for (int i = 0; i < 2; i++) {
            IdentityProviderRepresentation broker = bc.setUpIdentityProvider();
            broker.setAlias("broker-org-" + i);
            broker.setInternalId(null);
            String domain = "org-" + i + ".org";
            OrganizationRepresentation orgRep = createOrganization(managedRealm.admin(), getCleanup(), "org-" + i, broker, domain);
            OrganizationResource organization = managedRealm.admin().organizations().get(orgRep.getId());

            orgRep.setRedirectUrl("https://0.0.0.0:8080");
            try (Response response = organization.update(orgRep)) {
                assertThat(response.getStatus(), equalTo(Response.Status.NO_CONTENT.getStatusCode()));
            }

            RoleRepresentation defaultOrganizationRole = organization.roles().getDefault().toRepresentation();
            RoleRepresentation organizationRole = new RoleRepresentation("org-export-member-role-" + i, "Organization member role for export test", false);
            String organizationRoleId;
            try (Response response = organization.roles().create(organizationRole)) {
                assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
                organizationRoleId = ApiUtil.getCreatedId(response);
            }
            RoleRepresentation createdOrganizationRole = organization.roles().get(organizationRoleId).toRepresentation();
            organization.roles().getDefault().addComposites(List.of(createdOrganizationRole, createdRealmRole, createdClientRole));
            expectedDefaultRoleNames.put(orgRep.getName(), defaultOrganizationRole.getName());
            expectedCustomRoleNames.put(orgRep.getName(), createdOrganizationRole.getName());
            expectedCustomRoleIds.put(orgRep.getName(), createdOrganizationRole.getId());

            // Create organization groups with hierarchy
            String deptId = createTopLevelGroup(organization, "Department-" + i);
            String teamId = createTopLevelGroup(organization, "Team-" + i);
            String devId = createSubGroup(organization, deptId, "Development-" + i);
            String qaId = createSubGroup(organization, deptId, "QA-" + i);

            expectedGroupIds.put("Department-" + i, deptId);
            expectedGroupIds.put("Team-" + i, teamId);
            expectedGroupIds.put("Development-" + i, devId);
            expectedGroupIds.put("QA-" + i, qaId);

            // Add realm and client role mappings to the Department group
            organization.groups().group(deptId).roles().realmLevel().add(List.of(createdRealmRole));
            organization.groups().group(deptId).roles().clientLevel(clientUuid).add(List.of(createdClientRole));

            expectedOrganizations.add(orgRep);

            UserRepresentation customRoleMember = null;
            for (int j = 0; j < 3; j++) {
                UserRepresentation member = addMember(organization, "realmuser-" + j + "@" + domain);
                if (customRoleMember == null) {
                    customRoleMember = member;
                }
                expectedUnmanagedMembers.computeIfAbsent(orgRep.getName(), s -> new ArrayList<>()).add(member.getUsername());
            }
            UserRepresentation roleMember = new UserRepresentation();
            roleMember.setId(customRoleMember.getId());
            organization.roles().get(organizationRoleId).addUserMembers(List.of(roleMember));
            expectedCustomRoleMembers.put(orgRep.getName(), customRoleMember.getUsername());

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
                managedRealm.admin().logoutAll();
                providerRealm.logoutAll();
            }

            // Add members to organization groups
            List<MemberRepresentation> orgMembers = organization.members().getAll();
            organization.groups().group(deptId).addMember(orgMembers.get(0).getId());
            organization.groups().group(teamId).addMember(orgMembers.get(1).getId());
            organization.groups().group(devId).addMember(orgMembers.get(2).getId());
        }

        RealmRepresentation importedSingleFileRealm = exportRemoveImportRealm(true,
                exportedRealm -> validateExportedOrganizationRoles(exportedRealm, expectedManagedMembers,
                        expectedUnmanagedMembers, expectedDefaultRoleNames, expectedCustomRoleNames,
                        expectedCustomRoleIds, expectedCustomRoleMembers));

        validateImported(expectedOrganizations, expectedManagedMembers, expectedUnmanagedMembers, expectedGroupIds,
                expectedDefaultRoleNames, expectedCustomRoleNames, expectedCustomRoleIds, expectedCustomRoleMembers, importedSingleFileRealm);

        managedRealm.admin().logoutAll();
        providerRealm.logoutAll();

        RealmRepresentation importedDirRealm = exportRemoveImportRealm(false,
                exportedRealm -> validateExportedOrganizationRoles(exportedRealm, expectedManagedMembers,
                        expectedUnmanagedMembers, expectedDefaultRoleNames, expectedCustomRoleNames,
                        expectedCustomRoleIds, expectedCustomRoleMembers));

        validateImported(expectedOrganizations, expectedManagedMembers, expectedUnmanagedMembers, expectedGroupIds,
                expectedDefaultRoleNames, expectedCustomRoleNames, expectedCustomRoleIds, expectedCustomRoleMembers, importedDirRealm);
    }

    private void validateImported(List<OrganizationRepresentation> expectedOrganizations,
            Map<String, List<String>> expectedManagedMembers, Map<String, List<String>> expectedUnmanagedMembers,
            Map<String, String> expectedGroupIds,
            Map<String, String> expectedDefaultRoleNames, Map<String, String> expectedCustomRoleNames,
            Map<String, String> expectedCustomRoleIds, Map<String, String> expectedCustomRoleMembers,
            RealmRepresentation importedRealm) {
        assertTrue(importedRealm.isOrganizationsEnabled());

        List<OrganizationRepresentation> organizations = managedRealm.admin().organizations().list(-1, -1);
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
            OrganizationRepresentation fullRep = managedRealm.admin().organizations().get(organization.getId()).toRepresentation();
            // attributes should have been imported.
            assertThat(fullRep.getAttributes(), notNullValue());
            assertThat(fullRep.getAttributes().keySet(), hasSize(1));
            assertThat(fullRep.getAttributes().keySet(), hasItem("key"));
            List<String> attrValues = fullRep.getAttributes().get("key");
            assertThat(attrValues, notNullValue());
            assertThat(attrValues, containsInAnyOrder("value1", "value2"));
        }

        for (OrganizationRepresentation orgRep : organizations) {
            OrganizationResource organization = managedRealm.admin().organizations().get(orgRep.getId());
            
            // Validate members
            List<String> members = organization.members().list(-1, -1).stream().map(UserRepresentation::getEmail).toList();
            assertEquals(members.size(), expectedUnmanagedMembers.get(orgRep.getName()).size() + expectedManagedMembers.get(orgRep.getName()).size());
            assertTrue(members.containsAll(expectedUnmanagedMembers.get(orgRep.getName())));
            assertTrue(members.containsAll(expectedManagedMembers.get(orgRep.getName())));
            
            // Validate organization groups and hierarchy
            validateOrganizationGroups(organization, expectedGroupIds);
            validateOrganizationRoles(organization, orgRep, expectedDefaultRoleNames, expectedCustomRoleNames,
                    expectedCustomRoleIds, expectedCustomRoleMembers);
        }

        // make sure a managed user can authenticate through the broker associated with an org
        String email = expectedManagedMembers.values().stream().findAny().get().get(0);
        openIdentityFirstLoginPage(email, true, null, false, false);
        // login to the organization identity provider and run the configured first broker login flow
        loginPage.login(email, bc.getUserPassword());
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());

        AuthenticationManagementResource flows = managedRealm.admin().flows();
        List<AuthenticationExecutionInfoRepresentation> executions = flows.getExecutions(DefaultAuthenticationFlows.BROWSER_FLOW);
        assertThat(executions.stream().filter(e -> "Organization".equals(e.getDisplayName())).count(), is(1L));
        executions = flows.getExecutions(DefaultAuthenticationFlows.FIRST_BROKER_LOGIN_FLOW);
        assertThat(executions.stream().filter(e -> "First Broker Login - Conditional Organization".equals(e.getDisplayName())).count(), is(1L));
    }

    private void validateOrganizationRoles(OrganizationResource organization, OrganizationRepresentation orgRep,
            Map<String, String> expectedDefaultRoleNames, Map<String, String> expectedCustomRoleNames,
            Map<String, String> expectedCustomRoleIds, Map<String, String> expectedCustomRoleMembers) {
        String expectedDefaultRoleName = expectedDefaultRoleNames.get(orgRep.getName());
        String expectedCustomRoleName = expectedCustomRoleNames.get(orgRep.getName());
        RoleRepresentation defaultRole = organization.roles().getDefault().toRepresentation();
        List<RoleRepresentation> roles = organization.roles().list(false);
        List<String> roleNames = roles.stream().map(RoleRepresentation::getName).toList();

        assertThat(defaultRole.getName(), equalTo(expectedDefaultRoleName));
        assertThat(roleNames, containsInAnyOrder(expectedDefaultRoleName, expectedCustomRoleName));
        assertThat(roleNames.stream().filter(expectedDefaultRoleName::equals).count(), is(1L));

        RoleRepresentation customRole = roles.stream()
                .filter(role -> expectedCustomRoleName.equals(role.getName()))
                .findFirst()
                .orElseThrow();
        assertThat(customRole.getId(), is(expectedCustomRoleIds.get(orgRep.getName())));
        List<String> compositeNames = organization.roles().get(defaultRole.getId()).getRoleComposites()
                .stream()
                .map(RoleRepresentation::getName)
                .toList();
        assertThat(compositeNames, containsInAnyOrder(expectedCustomRoleName, "org-export-realm-role", "org-export-client-role"));

        List<String> customRoleUsers = organization.roles().get(customRole.getId()).getUserMembers()
                .stream()
                .map(UserRepresentation::getUsername)
                .toList();
        assertThat(customRoleUsers, hasItem(expectedCustomRoleMembers.get(orgRep.getName())));
    }

    private void validateExportedOrganizationRoles(RealmRepresentation exportedRealm,
            Map<String, List<String>> expectedManagedMembers, Map<String, List<String>> expectedUnmanagedMembers,
            Map<String, String> expectedDefaultRoleNames, Map<String, String> expectedCustomRoleNames,
            Map<String, String> expectedCustomRoleIds, Map<String, String> expectedCustomRoleMembers) {
        List<String> organizationRoleNames = new ArrayList<>();

        for (OrganizationRepresentation organization : exportedRealm.getOrganizations()) {
            String organizationName = organization.getName();
            String defaultRoleName = expectedDefaultRoleNames.get(organizationName);
            String customRoleName = expectedCustomRoleNames.get(organizationName);
            organizationRoleNames.add(defaultRoleName);
            organizationRoleNames.add(customRoleName);

            assertThat(organization.getDefaultRole().getName(), is(defaultRoleName));
            assertThat(organization.getRoles().stream().map(RoleRepresentation::getName).toList(),
                    containsInAnyOrder(defaultRoleName, customRoleName));

            RoleRepresentation defaultRole = organization.getRoles().stream()
                    .filter(role -> defaultRoleName.equals(role.getName()))
                    .findFirst()
                    .orElseThrow();
            RoleRepresentation customRole = organization.getRoles().stream()
                    .filter(role -> customRoleName.equals(role.getName()))
                    .findFirst()
                    .orElseThrow();
            assertThat(customRole.getId(), is(expectedCustomRoleIds.get(organizationName)));
            assertThat(defaultRole.getComposites().getOrganization(), containsInAnyOrder(customRoleName));
            assertThat(defaultRole.getComposites().getRealm(), containsInAnyOrder("org-export-realm-role"));
            assertThat(defaultRole.getComposites().getClient().get("org-export-test-client"),
                    containsInAnyOrder("org-export-client-role"));

            List<MemberRepresentation> managedMembers = organization.getMembers().stream()
                    .filter(member -> MembershipType.MANAGED.equals(member.getMembershipType()))
                    .toList();
            assertThat(managedMembers.stream().map(MemberRepresentation::getUsername).toList(),
                    containsInAnyOrder(expectedManagedMembers.get(organizationName).toArray()));
            assertTrue(managedMembers.stream().allMatch(member -> member.getOrganizationRoles() == null
                    || member.getOrganizationRoles().isEmpty()));

            List<MemberRepresentation> unmanagedMembers = organization.getMembers().stream()
                    .filter(member -> MembershipType.UNMANAGED.equals(member.getMembershipType()))
                    .toList();
            assertThat(unmanagedMembers.stream().map(MemberRepresentation::getUsername).toList(),
                    containsInAnyOrder(expectedUnmanagedMembers.get(organizationName).toArray()));
            MemberRepresentation mappedMember = unmanagedMembers.stream()
                    .filter(member -> expectedCustomRoleMembers.get(organizationName).equals(member.getUsername()))
                    .findFirst()
                    .orElseThrow();
            assertThat(mappedMember.getOrganizationRoles(), containsInAnyOrder(customRoleName));
            assertTrue(organization.getMembers().stream()
                    .flatMap(member -> Optional.ofNullable(member.getOrganizationRoles()).orElse(List.of()).stream())
                    .noneMatch(defaultRoleName::equals));
        }

        assertGenericRolesDoNotContainOrganizationComposites(exportedRealm.getRoles());
        assertGlobalMappingsDoNotContainOrganizationRoles(exportedRealm.getUsers(), organizationRoleNames);
        assertGlobalMappingsDoNotContainOrganizationRoles(exportedRealm.getFederatedUsers(), organizationRoleNames);
    }

    private void assertGenericRolesDoNotContainOrganizationComposites(RolesRepresentation roles) {
        if (roles == null) {
            return;
        }

        List<RoleRepresentation> genericRoles = new ArrayList<>(Optional.ofNullable(roles.getRealm()).orElse(List.of()));
        Optional.ofNullable(roles.getClient()).orElse(Map.of()).values().forEach(genericRoles::addAll);
        assertTrue(genericRoles.stream().map(RoleRepresentation::getComposites).filter(Objects::nonNull)
                .allMatch(composites -> composites.getOrganization() == null || composites.getOrganization().isEmpty()));
    }

    private void assertGlobalMappingsDoNotContainOrganizationRoles(List<UserRepresentation> users,
            List<String> organizationRoleNames) {
        Optional.ofNullable(users).orElse(List.of()).forEach(user -> {
            assertTrue(Optional.ofNullable(user.getRealmRoles()).orElse(List.of()).stream()
                    .noneMatch(organizationRoleNames::contains));
            assertTrue(Optional.ofNullable(user.getClientRoles()).orElse(Map.of()).values().stream()
                    .flatMap(List::stream)
                    .noneMatch(organizationRoleNames::contains));
        });
    }

    private void validateOrganizationGroups(OrganizationResource organization, Map<String, String> expectedGroupIds) {
        List<GroupRepresentation> topLevelGroups = organization.groups().getAll(null, null, null, null, null, true, false);
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

        // Validate role mappings are preserved
        validateGroupRoleMappings(organization, topLevelGroups);
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

    private void validateGroupRoleMappings(OrganizationResource organization, List<GroupRepresentation> topLevelGroups) {
        GroupRepresentation deptGroup = topLevelGroups.stream().filter(g -> g.getName().startsWith("Department-")).findFirst().orElseThrow();

        // Validate via role mapping API
        List<RoleRepresentation> realmRoles = organization.groups().group(deptGroup.getId()).roles().realmLevel().listAll();
        assertThat(realmRoles, hasSize(1));
        assertThat(realmRoles.get(0).getName(), equalTo("org-export-realm-role"));

        List<RoleRepresentation> clientRoles = organization.groups().group(deptGroup.getId()).roles()
                .clientLevel(managedRealm.admin().clients().findByClientId("org-export-test-client").get(0).getId()).listAll();
        assertThat(clientRoles, hasSize(1));
        assertThat(clientRoles.get(0).getName(), equalTo("org-export-client-role"));

        // Validate via group representation
        GroupRepresentation deptRep = organization.groups().group(deptGroup.getId()).toRepresentation(false);
        assertThat(deptRep.getRealmRoles(), hasSize(1));
        assertThat(deptRep.getRealmRoles(), hasItem("org-export-realm-role"));
        assertThat(deptRep.getClientRoles(), notNullValue());
        assertThat(deptRep.getClientRoles().get("org-export-test-client"), hasSize(1));
        assertThat(deptRep.getClientRoles().get("org-export-test-client"), hasItem("org-export-client-role"));

        // Team group should have no role mappings
        GroupRepresentation teamGroup = topLevelGroups.stream().filter(g -> g.getName().startsWith("Team-")).findFirst().orElseThrow();
        List<RoleRepresentation> teamRealmRoles = organization.groups().group(teamGroup.getId()).roles().realmLevel().listAll();
        assertThat(teamRealmRoles, hasSize(0));
    }

    private void validateGroupIds(List<GroupRepresentation> groups, Map<String, String> expectedGroupIds) {
        for (GroupRepresentation group : groups) {
            String expectedId = expectedGroupIds.get(group.getName());
            if (expectedId != null) {
                assertEquals(expectedId, group.getId(), "Group ID mismatch for group: " + group.getName());
            }
        }
    }

    @Test
    public void testExportImportEmptyOrg() {
        OrganizationRepresentation orgRep = createRepresentation("acme", "acme.com");

        try (Response response = managedRealm.admin().organizations().create(orgRep)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }
        List<OrganizationRepresentation> orgs = managedRealm.admin().organizations().list(-1, -1);
        assertEquals(1, orgs.size());

        RealmRepresentation importedSingleFileRealm = exportRemoveImportRealm(true);

        assertTrue(importedSingleFileRealm.isOrganizationsEnabled());

        orgs = managedRealm.admin().organizations().list(-1, -1);
        assertEquals(1, orgs.size());
        assertEquals("acme", orgs.get(0).getName());
    }

    @Test
    public void testOrganizationRoleImportValidationAndAtomicity() {
        RealmRepresentation valid = importRealm("valid");
        OrganizationRepresentation validOrganization = valid.getOrganizations().get(0);
        RoleRepresentation generatedDefault = role("generated-default-id", "default-roles-org-import-org");
        RoleRepresentation customDefault = role("custom-default-id", "member");
        RoleRepresentation viewer = role("viewer-id", "viewer");
        customDefault.setAttributes(Map.of("tier", List.of("base")));
        RoleRepresentation.Composites customDefaultComposites = new RoleRepresentation.Composites();
        customDefaultComposites.setOrganization(Set.of(viewer.getName()));
        customDefault.setComposites(customDefaultComposites);
        validOrganization.setRoles(List.of(generatedDefault, customDefault, viewer));
        validOrganization.setDefaultRole(customDefault);
        MemberRepresentation member = new MemberRepresentation();
        member.setUsername("import-member");
        member.setMembershipType(MembershipType.UNMANAGED);
        member.setOrganizationRoles(List.of(viewer.getName()));
        validOrganization.setMembers(List.of(member));

        OrganizationRepresentation defaultOnlyOrganization = organization("default-only-org-id", "default-only-org");
        RoleRepresentation defaultOnlyViewer = role("default-only-viewer-id", "viewer");
        RoleRepresentation defaultOnlyRole = role(null, "member");
        RoleRepresentation.Composites defaultOnlyComposites = new RoleRepresentation.Composites();
        defaultOnlyComposites.setOrganization(Set.of(defaultOnlyViewer.getName()));
        defaultOnlyRole.setComposites(defaultOnlyComposites);
        defaultOnlyOrganization.setDefaultRole(defaultOnlyRole);
        defaultOnlyOrganization.setRoles(List.of(defaultOnlyViewer));
        valid.setOrganizations(List.of(validOrganization, defaultOnlyOrganization));

        adminClient.realms().create(valid);
        try {
            RealmResource importedRealm = adminClient.realm(valid.getRealm());
            List<OrganizationRepresentation> importedOrganizations = importedRealm.organizations().list(-1, -1);
            OrganizationRepresentation importedOrganization = importedOrganizations.stream()
                    .filter(organization -> validOrganization.getAlias().equals(organization.getAlias()))
                    .findFirst()
                    .orElseThrow();
            OrganizationResource organization = importedRealm.organizations().get(importedOrganization.getId());
            RoleRepresentation importedDefault = organization.roles().getDefault().toRepresentation();
            assertEquals(customDefault.getName(), importedDefault.getName());
            assertThat(organization.roles().list(false).stream().map(RoleRepresentation::getName).toList(),
                    containsInAnyOrder(generatedDefault.getName(), customDefault.getName(), viewer.getName()));
            assertThat(importedDefault.getAttributes().get("tier"), containsInAnyOrder("base"));
            assertThat(organization.roles().get(importedDefault.getId()).getRoleComposites().stream()
                    .map(RoleRepresentation::getName).toList(), containsInAnyOrder(viewer.getName()));
            assertThat(organization.roles().get(viewer.getId()).getUserMembers().stream()
                    .map(UserRepresentation::getUsername).toList(), containsInAnyOrder(member.getUsername()));

            OrganizationRepresentation importedDefaultOnlyOrganization = importedOrganizations.stream()
                    .filter(organizationRepresentation -> defaultOnlyOrganization.getAlias().equals(organizationRepresentation.getAlias()))
                    .findFirst()
                    .orElseThrow();
            OrganizationResource defaultOnly = importedRealm.organizations().get(importedDefaultOnlyOrganization.getId());
            RoleRepresentation importedDefaultOnlyRole = defaultOnly.roles().getDefault().toRepresentation();
            assertEquals(defaultOnlyRole.getName(), importedDefaultOnlyRole.getName());
            assertThat(defaultOnly.roles().list(false).stream().map(RoleRepresentation::getName).toList(),
                    containsInAnyOrder(defaultOnlyRole.getName(), defaultOnlyViewer.getName()));
            assertThat(defaultOnly.roles().get(importedDefaultOnlyRole.getId()).getRoleComposites().stream()
                    .map(RoleRepresentation::getName).toList(), containsInAnyOrder(defaultOnlyViewer.getName()));
            assertTrue(defaultOnly.members().getAll().isEmpty());
        } finally {
            adminClient.realm(valid.getRealm()).remove();
        }

        assertInvalidRealmImport("unnamed-role", realm -> realm.getOrganizations().get(0).setRoles(List.of(new RoleRepresentation())));
        assertInvalidRealmImport("missing-organization-composite", realm -> {
            RoleRepresentation role = role("role-id", "member");
            RoleRepresentation.Composites composites = new RoleRepresentation.Composites();
            composites.setOrganization(Set.of("missing"));
            role.setComposites(composites);
            realm.getOrganizations().get(0).setRoles(List.of(role));
        });
        assertInvalidRealmImport("cross-organization-composite", realm -> {
            RoleRepresentation foreignRole = role("foreign-role-id", "foreign");
            OrganizationRepresentation foreignOrganization = organization("foreign-org-id", "foreign-org");
            foreignOrganization.setRoles(List.of(foreignRole));
            realm.setOrganizations(List.of(realm.getOrganizations().get(0), foreignOrganization));

            RoleRepresentation role = role("role-id", "member");
            RoleRepresentation.Composites composites = new RoleRepresentation.Composites();
            composites.setOrganization(Set.of(foreignRole.getName()));
            role.setComposites(composites);
            realm.getOrganizations().get(0).setRoles(List.of(role));
        });
        assertInvalidRealmImport("missing-realm-composite", realm -> {
            RoleRepresentation role = role("role-id", "member");
            RoleRepresentation.Composites composites = new RoleRepresentation.Composites();
            composites.setRealm(Set.of("missing"));
            role.setComposites(composites);
            realm.getOrganizations().get(0).setRoles(List.of(role));
        });
        assertInvalidRealmImport("missing-client-composite", realm -> {
            RoleRepresentation role = role("role-id", "member");
            RoleRepresentation.Composites composites = new RoleRepresentation.Composites();
            composites.setClient(Map.of("missing-client", List.of("missing-role")));
            role.setComposites(composites);
            realm.getOrganizations().get(0).setRoles(List.of(role));
        });
        assertInvalidRealmImport("missing-member", realm -> {
            MemberRepresentation missing = new MemberRepresentation();
            missing.setUsername("missing-member");
            missing.setOrganizationRoles(List.of("default-roles-org-import-org"));
            realm.getOrganizations().get(0).setMembers(List.of(missing));
        });
        assertInvalidRealmImport("missing-member-role", realm -> {
            MemberRepresentation invalidMapping = new MemberRepresentation();
            invalidMapping.setUsername("import-member");
            invalidMapping.setOrganizationRoles(List.of("missing-role"));
            realm.getOrganizations().get(0).setMembers(List.of(invalidMapping));
        });
        assertInvalidRealmImport("generic-organization-composite", realm -> {
            RoleRepresentation genericRole = role("realm-role-id", "realm-role");
            RoleRepresentation.Composites composites = new RoleRepresentation.Composites();
            composites.setOrganization(Set.of("organization-role"));
            genericRole.setComposites(composites);
            RolesRepresentation roles = new RolesRepresentation();
            roles.setRealm(List.of(genericRole));
            realm.setRoles(roles);
        });
        assertInvalidRealmImport("generic-client-organization-composite", realm -> {
            ClientRepresentation client = new ClientRepresentation();
            client.setId("generic-client-id");
            client.setClientId("generic-client");
            client.setEnabled(true);
            realm.setClients(List.of(client));

            RoleRepresentation genericRole = role("client-role-id", "client-role");
            RoleRepresentation.Composites composites = new RoleRepresentation.Composites();
            composites.setOrganization(Set.of("organization-role"));
            genericRole.setComposites(composites);
            RolesRepresentation roles = new RolesRepresentation();
            roles.setClient(Map.of(client.getClientId(), List.of(genericRole)));
            realm.setRoles(roles);
        });
    }

    private void assertInvalidRealmImport(String variant, Consumer<RealmRepresentation> mutation) {
        RealmRepresentation realm = importRealm(variant);
        mutation.accept(realm);

        WebApplicationException exception = assertThrows(WebApplicationException.class, () -> adminClient.realms().create(realm));
        assertTrue(exception.getResponse().getStatus() >= 400);
        assertThrows(NotFoundException.class, () -> adminClient.realm(realm.getRealm()).toRepresentation(),
                "Failed imports must not leave a partially-created realm");
    }

    private static RealmRepresentation importRealm(String variant) {
        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm("organization-role-import-" + variant);
        realm.setEnabled(true);
        realm.setOrganizationsEnabled(true);
        realm.setUsers(List.of(UserBuilder.create().username("import-member").enabled(true).build()));

        OrganizationRepresentation organization = organization("import-org-id", "import-org");
        realm.setOrganizations(List.of(organization));
        return realm;
    }

    private static OrganizationRepresentation organization(String id, String alias) {
        OrganizationRepresentation organization = new OrganizationRepresentation();
        organization.setId(id);
        organization.setName(alias);
        organization.setAlias(alias);
        return organization;
    }

    private static RoleRepresentation role(String id, String name) {
        RoleRepresentation role = new RoleRepresentation();
        role.setId(id);
        role.setName(name);
        return role;
    }

    private RealmRepresentation exportRemoveImportRealm(boolean file) {
        return exportRemoveImportRealm(file, ignored -> {
        });
    }

    private RealmRepresentation exportRemoveImportRealm(boolean file, Consumer<RealmRepresentation> exportValidator) {
        String fileOrDir;

        //export
        if (file) {
            runOnServerMaster.run(ExportImportHelper.setProvider(SingleFileExportProviderFactory.PROVIDER_ID));
            fileOrDir = runOnServerMaster.fetchString(ExportImportHelper.getExportImportTestDirectory()).replace("\"","") + File.separator + "org-export.json";
            runOnServerMaster.run(ExportImportHelper.setFile(fileOrDir));
        } else {
            runOnServerMaster.run(ExportImportHelper.setProvider(DirExportProviderFactory.PROVIDER_ID));
            fileOrDir = runOnServerMaster.fetchString(ExportImportHelper.getExportImportTestDirectory()).replace("\"","");
            runOnServerMaster.run(ExportImportHelper.setDir(fileOrDir));
        }
        runOnServerMaster.run(ExportImportHelper.setAction(ExportImportConfig.ACTION_EXPORT));
        runOnServerMaster.run(ExportImportHelper.setRealmName(managedRealm.admin().toRepresentation().getRealm()));
        runOnServerMaster.run(ExportImportHelper.runExport());

        String realmName = managedRealm.admin().toRepresentation().getRealm();
        String realmFile = file ? fileOrDir : fileOrDir + File.separator + realmName + "-realm.json";
        String serializedExport = runOnServerMaster.fetchString(session -> Files.readString(Path.of(realmFile)));
        RealmRepresentation exportedRealm = Assertions.assertDoesNotThrow(() -> {
            String exportedJson = JsonSerialization.readValue(serializedExport, String.class);
            return JsonSerialization.readValue(exportedJson, RealmRepresentation.class);
        });
        exportValidator.accept(exportedRealm);


        // remove the realm and import it back
        managedRealm.admin().remove();
        if (file) {
            runOnServerMaster.run(ExportImportHelper.setProvider(SingleFileImportProviderFactory.PROVIDER_ID));
            runOnServerMaster.run(ExportImportHelper.setFile(fileOrDir));
        } else {
            runOnServerMaster.run(ExportImportHelper.setProvider(DirImportProviderFactory.PROVIDER_ID));
            runOnServerMaster.run(ExportImportHelper.setDir(fileOrDir));
        }
        runOnServerMaster.run(ExportImportHelper.setAction(ExportImportConfig.ACTION_IMPORT));
        runOnServerMaster.run(ExportImportHelper.runImport());
        getCleanup().addCleanup(() -> {
            managedRealm.admin().remove();
            getTestContext().getTestRealmReps().clear();
        });

        return managedRealm.admin().toRepresentation();
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
        RealmRepresentation export = managedRealm.admin().partialExport(exportGroupsAndRoles, exportClients);
        assertTrue(Optional.ofNullable(export.getOrganizations()).orElse(List.of()).isEmpty());
        assertTrue(Optional.ofNullable(export.getIdentityProviders()).orElse(List.of()).stream().noneMatch(idp -> Objects.nonNull(idp.getOrganizationId())));
        PartialImportRepresentation rep = new PartialImportRepresentation();
        rep.setUsers(export.getUsers());
        rep.setClients(export.getClients());
        rep.setRoles(export.getRoles());
        rep.setIdentityProviders(export.getIdentityProviders());
        rep.setGroups(export.getGroups());
        managedRealm.admin().partialImport(rep).close();
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
