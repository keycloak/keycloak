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

package org.keycloak.exportimport;


import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.ServicesLogger;

import java.io.IOException;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ExportImportManager {

    private static final Logger logger = Logger.getLogger(ExportImportManager.class);

    private KeycloakSessionFactory sessionFactory;

    private final String realmName;

    private ExportProvider exportProvider;
    private ImportProvider importProvider;

    public ExportImportManager(KeycloakSession session) {
        this.sessionFactory = session.getKeycloakSessionFactory();

        realmName = ExportImportConfig.getRealmName();

        String providerId = ExportImportConfig.getProvider();
        String exportImportAction = ExportImportConfig.getAction();

        if (ExportImportConfig.ACTION_EXPORT.equals(exportImportAction)) {
            exportProvider = session.getProvider(ExportProvider.class, providerId);
            if (exportProvider == null) {
                throw new RuntimeException("Export provider '" + providerId + "' not found");
            }
        } else if (ExportImportConfig.ACTION_IMPORT.equals(exportImportAction)) {
            importProvider = session.getProvider(ImportProvider.class, providerId);
            if (importProvider == null) {
                throw new RuntimeException("Import provider '" + providerId + "' not found");
            }
        }
    }

    public boolean isRunImport() {
        return importProvider != null;
    }

    public boolean isImportMasterIncluded() {
        if (!isRunImport()) {
            throw new IllegalStateException("Import not enabled");
        }
        try {
            return importProvider.isMasterRealmExported();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isRunExport() {
        return exportProvider != null;
    }

    public void runImport() {
        try {
            Strategy strategy = ExportImportConfig.getStrategy();
            if (realmName == null) {
                ServicesLogger.LOGGER.fullModelImport(strategy.toString());
                importProvider.importModel(sessionFactory, strategy);
            } else {
                ServicesLogger.LOGGER.realmImportRequested(realmName, strategy.toString());
                importProvider.importRealm(sessionFactory, realmName, strategy);
            }
            ServicesLogger.LOGGER.importSuccess();
        } catch (IOException e) {
            throw new RuntimeException("Failed to run import", e);
        }
    }

    public void runExport() {
        try {
            if (realmName == null) {
                ServicesLogger.LOGGER.fullModelExportRequested();
                exportProvider.exportModel(sessionFactory);
            } else {
                ServicesLogger.LOGGER.realmExportRequested(realmName);
                exportProvider.exportRealm(sessionFactory, realmName);
            }
            ServicesLogger.LOGGER.exportSuccess();
        } catch (IOException e) {
            throw new RuntimeException("Failed to run export");
        }
    }

}
