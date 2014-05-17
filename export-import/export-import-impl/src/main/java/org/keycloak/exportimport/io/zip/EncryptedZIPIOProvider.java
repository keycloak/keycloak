package org.keycloak.exportimport.io.zip;

import org.jboss.logging.Logger;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.io.ExportImportIOProvider;
import org.keycloak.exportimport.io.ExportWriter;
import org.keycloak.exportimport.io.ImportReader;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class EncryptedZIPIOProvider implements ExportImportIOProvider {

    private static final Logger logger = Logger.getLogger(EncryptedZIPIOProvider.class);

    public static final String PROVIDER_ID = "zip";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public ExportWriter getExportWriter() {
        String zipFile = ExportImportConfig.getZipFile();
        String zipPassword = ExportImportConfig.getZipPassword();
        logger.infof("Using zip for export: " + zipFile);

        if (zipFile==null || zipPassword==null) {
            throw new IllegalArgumentException("zipFile or zipPassword are null");
        }

        return new EncryptedZIPExportWriter(zipFile, zipPassword);
    }

    @Override
    public ImportReader getImportReader() {
        String zipFile = ExportImportConfig.getZipFile();
        String zipPassword = ExportImportConfig.getZipPassword();
        logger.infof("Using zip for import: " + zipFile);

        if (zipFile==null || zipPassword==null) {
            throw new IllegalArgumentException("zipFile or zipPassword are null");
        }

        return new EncryptedZIPImportReader(zipFile, zipPassword);
    }
}
