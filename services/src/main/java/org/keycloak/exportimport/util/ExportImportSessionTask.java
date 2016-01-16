package org.keycloak.exportimport.util;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionTask;

import java.io.IOException;

/**
 * Just to wrap {@link IOException}
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class ExportImportSessionTask implements KeycloakSessionTask {

    @Override
    public void run(KeycloakSession session) {
        try {
            runExportImportTask(session);
        } catch (IOException ioe) {
            throw new RuntimeException("Error during export/import: " + ioe.getMessage(), ioe);
        }
    }

    protected abstract void runExportImportTask(KeycloakSession session) throws IOException;
}
