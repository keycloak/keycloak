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

package org.keycloak.exportimport.dir;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.keycloak.Config;
import org.keycloak.connections.jpa.support.EntityManagers;
import org.keycloak.exportimport.AbstractFileBasedImportProvider;
import org.keycloak.exportimport.Strategy;
import org.keycloak.exportimport.util.ExportImportSessionTask;
import org.keycloak.exportimport.util.ExportImportSessionTask.Mode;
import org.keycloak.exportimport.util.ImportUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.platform.Platform;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.ServicesLogger;
import org.keycloak.storage.datastore.DefaultExportImportManager;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.KeycloakSessionUtil;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DirImportProvider extends AbstractFileBasedImportProvider {

    private final Strategy strategy;
    private final KeycloakSessionFactory factory;

    private static final Logger logger = Logger.getLogger(DirImportProvider.class);

    private File rootDirectory;

    private String realmName;

    public DirImportProvider(KeycloakSessionFactory factory, Strategy strategy) {
        this.factory = factory;
        this.strategy = strategy;
    }

    public DirImportProvider withDir(String dir) {
        this.rootDirectory = new File(dir);

        if (!this.rootDirectory.exists()) {
            throw new IllegalStateException("Directory " + this.rootDirectory + " doesn't exist");
        }

        logger.infof("Importing from directory %s", this.rootDirectory.getAbsolutePath());
        return this;
    }

    public DirImportProvider withRealmName(String realmName) {
        this.realmName = realmName;
        return this;
    }

    private File getRootDirectory() {
        if (rootDirectory == null) {
            this.rootDirectory = new File(Platform.getPlatform().getTmpDirectory(), "keycloak-export");
            if (!this.rootDirectory.exists()) {
                throw new IllegalStateException("Directory " + this.rootDirectory + " doesn't exist");
            }

            logger.infof("Importing from directory %s", this.rootDirectory.getAbsolutePath());
        }
        return rootDirectory;
    }

    @Override
    public void importModel() throws IOException {
        if (realmName != null) {
            ServicesLogger.LOGGER.realmImportRequested(realmName, strategy.toString());
            importRealm(realmName, strategy);
        } else {
            ServicesLogger.LOGGER.fullModelImport(strategy.toString());
            List<String> realmNames = getRealmsToImport();

            for (String realmName : realmNames) {
                importRealm(realmName, strategy);
                Optional.ofNullable(KeycloakSessionUtil.getKeycloakSession())
                        .ifPresent(session -> EntityManagers.flush(session, true));
            }
        }
        ServicesLogger.LOGGER.importSuccess();
    }

    @Override
    public boolean isMasterRealmExported() {
        List<String> realmNames = getRealmsToImport();
        return realmNames.contains(Config.getAdminRealm());
    }

    private List<String> getRealmsToImport() {
        File[] realmFiles = getRootDirectory().listFiles((dir, name) -> (name.endsWith("-realm.json")));
        Objects.requireNonNull(realmFiles, "Directory not found: " + getRootDirectory().getName());
        List<String> realmNames = new ArrayList<>();
        for (File file : realmFiles) {
            String fileName = file.getName();
            // Parse "foo" from "foo-realm.json"
            String realmName = fileName.substring(0, fileName.length() - 11);

            // Ensure that master realm is imported first
            if (Config.getAdminRealm().equals(realmName)) {
                realmNames.add(0, realmName);
            } else {
                realmNames.add(realmName);
            }
        }
        return realmNames;
    }

    public void importRealm(final String realmName, final Strategy strategy) throws IOException {
        File realmFile = new File(getRootDirectory() + File.separator + realmName + "-realm.json");
        File[] userFiles = getRootDirectory().listFiles((dir, name) -> name.matches(realmName + "-users-[0-9]+\\.json"));
        Objects.requireNonNull(userFiles, "directory not found: " + getRootDirectory().getName());
        File[] federatedUserFiles = getRootDirectory().listFiles((dir, name) -> name.matches(realmName + "-federated-users-[0-9]+\\.json"));
        Objects.requireNonNull(federatedUserFiles, "directory not found: " + getRootDirectory().getName());

        // Import realm first
        InputStream is = parseFile(realmFile);
        final RealmRepresentation realmRep = JsonSerialization.readValue(is, RealmRepresentation.class);
        if (!realmRep.getRealm().equals(realmName)) {
            throw new IllegalStateException(String.format("File name / realm name mismatch. %s, contains realm %s. File name should be %s", realmFile.getName(), realmRep.getRealm(), realmRep.getRealm() + "-realm.json"));
        }

        new ExportImportSessionTask() {

            @Override
            public void runExportImportTask(KeycloakSession session) {
                ImportUtils.importRealm(session, realmRep, strategy, () -> {
                    importUsers(realmName, userFiles, false);
                    importUsers(realmName, federatedUserFiles, true);
                });
            }

        }.runTask(factory);
    }

    private void importUsers(final String realmName, File[] userFiles, boolean federated) {
        for (final File userFile : userFiles) {
            try (InputStream fis = parseFile(userFile)) {
                new ExportImportSessionTask() {
                    @Override
                    protected void runExportImportTask(KeycloakSession session) throws IOException {
                        session.getContext().setRealm(session.realms().getRealmByName(realmName));
                        ImportUtils.importUsersFromStream(session, realmName, JsonSerialization.mapper, fis, federated, new DefaultExportImportManager.Batcher());
                        logger.infof("Imported %susers from %s", federated?"federated ":"", userFile.getAbsolutePath());
                    }
                }.runTask(factory, Mode.BATCHED);
            } catch (IOException e) {
                throw new RuntimeException("Error during import: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void close() {

    }
}
