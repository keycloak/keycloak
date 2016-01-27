package org.keycloak.exportimport.singlefile;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;
import org.keycloak.exportimport.ExportProvider;
import org.keycloak.exportimport.util.ExportImportSessionTask;
import org.keycloak.exportimport.util.ExportUtils;
import org.keycloak.logging.KeycloakLogger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.util.JsonSerialization;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SingleFileExportProvider implements ExportProvider {

    private static final KeycloakLogger logger = Logger.getMessageLogger(KeycloakLogger.class, SingleFileExportProvider.class.getName());

    private File file;

    public SingleFileExportProvider(File file) {
        this.file = file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    @Override
    public void exportModel(KeycloakSessionFactory factory) throws IOException {
        logger.IMPORT_EXPORT.exportingModelIntoFile(this.file.getAbsolutePath());
        KeycloakModelUtils.runJobInTransaction(factory, new ExportImportSessionTask() {

            @Override
            protected void runExportImportTask(KeycloakSession session) throws IOException {
                List<RealmModel> realms = session.realms().getRealms();
                List<RealmRepresentation> reps = new ArrayList<RealmRepresentation>();
                for (RealmModel realm : realms) {
                    reps.add(ExportUtils.exportRealm(session, realm, true));
                }

                writeToFile(reps);
            }

        });

    }

    @Override
    public void exportRealm(KeycloakSessionFactory factory, final String realmName) throws IOException {
        logger.IMPORT_EXPORT.exportingRealmIntoFile(realmName, this.file.getAbsolutePath());
        KeycloakModelUtils.runJobInTransaction(factory, new ExportImportSessionTask() {

            @Override
            protected void runExportImportTask(KeycloakSession session) throws IOException {
                RealmModel realm = session.realms().getRealmByName(realmName);
                RealmRepresentation realmRep = ExportUtils.exportRealm(session, realm, true);
                writeToFile(realmRep);
            }

        });
    }

    @Override
    public void close() {
    }

    private ObjectMapper getObjectMapper() {
        return JsonSerialization.prettyMapper;
    }

    private void writeToFile(Object reps) throws IOException {
        FileOutputStream stream = new FileOutputStream(this.file);
        getObjectMapper().writeValue(stream, reps);
    }
}
