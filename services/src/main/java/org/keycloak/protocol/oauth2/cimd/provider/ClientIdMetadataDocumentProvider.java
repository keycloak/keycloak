package org.keycloak.protocol.oauth2.cimd.provider;

import org.keycloak.models.ClientModel;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.executor.AbstractClientIdMetadataDocumentExecutor;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.executor.AbstractClientIdMetadataDocumentExecutor.FetchOperation;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.executor.AbstractClientIdMetadataDocumentExecutor.OIDCClientRepresentationWithCacheControl;
import org.keycloak.provider.Provider;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;

/**
 * The interface provides the following features:
 *
 * <ul>
 *     <li>Determining if (re-)fetching a client metadata is needed</li>
 *     <li>Concrete process of caching a client metadata: create and update</li>
 *     <li>Update cache expiry time</li>
 *     <li>Augment a client metadata in {@code ClientRepresentation}</li>
 * </ul>
 *
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public interface ClientIdMetadataDocumentProvider<CONFIG extends AbstractClientIdMetadataDocumentExecutor.Configuration> extends Provider {

    /**
     * Gets a configuration of an executor for Client ID metadata document.
     * @return {@code CONFIG extends AbstractClientIdMetadataDocumentExecutor.Configuration}
     */
    CONFIG getConfiguration();

    /**
     * Sets a configuration of an executor for Client ID metadata document.
     * @param configuration a configuration of an executor for Client ID metadata document, not {@code null}
     */
    void setConfiguration(CONFIG configuration);

    /**
     * Sets a cache expiry time in sec to a client metadata.
     * @param clientRep a client metadata in {@code ClientRepresentation}, not {@code null}
     * @param cacheExpiryTimeInSec when a cache expires in sec
     */
    void setCacheExpiryTimeToClientMetadata(ClientRepresentation clientRep, int cacheExpiryTimeInSec);

    /**
     * Sets a cache expiry time in sec to a client metadata.
     * @param clientModel a client metadata in {@code ClientModel}, not {@code null}
     * @param cacheExpiryTimeInSec when a cache expires in sec
     */
    void setCacheExpiryTimeToClientMetadata(ClientModel clientModel, int cacheExpiryTimeInSec);

    /**
     * Returns if fetching a client metadata to newly create it is needed, or re-fetching a client metadata to update it is needed,
     * or re-fetching is not needed because a client metadata does not expire.
     * @param clientId {@code client_id} parameter of an authorization request, not {@code null}
     * @return {@link FetchOperation}
     */
    FetchOperation determineFetchOperation(String clientId);

    /**
     * Creates a client metadata.
     * @param clientOIDCWithCacheControl a combination of a fetched client metadata and Cache-Control header accompanied by it, not {@code null}
     * @return {@link ClientModel} a created client metadata in {@link ClientModel}
     * @throws ClientPolicyException when creating a client metadata fails
     */
    ClientModel createClientMetadata(OIDCClientRepresentationWithCacheControl clientOIDCWithCacheControl) throws ClientPolicyException;

    /**
     * Updates a client metadata.
     * @param clientOIDCWithCacheControl a combination of a re-fetched client metadata and Cache-Control header accompanied by it, not {@code null}
     * @return {@link ClientModel} an updated client metadata in {@link ClientModel}
     * @throws ClientPolicyException when updating a client metadata fails
     */
    ClientModel updateClientMetadata(OIDCClientRepresentationWithCacheControl clientOIDCWithCacheControl) throws ClientPolicyException;

    /**
     * Augments a client metadata.
     * @param clientRep a client metadata in {@link ClientRepresentation}, not {@code null}
     */
    void augmentClientMetadata(ClientRepresentation clientRep);

    @Override
    default void close() {
    }
}
