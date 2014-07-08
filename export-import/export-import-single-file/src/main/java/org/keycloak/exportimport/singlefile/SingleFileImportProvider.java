package org.keycloak.exportimport.singlefile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.exportimport.ImportProvider;
import org.keycloak.exportimport.Strategy;
import org.keycloak.exportimport.util.ExportImportJob;
import org.keycloak.exportimport.util.ExportImportUtils;
import org.keycloak.exportimport.util.ExportUtils;
import org.keycloak.exportimport.util.ImportUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.util.JsonSerialization;

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
    public void importModel(final KeycloakSession session, final Strategy strategy) throws IOException {
        logger.infof("Full importing from file %s", this.file.getAbsolutePath());
        ExportImportUtils.runJobInTransaction(session, new ExportImportJob() {

            @Override
            public void run() throws IOException {
                FileInputStream is = new FileInputStream(file);
                ImportUtils.importFromStream(session, JsonSerialization.mapper, is, strategy);
            }

        });
    }

    @Override
    public void importRealm(KeycloakSession session, String realmName, Strategy strategy) throws IOException {
        // TODO: just that single realm in case that file contains many realms?
        importModel(session, strategy);
    }

    @Override
    public void close() {

    }
}
