package org.keycloak.exportimport;

import org.junit.Assert;
import org.junit.Ignore;
import org.keycloak.exportimport.io.zip.EncryptedZIPIOProvider;
import org.keycloak.models.KeycloakSessionFactory;

import java.io.File;

/**
 * Test for full export of data from Mongo and import them to JPA. Using export into encrypted ZIP and import from it
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@Ignore
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
        ExportImportConfig.setAction(ExportImportProviderImpl.ACTION_EXPORT);
        ExportImportConfig.setProvider(EncryptedZIPIOProvider.PROVIDER_ID);
        File zipFile = getZipFile();
        ExportImportConfig.setZipFile(zipFile.getAbsolutePath());
        ExportImportConfig.setZipPassword("password123");

        if (zipFile.exists()) {
            zipFile.delete();
        }

        new ExportImportProviderImpl().checkExportImport(factory);
    }

    @Override
    protected void importModel(KeycloakSessionFactory factory) {
        ExportImportConfig.setAction(ExportImportProviderImpl.ACTION_IMPORT);
        ExportImportConfig.setProvider(EncryptedZIPIOProvider.PROVIDER_ID);
        File zipFile = getZipFile();
        ExportImportConfig.setZipFile(zipFile.getAbsolutePath());
        ExportImportConfig.setZipPassword("password-invalid");

        // Try invalid password
        try {
            new ExportImportProviderImpl().checkExportImport(factory);
            Assert.fail("Not expected to be here. Exception should be thrown");
        } catch (Exception e) {};

        ExportImportConfig.setZipPassword("password123");
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
