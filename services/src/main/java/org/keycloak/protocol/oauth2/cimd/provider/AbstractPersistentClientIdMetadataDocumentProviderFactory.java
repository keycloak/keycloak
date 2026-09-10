package org.keycloak.protocol.oauth2.cimd.provider;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * The abstract class is the factory class of {@link AbstractPersistentClientIdMetadataDocumentProvider}.
 *
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public abstract class AbstractPersistentClientIdMetadataDocumentProviderFactory implements ClientIdMetadataDocumentProviderFactory {

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

}
