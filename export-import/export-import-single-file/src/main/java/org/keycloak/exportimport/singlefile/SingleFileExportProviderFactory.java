package org.keycloak.exportimport.singlefile;

import org.keycloak.Config;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.ExportProvider;
import org.keycloak.exportimport.ExportProviderFactory;
import org.keycloak.models.KeycloakSession;

import java.io.File;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SingleFileExportProviderFactory implements ExportProviderFactory {

    public static final String PROVIDER_ID = "singleFile";

    @Override
    public ExportProvider create(KeycloakSession session) {
        String fileName = ExportImportConfig.getFile();
        return new SingleFileExportProvider(new File(fileName));
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
