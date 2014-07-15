package org.keycloak.exportimport.zip;

import java.io.File;

import org.keycloak.Config;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.ImportProvider;
import org.keycloak.exportimport.ImportProviderFactory;
import org.keycloak.models.KeycloakSession;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ZipImportProviderFactory implements ImportProviderFactory {

    @Override
    public ImportProvider create(KeycloakSession session) {
        String fileName = ExportImportConfig.getZipFile();
        String password = ExportImportConfig.getZipPassword();
        if (fileName == null) {
            throw new IllegalArgumentException("ZIP file for import not provided");
        }
        if (password == null) {
            throw new IllegalArgumentException("Password for decrypting ZIP not provided");
        }
        return new ZipImportProvider(new File(fileName), password);
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return ZipExportProviderFactory.PROVIDER_ID;
    }
}
