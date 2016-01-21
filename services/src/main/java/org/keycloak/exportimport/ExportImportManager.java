package org.keycloak.exportimport;


import org.keycloak.services.ServicesLogger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.io.IOException;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ExportImportManager {

    private static final ServicesLogger logger = ServicesLogger.ROOT_LOGGER;

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
                throw new RuntimeException("Export provider not found");
            }
        } else if (ExportImportConfig.ACTION_IMPORT.equals(exportImportAction)) {
            importProvider = session.getProvider(ImportProvider.class, providerId);
            if (importProvider == null) {
                throw new RuntimeException("Import provider not found");
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
                logger.fullModelImport(strategy.toString());
                importProvider.importModel(sessionFactory, strategy);
            } else {
                logger.realmImportRequested(realmName, strategy.toString());
                importProvider.importRealm(sessionFactory, realmName, strategy);
            }
            logger.importSuccess();
        } catch (IOException e) {
            throw new RuntimeException("Failed to run import", e);
        }
    }

    public void runExport() {
        try {
            if (realmName == null) {
                logger.fullModelExportRequested();
                exportProvider.exportModel(sessionFactory);
            } else {
                logger.realmExportRequested(realmName);
                exportProvider.exportRealm(sessionFactory, realmName);
            }
            logger.exportSuccess();
        } catch (IOException e) {
            throw new RuntimeException("Failed to run export");
        }
    }

}
