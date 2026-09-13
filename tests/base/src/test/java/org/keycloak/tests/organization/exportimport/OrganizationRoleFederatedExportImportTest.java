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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.singlefile.SingleFileExportProviderFactory;
import org.keycloak.exportimport.singlefile.SingleFileImportProviderFactory;
import org.keycloak.models.GroupModel;
import org.keycloak.models.ModelException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageUtil;
import org.keycloak.storage.jpa.entity.FederatedUserGroupMembershipEntity;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.tests.organization.admin.AbstractOrganizationTest;
import org.keycloak.testsuite.util.runonserver.ExportImportHelper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KeycloakIntegrationTest
public class OrganizationRoleFederatedExportImportTest extends AbstractOrganizationTest {

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectRealm(ref = "master", attachTo = "master")
    ManagedRealm masterRealm;

    @InjectRunOnServer(ref = "master", realmRef = "master")
    RunOnServerClient runOnServerMaster;

    private String exportFile;

    @AfterEach
    public void clearExport() {
        String file = exportFile;
        runOnServerMaster.run(session -> {
            List<String> properties = System.getProperties().stringPropertyNames().stream()
                    .filter(key -> key.startsWith(ExportImportConfig.PREFIX)).toList();
            properties.forEach(System::clearProperty);
            if (file != null) {
                Files.deleteIfExists(Path.of(file));
            }
        });
    }

    @Test
    public void shouldExcludeOrganizationMembershipFromGenericFederatedExport() {
        realm.dirty();
        String realmName = realm.getName();
        String userId = "f:1:organization-export-member";
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            RoleModel role = realm.addRole("federated-export-realm-role");
            GroupModel group = realm.createGroup("federated-export-realm-group");
            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            OrganizationModel organization = organizations.create("federated-export-org", "Federated export", "federated-export-org");
            GroupModel organizationGroup = organizations.createGroup(organization, "internal-group", null);
            RoleModel organizationRole = organization.addRole("federated-organization-role");
            RoleModel defaultRole = organization.getDefaultRole();

            var storage = UserStorageUtil.userFederatedStorage(session);
            storage.setSingleAttribute(realm, userId, "exported", "value");
            storage.grantRole(realm, userId, role);
            assertThrows(ModelException.class, () -> storage.grantRole(realm, userId, defaultRole));
            assertThrows(ModelException.class, () -> storage.grantRole(realm, userId, organizationRole));
            storage.joinGroup(realm, userId, group);
            storage.leaveGroup(realm, userId, group);
            storage.joinGroup(realm, userId, group);

            // A raw organization membership must not leak through the generic federated export.
            FederatedUserGroupMembershipEntity membership = new FederatedUserGroupMembershipEntity();
            membership.setUserId(userId);
            membership.setStorageProviderId(new StorageId(userId).getProviderId());
            membership.setGroupId(organizationGroup.getId());
            membership.setRealmId(realm.getId());
            session.getProvider(JpaConnectionProvider.class).getEntityManager().persist(membership);
        });

        exportFile = runOnServerMaster.fetchString(ExportImportHelper.getExportImportTestDirectory()).replace("\"", "")
                + "/organization-federated-roles-" + UUID.randomUUID() + ".json";
        runOnServerMaster.run(ExportImportHelper.setProvider(SingleFileExportProviderFactory.PROVIDER_ID));
        runOnServerMaster.run(ExportImportHelper.setFile(exportFile));
        runOnServerMaster.run(ExportImportHelper.setRealmName(realmName));
        runOnServerMaster.run(ExportImportHelper.setAction(ExportImportConfig.ACTION_EXPORT));
        runOnServerMaster.run(ExportImportHelper.runExport());
        realm.admin().remove();
        runOnServerMaster.run(ExportImportHelper.setProvider(SingleFileImportProviderFactory.PROVIDER_ID));
        runOnServerMaster.run(ExportImportHelper.setAction(ExportImportConfig.ACTION_IMPORT));
        runOnServerMaster.run(ExportImportHelper.runImport());
        runOnServerMaster.run(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            assertNotNull(realm);
            session.getContext().setRealm(realm);
            var storage = UserStorageUtil.userFederatedStorage(session);
            assertEquals(1, storage.getStoredUsersCount(realm));
            assertEquals("value", storage.getAttributes(realm, userId).getFirst("exported"));
            assertEquals(Set.of("federated-export-realm-role"), storage.getRoleMappingsStream(realm, userId)
                    .map(RoleModel::getName).collect(Collectors.toSet()));
            assertEquals(Set.of("federated-export-realm-group"), storage.getGroupsStream(realm, userId)
                    .map(GroupModel::getName).collect(Collectors.toSet()));
        });
    }
}
