package org.keycloak.exportimport;


import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;

import java.io.IOException;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ExportImportManager {

    private static final Logger logger = Logger.getLogger(ExportImportManager.class);

    private KeycloakSession session;

    private final String realmName;

    private ExportProvider exportProvider;
    private ImportProvider importProvider;

    public ExportImportManager(KeycloakSession session) {
        this.session = session;

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
                logger.infof("Full model import requested. Strategy: %s", strategy.toString());
                importProvider.importModel(session.getKeycloakSessionFactory(), strategy);
            } else {
                logger.infof("Import of realm '%s' requested. Strategy: %s", realmName, strategy.toString());
                importProvider.importRealm(session.getKeycloakSessionFactory(), realmName, strategy);
            }
            logger.info("Import finished successfully");
        } catch (IOException e) {
            throw new RuntimeException("Failed to run import", e);
        }
    }

    public void runExport() {
        try {
            if (realmName == null) {
                logger.info("Full model export requested");
                exportProvider.exportModel(session.getKeycloakSessionFactory());
            } else {
                logger.infof("Export of realm '%s' requested", realmName);
                exportProvider.exportRealm(session.getKeycloakSessionFactory(), realmName);
            }
            logger.info("Export finished successfully");
        } catch (IOException e) {
            throw new RuntimeException("Failed to run export");
        }
    }

}
