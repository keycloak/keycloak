package org.keycloak.exportimport.io.directory;

import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.io.ExportImportIOProvider;
import org.keycloak.exportimport.io.ExportWriter;
import org.keycloak.exportimport.io.ImportReader;

import java.io.File;

/**
 * Export/import into JSON files inside "tmp" directory. This implementation is used mainly for testing
 * (shouldn't be used in production due to passwords in JSON files)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class TmpDirExportImportIOProvider implements ExportImportIOProvider {

    public static final String PROVIDER_ID = "dir";

    @Override
    public ExportWriter getExportWriter() {
        String dir = ExportImportConfig.getDir();
        return dir!=null ? new TmpDirExportWriter(new File(dir)) : new TmpDirExportWriter();
    }

    @Override
    public ImportReader getImportReader() {
        String dir = ExportImportConfig.getDir();
        return dir!=null ? new TmpDirImportReader(new File(dir)) : new TmpDirImportReader();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
