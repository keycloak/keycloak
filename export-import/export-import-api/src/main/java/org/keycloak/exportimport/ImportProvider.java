package org.keycloak.exportimport;

import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.Provider;

import java.io.IOException;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface ImportProvider extends Provider {

    void importModel(KeycloakSessionFactory factory, Strategy strategy) throws IOException;

    void importRealm(KeycloakSessionFactory factory, String realmName, Strategy strategy) throws IOException;

    /**
     * @return true if master realm was previously exported and is available in the data to be imported
     * @throws IOException
     */
    boolean isMasterRealmExported() throws IOException;
}
