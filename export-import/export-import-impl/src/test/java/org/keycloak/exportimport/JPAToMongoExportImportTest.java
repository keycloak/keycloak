package org.keycloak.exportimport;

import org.keycloak.exportimport.io.directory.TmpDirExportImportIOProvider;
import org.keycloak.models.Config;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * Test for full export of data from JPA and import them to Mongo. Using "directory" provider
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JPAToMongoExportImportTest extends ExportImportTestBase {

    @Override
    protected String getExportModelProvider() {
        return "jpa";
    }

    @Override
    protected String getImportModelProvider() {
        return "mongo";
    }

    @Override
    protected void exportModel(KeycloakSessionFactory factory) {
        Config.setExportImportAction(ExportImportProviderImpl.ACTION_EXPORT);
        Config.setExportImportProvider(TmpDirExportImportIOProvider.PROVIDER_ID);
        getExportImportProvider().checkExportImport(factory);
    }

    @Override
    protected void importModel(KeycloakSessionFactory factory) {
        Config.setExportImportAction(ExportImportProviderImpl.ACTION_IMPORT);
        Config.setExportImportProvider(TmpDirExportImportIOProvider.PROVIDER_ID);
        getExportImportProvider().checkExportImport(factory);
    }
}
