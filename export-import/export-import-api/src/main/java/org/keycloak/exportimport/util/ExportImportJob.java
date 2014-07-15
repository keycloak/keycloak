package org.keycloak.exportimport.util;

import java.io.IOException;

import org.keycloak.models.KeycloakSession;

/**
 * Task to be executed inside transaction
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface ExportImportJob {

    public void run(KeycloakSession session) throws IOException;
}
