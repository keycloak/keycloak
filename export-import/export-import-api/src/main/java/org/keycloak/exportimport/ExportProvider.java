package org.keycloak.exportimport;

import java.io.IOException;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.Provider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface ExportProvider extends Provider {

    void exportModel(KeycloakSessionFactory factory) throws IOException;

    void exportRealm(KeycloakSessionFactory factory, String realmName) throws IOException;

}
