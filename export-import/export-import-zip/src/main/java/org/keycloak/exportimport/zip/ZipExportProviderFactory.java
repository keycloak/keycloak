package org.keycloak.exportimport.zip;

import org.keycloak.Config;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.ExportProvider;
import org.keycloak.exportimport.ExportProviderFactory;
import org.keycloak.models.KeycloakSession;

import java.io.File;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ZipExportProviderFactory implements ExportProviderFactory {


    public static final String PROVIDER_ID = "zip";

    @Override
    public ExportProvider create(KeycloakSession session) {
        String fileName = ExportImportConfig.getZipFile();
        String password = ExportImportConfig.getZipPassword();
        if (fileName == null) {
            throw new IllegalArgumentException("ZIP file for export not provided");
        }
        if (password == null) {
            throw new IllegalArgumentException("Password for encrypting ZIP not provided");
        }
        return new ZipExportProvider(new File(fileName), password);
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
