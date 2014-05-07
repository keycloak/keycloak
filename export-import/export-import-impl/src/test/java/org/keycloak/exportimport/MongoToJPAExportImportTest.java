package org.keycloak.exportimport;

import java.io.File;

import org.junit.Assert;
import org.keycloak.exportimport.io.zip.EncryptedZIPIOProvider;
import org.keycloak.models.Config;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * Test for full export of data from Mongo and import them to JPA. Using export into encrypted ZIP and import from it
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoToJPAExportImportTest extends ExportImportTestBase {

    private static final String zipFile = "keycloak-export.zip";

    @Override
    protected String getExportModelProvider() {
        return "mongo";
    }

    @Override
    protected String getImportModelProvider() {
        return "jpa";
    }

    @Override
    protected void exportModel(KeycloakSessionFactory factory) {
        Config.setExportImportAction(ExportImportProviderImpl.ACTION_EXPORT);
        Config.setExportImportProvider(EncryptedZIPIOProvider.PROVIDER_ID);
        File zipFile = getZipFile();
        Config.setExportImportZipFile(zipFile.getAbsolutePath());
        Config.setExportImportZipPassword("password123");

        if (zipFile.exists()) {
            zipFile.delete();
        }

        new ExportImportProviderImpl().checkExportImport(factory);
    }

    @Override
    protected void importModel(KeycloakSessionFactory factory) {
        Config.setExportImportAction(ExportImportProviderImpl.ACTION_IMPORT);
        Config.setExportImportProvider(EncryptedZIPIOProvider.PROVIDER_ID);
        File zipFile = getZipFile();
        Config.setExportImportZipFile(zipFile.getAbsolutePath());
        Config.setExportImportZipPassword("password-invalid");

        // Try invalid password
        try {
            new ExportImportProviderImpl().checkExportImport(factory);
            Assert.fail("Not expected to be here. Exception should be thrown");
        } catch (Exception e) {};

        Config.setExportImportZipPassword("password123");
        new ExportImportProviderImpl().checkExportImport(factory);

        if (zipFile.exists()) {
            zipFile.delete();
        }
    }

    private File getZipFile() {
        String tempDir = System.getProperty("java.io.tmpdir");
        return new File(tempDir + File.separator + "keycloak-export.zip");
    }
}
