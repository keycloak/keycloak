package org.keycloak.exportimport;

import java.io.IOException;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.Provider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface ImportProvider extends Provider {

    void importModel(KeycloakSessionFactory factory, Strategy strategy) throws IOException;

    void importRealm(KeycloakSessionFactory factory, String realmName, Strategy strategy) throws IOException;
}
