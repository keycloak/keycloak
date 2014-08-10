package org.keycloak.exportimport.dir;

import org.keycloak.Config;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.ExportProvider;
import org.keycloak.exportimport.ExportProviderFactory;
import org.keycloak.models.KeycloakSession;

import java.io.File;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DirExportProviderFactory implements ExportProviderFactory {

    public static final String PROVIDER_ID = "dir";

    @Override
    public ExportProvider create(KeycloakSession session) {
        String dir = ExportImportConfig.getDir();
        return dir!=null ? new DirExportProvider(new File(dir)) : new DirExportProvider();
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
