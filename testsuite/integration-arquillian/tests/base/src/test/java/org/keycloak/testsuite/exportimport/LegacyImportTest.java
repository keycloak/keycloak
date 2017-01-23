/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.exportimport;

import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.Config;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.singlefile.SingleFileExportProviderFactory;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ImpersonationConstants;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.exportimport.Strategy;
import static org.keycloak.testsuite.Assert.assertNames;
import static org.keycloak.testsuite.migration.MigrationTest.MIGRATION;

/**
 * Test importing JSON files exported from previous adminClient versions
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LegacyImportTest extends AbstractExportImportTest {

    @After
    public void after() {
        clearExportImportProperties();
    }


    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
    }

    @Test
    public void testLegacyImport(String version) {
        String file = "/migration-test/migration-realm-1.9.8.Final.json";
        
        URL url = LegacyImportTest.class.getResource(file);
        String targetFilePath = new File(url.getFile()).getAbsolutePath();
        testingClient.testing().exportImport().setFile(targetFilePath);
        testingClient.testing().exportImport().setProvider(SingleFileExportProviderFactory.PROVIDER_ID);
        testingClient.testing().exportImport().setAction(ExportImportConfig.ACTION_IMPORT);
        testingClient.testing().exportImport().setRealmName(MIGRATION);
        testingClient.testing().exportImport().setStrategy(Strategy.IGNORE_EXISTING);
        
        try {
            testingClient.testing().exportImport().runImport();

            RealmResource imported = adminClient.realm(MIGRATION);

            assertNames(imported.roles().list(), "offline_access", "uma_authorization", "migration-test-realm-role");
            assertNames(imported.clients().findAll(), "account", "admin-cli", "broker", "migration-test-client", "realm-management", "security-admin-console");
            String id = imported.clients().findByClientId("migration-test-client").get(0).getId();
            assertNames(imported.clients().get(id).roles().list(), "migration-test-client-role");
            assertNames(imported.users().search("", 0, 5), "migration-test-user");
            assertNames(imported.groups().groups(), "migration-test-group");
        } finally {
            removeRealm(MIGRATION);
        }
    }

    //KEYCLOAK-1982
    @Test
    public void importFrom11() throws LifecycleException {
        URL url = LegacyImportTest.class.getResource("/exportimport-test/kc11-exported-realm.json");
        String targetFilePath = new File(url.getFile()).getAbsolutePath();
        testingClient.testing().exportImport().setFile(targetFilePath);
        testingClient.testing().exportImport().setProvider(SingleFileExportProviderFactory.PROVIDER_ID);
        testingClient.testing().exportImport().setAction(ExportImportConfig.ACTION_IMPORT);

        try {
            testingClient.testing().exportImport().runImport();

            // Assert "locale" mapper available in security-admin-console client
            ClientResource foo11AdminConsoleClient = adminClient.realm("foo11").clients().get("a9ca4217-74a8-4658-92c8-c2f9ed48a474");
            assertLocaleMapperPresent(foo11AdminConsoleClient);

            // Assert "realm-management" role correctly set and contains all admin roles.
            ClientResource foo11RealmManagementClient = adminClient.realm("foo11").clients().get("c7a9cf59-feeb-44a4-a467-e008e157efa2");
            List<RoleRepresentation> roles = foo11RealmManagementClient.roles().list();
            assertRolesAvailable(roles);

            // Assert all admin roles are also available as composites of "realm-admin"
            Set<RoleRepresentation> realmAdminComposites = foo11RealmManagementClient.roles().get(AdminRoles.REALM_ADMIN).getRoleComposites();
            assertRolesAvailable(realmAdminComposites);

            // Assert all admin roles are also available as composites of "admin" role
            Set<RoleRepresentation> masterAdminComposites = adminClient.realm(Config.getAdminRealm()).roles().get(AdminRoles.ADMIN).getRoleComposites();
            assertRolesAvailable(masterAdminComposites);
        } finally {
            removeRealm("foo11");
        }
    }


    private void assertLocaleMapperPresent(ClientResource client) {
        List<ProtocolMapperRepresentation> protMappers = client.getProtocolMappers().getMappersPerProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        for (ProtocolMapperRepresentation protMapper : protMappers) {
            if (protMapper.getName().equals(OIDCLoginProtocolFactory.LOCALE)) {
                return;
            }
        }

        Assert.fail("Locale mapper not found for client");
    }


    private void assertRolesAvailable(Collection<RoleRepresentation> roles) {
        assertRoleAvailable(roles, AdminRoles.VIEW_IDENTITY_PROVIDERS);
        assertRoleAvailable(roles, AdminRoles.MANAGE_IDENTITY_PROVIDERS);
        assertRoleAvailable(roles, AdminRoles.CREATE_CLIENT);
        assertRoleAvailable(roles, ImpersonationConstants.IMPERSONATION_ROLE);
    }


    private RoleRepresentation assertRoleAvailable(Collection<RoleRepresentation> roles, String roleName) {
        for (RoleRepresentation role : roles) {
            if (role.getName().equals(roleName)) {
                return role;
            }
        }

        Assert.fail("Role " + roleName + " not found");
        return null;
    }
}
