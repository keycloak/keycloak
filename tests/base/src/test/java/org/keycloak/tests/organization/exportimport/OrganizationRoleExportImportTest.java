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

package org.keycloak.tests.organization.exportimport;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.singlefile.SingleFileExportProviderFactory;
import org.keycloak.exportimport.singlefile.SingleFileImportProviderFactory;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.MembershipType;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.organization.admin.AbstractOrganizationTest;
import org.keycloak.testsuite.util.runonserver.ExportImportHelper;
import org.keycloak.util.JsonSerialization;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class OrganizationRoleExportImportTest extends AbstractOrganizationTest {

    @InjectRealm(ref = "master", attachTo = "master")
    ManagedRealm masterRealm;

    @InjectRunOnServer(ref = "master", realmRef = "master")
    RunOnServerClient runOnServerMaster;

    @InjectAdminClient(mode = InjectAdminClient.Mode.BOOTSTRAP)
    Keycloak adminClient;

    @AfterEach
    public void clearExportImportProperties() {
        Properties properties = System.getProperties();
        Set<String> keys = new HashSet<>();
        properties.keySet().stream().map(Object::toString)
                .filter(key -> key.startsWith(ExportImportConfig.PREFIX))
                .forEach(keys::add);
        keys.forEach(properties::remove);
    }

    @Test
    public void shouldPreserveOrganizationRolesAcrossRealmExportImport() throws Exception {
        RealmResource realmResource = realm.admin();
        OrganizationRepresentation organizationRepresentation = createOrganization("acme");
        OrganizationResource organization = realmResource.organizations().get(organizationRepresentation.getId());
        MemberRepresentation member = addMember(organization);

        RoleRepresentation realmRole = new RoleRepresentation("export-realm-role", null, false);
        realmResource.roles().create(realmRole);
        realmRole = realmResource.roles().get(realmRole.getName()).toRepresentation();

        ClientRepresentation client = new ClientRepresentation();
        client.setClientId("export-client");
        client.setEnabled(true);
        String clientId;
        try (Response response = realmResource.clients().create(client)) {
            clientId = ApiUtil.getCreatedId(response);
        }
        RoleRepresentation clientRole = new RoleRepresentation("export-client-role", null, false);
        realmResource.clients().get(clientId).roles().create(clientRole);
        clientRole = realmResource.clients().get(clientId).roles().get(clientRole.getName()).toRepresentation();

        RoleRepresentation customRoleRequest = role(null, "project-admin");
        customRoleRequest.setDescription("Exported organization role");
        customRoleRequest.setAttributes(Map.of("tier", List.of("admin")));
        String customRoleId;
        try (Response response = organization.roles().create(customRoleRequest)) {
            customRoleId = ApiUtil.getCreatedId(response);
        }
        RoleRepresentation customRole = organization.roles().get(customRoleId).toRepresentation();
        RoleRepresentation defaultRole = organization.roles().getDefault().toRepresentation();
        organization.roles().getDefault().addComposites(List.of(customRole, realmRole, clientRole));
        UserRepresentation memberReference = new UserRepresentation();
        memberReference.setId(member.getId());
        organization.roles().get(customRoleId).addUserMembers(List.of(memberReference));

        String exportFile = runOnServerMaster.fetchString(ExportImportHelper.getExportImportTestDirectory()).replace("\"", "")
                + File.separator + "organization-roles-" + UUID.randomUUID() + ".json";
        runOnServerMaster.run(ExportImportHelper.setProvider(SingleFileExportProviderFactory.PROVIDER_ID));
        runOnServerMaster.run(ExportImportHelper.setFile(exportFile));
        runOnServerMaster.run(ExportImportHelper.setAction(ExportImportConfig.ACTION_EXPORT));
        runOnServerMaster.run(ExportImportHelper.setRealmName(realm.getName()));
        runOnServerMaster.run(ExportImportHelper.runExport());

        String serializedExport = runOnServerMaster.fetchString(session -> Files.readString(Path.of(exportFile)));
        RealmRepresentation exportedRealm = JsonSerialization.readValue(serializedExport, RealmRepresentation.class);
        assertExportedRealm(exportedRealm, organizationRepresentation.getAlias(), defaultRole.getName(), customRole,
                realmRole.getName(), client.getClientId(), clientRole.getName(), member.getUsername());

        realmResource.remove();
        runOnServerMaster.run(ExportImportHelper.setProvider(SingleFileImportProviderFactory.PROVIDER_ID));
        runOnServerMaster.run(ExportImportHelper.setAction(ExportImportConfig.ACTION_IMPORT));
        runOnServerMaster.run(ExportImportHelper.runImport());

        RealmResource importedRealm = adminClient.realm(realm.getName());
        OrganizationRepresentation importedRepresentation = importedRealm.organizations().list(-1, -1).stream()
                .filter(candidate -> organizationRepresentation.getAlias().equals(candidate.getAlias()))
                .findFirst()
                .orElseThrow();
        OrganizationResource importedOrganization = importedRealm.organizations().get(importedRepresentation.getId());
        RoleRepresentation importedDefault = importedOrganization.roles().getDefault().toRepresentation();
        RoleRepresentation importedCustom = importedOrganization.roles().list(false).stream()
                .filter(candidate -> customRole.getName().equals(candidate.getName()))
                .findFirst()
                .orElseThrow();

        assertEquals(defaultRole.getName(), importedDefault.getName());
        assertEquals(customRole.getId(), importedCustom.getId());
        assertEquals(customRole.getDescription(), importedCustom.getDescription());
        assertThat(importedCustom.getAttributes().get("tier"), containsInAnyOrder("admin"));
        assertThat(importedOrganization.roles().list(false).stream().map(RoleRepresentation::getName).toList(),
                containsInAnyOrder(defaultRole.getName(), customRole.getName()));
        assertThat(importedOrganization.roles().get(importedDefault.getId()).getRoleComposites().stream()
                        .map(RoleRepresentation::getName).toList(),
                containsInAnyOrder(customRole.getName(), realmRole.getName(), clientRole.getName()));
        assertThat(importedOrganization.roles().get(importedCustom.getId()).getUserMembers().stream()
                        .map(UserRepresentation::getUsername).toList(),
                containsInAnyOrder(member.getUsername()));
    }

    @Test
    public void shouldValidateOrganizationRoleImportsAtomically() {
        RealmRepresentation valid = importRealm("valid");
        OrganizationRepresentation organization = valid.getOrganizations().get(0);
        RoleRepresentation generatedDefault = role("generated-default-id", "default-roles-org-import-org");
        RoleRepresentation customDefault = role("custom-default-id", "member");
        RoleRepresentation viewer = role("viewer-id", "viewer");
        customDefault.setAttributes(Map.of("tier", List.of("base")));
        customDefault.setComposites(organizationComposites(viewer.getName()));
        organization.setRoles(List.of(generatedDefault, customDefault, viewer));
        organization.setDefaultRole(customDefault);
        MemberRepresentation member = new MemberRepresentation();
        member.setUsername("import-member");
        member.setMembershipType(MembershipType.UNMANAGED);
        member.setOrganizationRoles(List.of(viewer.getName()));
        organization.setMembers(List.of(member));

        OrganizationRepresentation defaultOnlyOrganization = organization("default-only-org-id", "default-only-org");
        RoleRepresentation defaultOnlyViewer = role("default-only-viewer-id", "viewer");
        RoleRepresentation defaultOnlyRole = role(null, "member");
        defaultOnlyRole.setComposites(organizationComposites(defaultOnlyViewer.getName()));
        defaultOnlyOrganization.setDefaultRole(defaultOnlyRole);
        defaultOnlyOrganization.setRoles(List.of(defaultOnlyViewer));
        valid.setOrganizations(List.of(organization, defaultOnlyOrganization));

        removeRealm(valid.getRealm());
        adminClient.realms().create(valid);
        try {
            RealmResource importedRealm = adminClient.realm(valid.getRealm());
            List<OrganizationRepresentation> importedOrganizations = importedRealm.organizations().list(-1, -1);
            OrganizationResource imported = organization(importedRealm, importedOrganizations, organization.getAlias());
            RoleRepresentation importedDefault = imported.roles().getDefault().toRepresentation();
            assertEquals(customDefault.getName(), importedDefault.getName());
            assertThat(imported.roles().list(false).stream().map(RoleRepresentation::getName).toList(),
                    containsInAnyOrder(generatedDefault.getName(), customDefault.getName(), viewer.getName()));
            assertThat(importedDefault.getAttributes().get("tier"), containsInAnyOrder("base"));
            assertThat(imported.roles().get(importedDefault.getId()).getRoleComposites().stream()
                    .map(RoleRepresentation::getName).toList(), containsInAnyOrder(viewer.getName()));
            assertThat(imported.roles().get(viewer.getId()).getUserMembers().stream()
                    .map(UserRepresentation::getUsername).toList(), containsInAnyOrder(member.getUsername()));

            OrganizationResource importedDefaultOnly = organization(importedRealm, importedOrganizations,
                    defaultOnlyOrganization.getAlias());
            RoleRepresentation importedDefaultOnlyRole = importedDefaultOnly.roles().getDefault().toRepresentation();
            assertEquals(defaultOnlyRole.getName(), importedDefaultOnlyRole.getName());
            assertThat(importedDefaultOnly.roles().list(false).stream().map(RoleRepresentation::getName).toList(),
                    containsInAnyOrder(defaultOnlyRole.getName(), defaultOnlyViewer.getName()));
            assertThat(importedDefaultOnly.roles().get(importedDefaultOnlyRole.getId()).getRoleComposites().stream()
                    .map(RoleRepresentation::getName).toList(), containsInAnyOrder(defaultOnlyViewer.getName()));
            assertTrue(importedDefaultOnly.members().getAll().isEmpty());
        } finally {
            removeRealm(valid.getRealm());
        }

        assertInvalidRealmImport("unnamed-role", imported -> imported.getOrganizations().get(0)
                .setRoles(List.of(new RoleRepresentation())));
        assertInvalidRealmImport("missing-organization-composite", imported -> {
            RoleRepresentation role = role("role-id", "member");
            role.setComposites(organizationComposites("missing"));
            imported.getOrganizations().get(0).setRoles(List.of(role));
        });
        assertInvalidRealmImport("cross-organization-composite", imported -> {
            RoleRepresentation foreignRole = role("foreign-role-id", "foreign");
            OrganizationRepresentation foreignOrganization = organization("foreign-org-id", "foreign-org");
            foreignOrganization.setRoles(List.of(foreignRole));
            imported.setOrganizations(List.of(imported.getOrganizations().get(0), foreignOrganization));
            RoleRepresentation role = role("role-id", "member");
            role.setComposites(organizationComposites(foreignRole.getName()));
            imported.getOrganizations().get(0).setRoles(List.of(role));
        });
        assertInvalidRealmImport("missing-realm-composite", imported -> {
            RoleRepresentation role = role("role-id", "member");
            RoleRepresentation.Composites composites = new RoleRepresentation.Composites();
            composites.setRealm(Set.of("missing"));
            role.setComposites(composites);
            imported.getOrganizations().get(0).setRoles(List.of(role));
        });
        assertInvalidRealmImport("missing-client-composite", imported -> {
            RoleRepresentation role = role("role-id", "member");
            RoleRepresentation.Composites composites = new RoleRepresentation.Composites();
            composites.setClient(Map.of("missing-client", List.of("missing-role")));
            role.setComposites(composites);
            imported.getOrganizations().get(0).setRoles(List.of(role));
        });
        assertInvalidRealmImport("missing-member", imported -> {
            MemberRepresentation missing = new MemberRepresentation();
            missing.setUsername("missing-member");
            missing.setOrganizationRoles(List.of("default-roles-org-import-org"));
            imported.getOrganizations().get(0).setMembers(List.of(missing));
        });
        assertInvalidRealmImport("missing-member-role", imported -> {
            MemberRepresentation invalidMapping = new MemberRepresentation();
            invalidMapping.setUsername("import-member");
            invalidMapping.setOrganizationRoles(List.of("missing-role"));
            imported.getOrganizations().get(0).setMembers(List.of(invalidMapping));
        });
        assertInvalidRealmImport("generic-organization-composite", imported -> {
            RoleRepresentation genericRole = role("realm-role-id", "realm-role");
            genericRole.setComposites(organizationComposites("organization-role"));
            RolesRepresentation roles = new RolesRepresentation();
            roles.setRealm(List.of(genericRole));
            imported.setRoles(roles);
        });
        assertInvalidRealmImport("generic-client-organization-composite", imported -> {
            ClientRepresentation client = new ClientRepresentation();
            client.setId("generic-client-id");
            client.setClientId("generic-client");
            client.setEnabled(true);
            imported.setClients(List.of(client));
            RoleRepresentation genericRole = role("client-role-id", "client-role");
            genericRole.setComposites(organizationComposites("organization-role"));
            RolesRepresentation roles = new RolesRepresentation();
            roles.setClient(Map.of(client.getClientId(), List.of(genericRole)));
            imported.setRoles(roles);
        });
    }

    private static void assertExportedRealm(RealmRepresentation exportedRealm, String organizationAlias,
            String defaultRoleName, RoleRepresentation customRole, String realmRoleName, String clientId,
            String clientRoleName, String memberName) {
        OrganizationRepresentation organization = exportedRealm.getOrganizations().stream()
                .filter(candidate -> organizationAlias.equals(candidate.getAlias()))
                .findFirst()
                .orElseThrow();
        assertEquals(defaultRoleName, organization.getDefaultRole().getName());
        assertThat(organization.getRoles().stream().map(RoleRepresentation::getName).toList(),
                containsInAnyOrder(defaultRoleName, customRole.getName()));
        RoleRepresentation exportedDefault = organization.getRoles().stream()
                .filter(role -> defaultRoleName.equals(role.getName())).findFirst().orElseThrow();
        RoleRepresentation exportedCustom = organization.getRoles().stream()
                .filter(role -> customRole.getName().equals(role.getName())).findFirst().orElseThrow();
        assertEquals(customRole.getId(), exportedCustom.getId());
        assertThat(exportedDefault.getComposites().getOrganization(), containsInAnyOrder(customRole.getName()));
        assertThat(exportedDefault.getComposites().getRealm(), containsInAnyOrder(realmRoleName));
        assertThat(exportedDefault.getComposites().getClient().get(clientId), containsInAnyOrder(clientRoleName));

        MemberRepresentation exportedMember = organization.getMembers().stream()
                .filter(member -> memberName.equals(member.getUsername()))
                .findFirst()
                .orElseThrow();
        assertThat(exportedMember.getOrganizationRoles(), containsInAnyOrder(customRole.getName()));
        assertTrue(organization.getMembers().stream()
                .flatMap(member -> Optional.ofNullable(member.getOrganizationRoles()).orElse(List.of()).stream())
                .noneMatch(defaultRoleName::equals));

        List<RoleRepresentation> genericRoles = new ArrayList<>(Optional.ofNullable(exportedRealm.getRoles())
                .map(RolesRepresentation::getRealm).orElse(List.of()));
        Optional.ofNullable(exportedRealm.getRoles()).map(RolesRepresentation::getClient).orElse(Map.of())
                .values().forEach(genericRoles::addAll);
        assertTrue(genericRoles.stream().map(RoleRepresentation::getComposites).filter(Objects::nonNull)
                .allMatch(composites -> composites.getOrganization() == null || composites.getOrganization().isEmpty()));
        Optional.ofNullable(exportedRealm.getUsers()).orElse(List.of()).forEach(user -> {
            assertTrue(Optional.ofNullable(user.getRealmRoles()).orElse(List.of()).stream()
                    .noneMatch(role -> role.equals(defaultRoleName) || role.equals(customRole.getName())));
            assertTrue(Optional.ofNullable(user.getClientRoles()).orElse(Map.of()).values().stream()
                    .flatMap(List::stream)
                    .noneMatch(role -> role.equals(defaultRoleName) || role.equals(customRole.getName())));
        });
    }

    private void assertInvalidRealmImport(String variant, Consumer<RealmRepresentation> mutation) {
        RealmRepresentation imported = importRealm(variant);
        mutation.accept(imported);
        removeRealm(imported.getRealm());
        try {
            WebApplicationException exception = assertThrows(WebApplicationException.class,
                    () -> adminClient.realms().create(imported));
            assertTrue(exception.getResponse().getStatus() >= 400);
            assertThrows(NotFoundException.class, () -> adminClient.realm(imported.getRealm()).toRepresentation(),
                    "Failed imports must not leave a partially-created realm");
        } finally {
            removeRealm(imported.getRealm());
        }
    }

    private static RealmRepresentation importRealm(String variant) {
        RealmRepresentation imported = new RealmRepresentation();
        imported.setRealm("organization-role-import-" + variant);
        imported.setEnabled(true);
        imported.setOrganizationsEnabled(true);
        imported.setUsers(List.of(UserBuilder.create().username("import-member").enabled(true).build()));
        imported.setOrganizations(List.of(organization("import-org-id", "import-org")));
        return imported;
    }

    private static OrganizationResource organization(RealmResource realmResource,
            List<OrganizationRepresentation> organizations, String alias) {
        OrganizationRepresentation representation = organizations.stream()
                .filter(candidate -> alias.equals(candidate.getAlias()))
                .findFirst()
                .orElseThrow();
        return realmResource.organizations().get(representation.getId());
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

    private static RoleRepresentation.Composites organizationComposites(String roleName) {
        RoleRepresentation.Composites composites = new RoleRepresentation.Composites();
        composites.setOrganization(Set.of(roleName));
        return composites;
    }

    private void removeRealm(String realmName) {
        try {
            adminClient.realm(realmName).remove();
        } catch (NotFoundException ignored) {
        }
    }
}
