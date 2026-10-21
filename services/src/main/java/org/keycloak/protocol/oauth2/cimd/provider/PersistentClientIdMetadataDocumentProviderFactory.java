package org.keycloak.protocol.oauth2.cimd.provider;

import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.executor.ClientIdMetadataDocumentExecutor;

/**
 * The class is the factory class of {@link PersistentClientIdMetadataDocumentProvider}.
 *
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class PersistentClientIdMetadataDocumentProviderFactory extends AbstractPersistentClientIdMetadataDocumentProviderFactory {

    public static final String PROVIDER_ID = "persistent-cimd";

    @Override
    public ClientIdMetadataDocumentProvider<ClientIdMetadataDocumentExecutor.Configuration> create(KeycloakSession session) {
        return new PersistentClientIdMetadataDocumentProvider(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
