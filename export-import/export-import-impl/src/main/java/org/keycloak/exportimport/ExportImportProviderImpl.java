package org.keycloak.exportimport;

import org.jboss.logging.Logger;
import org.keycloak.exportimport.io.ExportImportIOProvider;
import org.keycloak.exportimport.io.ExportWriter;
import org.keycloak.exportimport.io.ImportReader;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.util.ProviderLoader;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ExportImportProviderImpl implements ExportImportProvider {

    private static final Logger logger = Logger.getLogger(ExportImportProviderImpl.class);

    public static final String ACTION_EXPORT = "export";
    public static final String ACTION_IMPORT = "import";

    @Override
    public void checkExportImport(KeycloakSessionFactory sessionFactory) {
        String exportImportAction = ExportImportConfig.getAction();

        boolean export = false;
        boolean importt = false;
        if (ACTION_EXPORT.equals(exportImportAction)) {
            logger.infof("Full model export requested");
            export = true;
        } else if (ACTION_IMPORT.equals(exportImportAction)) {
            logger.infof("Full model import requested");
            importt = true;
        }

        if (export || importt) {
            KeycloakSession session = sessionFactory.create();
            KeycloakTransaction transaction = session.getTransaction();
            try {
                transaction.begin();

                if (export) {
                    ExportWriter exportWriter = getProvider().getExportWriter();
                    new ModelExporter().exportModel(session.model(), exportWriter);
                    logger.infof("Export finished successfully");
                } else {
                    ImportReader importReader = getProvider().getImportReader();
                    new ModelImporter().importModel(session.model(), importReader);
                    logger.infof("Import finished successfully");
                }

                if (transaction.isActive()) {
                    if (transaction.getRollbackOnly()) {
                        transaction.rollback();
                    } else {
                        transaction.commit();
                    }
                }
            } catch (Exception e) {
                if (transaction.isActive()) {
                    session.getTransaction().rollback();
                }
                throw new RuntimeException(e);
            } finally {
                session.close();
            }
        }
    }

    private ExportImportIOProvider getProvider() {
        String providerId = ExportImportConfig.getProvider();
        logger.infof("Requested migration provider: " + providerId);

        Iterable<ExportImportIOProvider> providers = ProviderLoader.load(ExportImportIOProvider.class);
        for (ExportImportIOProvider provider : providers) {
            if (providerId.equals(provider.getId())) {
                return provider;
            }
        }

        throw new IllegalStateException("Provider " + providerId + " not found");
    }
}
