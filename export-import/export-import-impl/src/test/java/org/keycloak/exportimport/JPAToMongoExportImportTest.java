package org.keycloak.exportimport;

import org.keycloak.exportimport.io.directory.TmpDirExportImportIOProvider;
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
        ExportImportConfig.setAction(ExportImportProviderImpl.ACTION_EXPORT);
        ExportImportConfig.setProvider(TmpDirExportImportIOProvider.PROVIDER_ID);
        getExportImportProvider().checkExportImport(factory);
    }

    @Override
    protected void importModel(KeycloakSessionFactory factory) {
        ExportImportConfig.setAction(ExportImportProviderImpl.ACTION_IMPORT);
        ExportImportConfig.setProvider(TmpDirExportImportIOProvider.PROVIDER_ID);
        getExportImportProvider().checkExportImport(factory);
    }
}
