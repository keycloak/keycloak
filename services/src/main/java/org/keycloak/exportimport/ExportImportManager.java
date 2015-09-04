package org.keycloak.exportimport;


import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.managers.ApplianceBootstrap;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ExportImportManager {

    private static final Logger logger = Logger.getLogger(ExportImportManager.class);

    public void checkExportImport(KeycloakSessionFactory sessionFactory, String contextPath) {
        String exportImportAction = ExportImportConfig.getAction();
        String realmName = ExportImportConfig.getRealmName();

        boolean export = false;
        boolean importt = false;
        if (ExportImportConfig.ACTION_EXPORT.equals(exportImportAction)) {
            export = true;
        } else if (ExportImportConfig.ACTION_IMPORT.equals(exportImportAction)) {
            importt = true;
        }

        if (export || importt) {
            String exportImportProviderId = ExportImportConfig.getProvider();
            logger.debug("Will use provider: " + exportImportProviderId);
            KeycloakSession session = sessionFactory.create();

            try {
                if (export) {
                    ExportProvider exportProvider = session.getProvider(ExportProvider.class, exportImportProviderId);

                    if (exportProvider == null) {
                        logger.errorf("Invalid Export Provider %s", exportImportProviderId);
                    } else {
                        if (realmName == null) {
                            logger.info("Full model export requested");
                            exportProvider.exportModel(sessionFactory);
                        } else {
                            logger.infof("Export of realm '%s' requested", realmName);
                            exportProvider.exportRealm(sessionFactory, realmName);
                        }
                        logger.info("Export finished successfully");
                    }
                } else {
                    ImportProvider importProvider = session.getProvider(ImportProvider.class, exportImportProviderId);
                    
                    if (importProvider == null) {
                    	logger.errorf("Invalid Import Provider %s", exportImportProviderId);
                    } else {
                    
	                    Strategy strategy = ExportImportConfig.getStrategy();
	                    if (realmName == null) {
	                        logger.infof("Full model import requested. Strategy: %s", strategy.toString());
	                        
	                        // Check if master realm was exported. If it's not, then it needs to be created before other realms are imported
	                        if (!importProvider.isMasterRealmExported()) {
	                            new ApplianceBootstrap().bootstrap(sessionFactory, contextPath);
	                        }
	
	                        importProvider.importModel(sessionFactory, strategy);
	                    } else {
	                        logger.infof("Import of realm '%s' requested. Strategy: %s", realmName, strategy.toString());
	
	                        if (!realmName.equals(Config.getAdminRealm())) {
	                            // Check if master realm exists. If it's not, then it needs to be created before other realm is imported
	                            new ApplianceBootstrap().bootstrap(sessionFactory, contextPath);
	                        }
	
	                        importProvider.importRealm(sessionFactory, realmName, strategy);
	                    }
	                    logger.info("Import finished successfully");
                    }
                }
            } catch (Throwable ioe) {
                logger.error("Error during export/import", ioe);
            } finally {
                session.close();
            }
        }
    }
}
