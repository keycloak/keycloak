package org.keycloak.exportimport;

import org.keycloak.models.KeycloakSessionFactory;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface ExportImportProvider {

    void checkExportImport(KeycloakSessionFactory identitySessionFactory);
}
