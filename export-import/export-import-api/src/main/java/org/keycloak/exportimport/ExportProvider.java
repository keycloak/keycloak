package org.keycloak.exportimport;

import java.io.IOException;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.Provider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface ExportProvider extends Provider {

    void exportModel(KeycloakSession session) throws IOException;

    void exportRealm(KeycloakSession session, String realmName) throws IOException;

}
