/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.model.exportimport;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.ExportImportManager;
import org.keycloak.exportimport.ExportProvider;
import org.keycloak.exportimport.ImportProvider;
import org.keycloak.exportimport.dir.DirExportProviderFactory;
import org.keycloak.exportimport.dir.DirImportProviderFactory;
import org.keycloak.exportimport.singlefile.SingleFileImportProviderFactory;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.representations.idm.ScopeMappingRepresentation;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@RequireProvider(value = ImportProvider.class)
public class ImportModelTest extends KeycloakModelTest {

    public static final String SPI_NAME = "import";

    @Override
    public void createEnvironment(KeycloakSession s) {
        // Master realm is needed for importing a realm
        if (s.realms().getRealmByName("master") == null) {
            new ApplianceBootstrap(s).createMasterRealm();
        }
        // clean-up test realm which might be left-over from a previous run
        RealmModel test = s.realms().getRealmByName("test");
        if (test != null) {
            s.realms().removeRealm(test.getId());
        }
    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        RealmModel master = s.realms().getRealmByName("master");
        if (master != null) {
            s.realms().removeRealm(master.getId());
        }
        RealmModel test = s.realms().getRealmByName("test");
        if (test != null) {
            s.realms().removeRealm(test.getId());
        }
    }

    @Test
    @RequireProvider(value = ExportProvider.class, only = SingleFileImportProviderFactory.PROVIDER_ID)
    public void testImportSingleFile() {
        try {
            Path singleFileExport = Paths.get("src/test/resources/exportimport/singleFile/testrealm.json");

            CONFIG.spi(SPI_NAME)
                    .config("importer", new SingleFileImportProviderFactory().getId());
            CONFIG.spi(SPI_NAME)
                    .provider(SingleFileImportProviderFactory.PROVIDER_ID)
                    .config(SingleFileImportProviderFactory.FILE, singleFileExport.toAbsolutePath().toString());

            inComittedTransaction(session -> {
                ExportImportConfig.setAction(ExportImportConfig.ACTION_IMPORT);
                ExportImportManager exportImportManager = new ExportImportManager(session);
                exportImportManager.runImport();
            });

            inComittedTransaction(session -> {
                Assert.assertNotNull(session.realms().getRealmByName("test"));
            });

        } finally {
            CONFIG.spi(SPI_NAME)
                    .config("importer", null);
            CONFIG.spi(SPI_NAME)
                    .provider(SingleFileImportProviderFactory.PROVIDER_ID)
                    .config(SingleFileImportProviderFactory.FILE, null);
        }
    }

    @Test
    @RequireProvider(value = ExportProvider.class, only = DirImportProviderFactory.PROVIDER_ID)
    public void testImportDirectory() {
        try {
            Path importFolder = Paths.get("src/test/resources/exportimport/dir");
            CONFIG.spi(SPI_NAME)
                    .config("importer", new DirImportProviderFactory().getId());
            CONFIG.spi(SPI_NAME)
                    .provider(DirImportProviderFactory.PROVIDER_ID)
                    .config(DirImportProviderFactory.DIR, importFolder.toAbsolutePath().toString());

            inComittedTransaction(session -> {
                ExportImportConfig.setAction(ExportImportConfig.ACTION_IMPORT);
                ExportImportManager exportImportManager = new ExportImportManager(session);
                exportImportManager.runImport();
            });

            inComittedTransaction(session -> {
                Assert.assertNotNull(session.realms().getRealmByName("test"));
            });

        } finally {
            CONFIG.spi(SPI_NAME)
                    .config("importer", null);
            CONFIG.spi(SPI_NAME)
                    .provider(DirImportProviderFactory.PROVIDER_ID)
                    .config(DirExportProviderFactory.DIR, null);
        }
    }

    @Test
    public void testImportRealmWithBatchClientsAndCrossBatchDependencies() {
        RealmRepresentation rep = new RealmRepresentation();
        rep.setRealm("test");
        rep.setEnabled(true);

        List<ClientRepresentation> clients = new ArrayList<>();
        for (int i = 0; i < 101; i++) {
            ClientRepresentation client = new ClientRepresentation();
            client.setClientId("client-" + i);
            client.setEnabled(true);
            clients.add(client);
        }
        rep.setClients(clients);

        RolesRepresentation rolesRep = new RolesRepresentation();
        Map<String, List<RoleRepresentation>> clientRoles = new HashMap<>();
        clientRoles.put("client-0", Collections.singletonList(new RoleRepresentation("role-client-0", "Client Role 0", false)));
        rolesRep.setClient(clientRoles);
        rep.setRoles(rolesRep);

        ScopeMappingRepresentation scopeMapping = new ScopeMappingRepresentation();
        scopeMapping.setClient("client-100");
        scopeMapping.setRoles(Collections.singleton("role-client-0"));

        Map<String, List<ScopeMappingRepresentation>> clientScopeMappings = new HashMap<>();
        clientScopeMappings.put("client-100", Collections.singletonList(scopeMapping));
        rep.setClientScopeMappings(clientScopeMappings);

        inComittedTransaction(session -> {
            RealmModel realm = session.realms().createRealm("test");
            RepresentationToModel.importRealm(session, rep, realm, () -> {});
        });

        inComittedTransaction(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            assertThat(realm.getClientsStream().count(), equalTo(101L));
            ClientModel client0 = realm.getClientByClientId("client-0");
            ClientModel client100 = realm.getClientByClientId("client-100");
            assertThat(client0, notNullValue());
            assertThat(client100, notNullValue());
            RoleModel role0 = client0.getRole("role-client-0");
            assertThat(role0, notNullValue());
            assertThat(client100.hasScope(role0), is(true));
        });
    }
}
