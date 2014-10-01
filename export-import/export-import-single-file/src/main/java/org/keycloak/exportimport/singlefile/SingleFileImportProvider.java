package org.keycloak.exportimport.singlefile;

import org.jboss.logging.Logger;
import org.keycloak.exportimport.ImportProvider;
import org.keycloak.exportimport.Strategy;
import org.keycloak.exportimport.util.ExportImportSessionTask;
import org.keycloak.exportimport.util.ImportUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.util.JsonSerialization;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SingleFileImportProvider implements ImportProvider {

    private static final Logger logger = Logger.getLogger(SingleFileImportProvider.class);

    private File file;

    public SingleFileImportProvider(File file) {
        this.file = file;
    }

    @Override
    public void importModel(KeycloakSessionFactory factory, final Strategy strategy) throws IOException {
        logger.infof("Full importing from file %s", this.file.getAbsolutePath());
        KeycloakModelUtils.runJobInTransaction(factory, new ExportImportSessionTask() {

            @Override
            protected void runExportImportTask(KeycloakSession session) throws IOException {
                FileInputStream is = new FileInputStream(file);
                ImportUtils.importFromStream(session, JsonSerialization.mapper, is, strategy);
            }

        });
    }

    @Override
    public void importRealm(KeycloakSessionFactory factory, String realmName, Strategy strategy) throws IOException {
        // TODO: import just that single realm in case that file contains many realms?
        importModel(factory, strategy);
    }

    @Override
    public void close() {

    }
}
