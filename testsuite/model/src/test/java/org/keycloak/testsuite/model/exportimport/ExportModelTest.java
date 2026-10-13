/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.keycloak.common.enums.SslRequired;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.ExportImportManager;
import org.keycloak.exportimport.ExportProvider;
import org.keycloak.exportimport.dir.DirExportProviderFactory;
import org.keycloak.exportimport.singlefile.SingleFileExportProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

@RequireProvider(value = ExportProvider.class)
public class ExportModelTest extends KeycloakModelTest {

    public static final String REALM_NAME = "realm";
    private String realmId;

    @Override
    public void createEnvironment(KeycloakSession s) {
        // initialize a minimal realm with necessary entries to avoid any NPEs
        RealmModel realm = createRealm(s, REALM_NAME);
        realm.setSslRequired(SslRequired.NONE);
        RoleModel role = s.roles().addRealmRole(realm, "default");
        realm.setDefaultRole(role);
        this.realmId = realm.getId();
    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        s.realms().removeRealm(realmId);
    }

    @Test
    @RequireProvider(value = ExportProvider.class, only = SingleFileExportProviderFactory.PROVIDER_ID)
    public void testExportSingleFile() throws IOException {
        try {
            Path exportFolder = prepareTestFolder();
            Path singleFileExport = exportFolder.resolve("singleFileExport.json");

            CONFIG.spi("export")
                    .config("exporter", SingleFileExportProviderFactory.PROVIDER_ID);
            CONFIG.spi("export")
                    .provider(SingleFileExportProviderFactory.PROVIDER_ID)
                    .config(SingleFileExportProviderFactory.FILE, singleFileExport.toAbsolutePath().toString());
            CONFIG.spi("export")
                    .provider(SingleFileExportProviderFactory.PROVIDER_ID)
                    .config(SingleFileExportProviderFactory.REALM_NAME, REALM_NAME);

            inComittedTransaction(session -> {
                try (Closeable c = ExportImportConfig.setAction(ExportImportConfig.ACTION_EXPORT)) {
                    ExportImportManager exportImportManager = new ExportImportManager(session);
                    exportImportManager.runExport();
                } catch (IOException ex) {
                    log.error(ex.getMessage(), ex);
                }
            });

            // file will exist if export was successful
            Assert.assertTrue(Files.exists(singleFileExport));
        } finally {
            CONFIG.spi("export")
                    .config("exporter", null);
            CONFIG.spi("export")
                    .provider(SingleFileExportProviderFactory.PROVIDER_ID)
                    .config(SingleFileExportProviderFactory.FILE, null);
            CONFIG.spi("export")
                    .provider(SingleFileExportProviderFactory.PROVIDER_ID)
                    .config(SingleFileExportProviderFactory.REALM_NAME, null);
        }
    }

    @Test
    @RequireProvider(value = ExportProvider.class, only = DirExportProviderFactory.PROVIDER_ID)
    public void testExportDirectory() throws IOException {
        try {
            Path exportFolder = prepareTestFolder();

            CONFIG.spi("export")
                    .config("exporter", DirExportProviderFactory.PROVIDER_ID);
            CONFIG.spi("export")
                    .provider(DirExportProviderFactory.PROVIDER_ID)
                    .config(DirExportProviderFactory.DIR, exportFolder.toAbsolutePath().toString());
            CONFIG.spi("export")
                    .provider(DirExportProviderFactory.PROVIDER_ID)
                    .config(DirExportProviderFactory.REALM_NAME, REALM_NAME);

            inComittedTransaction(session -> {
                try (Closeable c = ExportImportConfig.setAction(ExportImportConfig.ACTION_EXPORT)) {
                    ExportImportManager exportImportManager = new ExportImportManager(session);
                    exportImportManager.runExport();
                } catch (IOException ex) {
                    log.error(ex.getMessage(), ex);
                }
            });

            // file will exist if export was successful
            Assert.assertTrue(Files.exists(exportFolder.resolve(REALM_NAME + "-realm.json")));
        } finally {
            CONFIG.spi("export")
                    .config("exporter", null);
            CONFIG.spi("export")
                    .provider(DirExportProviderFactory.PROVIDER_ID)
                    .config(DirExportProviderFactory.DIR, null);
            CONFIG.spi("export")
                    .provider(DirExportProviderFactory.PROVIDER_ID)
                    .config(DirExportProviderFactory.REALM_NAME, null);
        }
    }

    @Rule
    public TestName name = new TestName();

    private Path prepareTestFolder() throws IOException {
        Path singleFileExportFolder = Paths.get("target", "test", this.getClass().getName(), name.getMethodName());
        if (singleFileExportFolder.toFile().exists()) {
            FileUtils.deleteDirectory(singleFileExportFolder.toFile());
        }
        Assert.assertTrue(singleFileExportFolder.toFile().mkdirs());
        return singleFileExportFolder;
    }

}
