package org.keycloak.exportimport.singlefile;

import java.io.File;

import org.keycloak.Config;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.ImportProvider;
import org.keycloak.exportimport.ImportProviderFactory;
import org.keycloak.models.KeycloakSession;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SingleFileImportProviderFactory implements ImportProviderFactory {

    @Override
    public ImportProvider create(KeycloakSession session) {
        String fileName = ExportImportConfig.getFile();
        return new SingleFileImportProvider(new File(fileName));
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return SingleFileExportProviderFactory.PROVIDER_ID;
    }
}
