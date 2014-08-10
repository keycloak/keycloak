package org.keycloak.exportimport.dir;

import org.keycloak.Config;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.ImportProvider;
import org.keycloak.exportimport.ImportProviderFactory;
import org.keycloak.models.KeycloakSession;

import java.io.File;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DirImportProviderFactory implements ImportProviderFactory {

    @Override
    public ImportProvider create(KeycloakSession session) {
        String dir = ExportImportConfig.getDir();
        return dir!=null ? new DirImportProvider(new File(dir)) : new DirImportProvider();
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return DirExportProviderFactory.PROVIDER_ID;
    }
}
