package org.keycloak.models.map.storage;

import org.keycloak.credential.CredentialModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.UpdatableEntity;

/**
 * Implementing this interface signals that the store can validate credentials.
 * This will be implemented, for example, by a store that supports SPNEGO for Kerberos authentication.
 *
 * @author Alexander Schwartz
 */
public interface MapStorageWithAuth<V extends AbstractEntity & UpdatableEntity, M> extends MapStorage<V, M> {

    /**
     * Determine which credential types a store supports.
     * This method should be a cheap way to query the store before creating a more expensive transaction and performing an authentication.
     *
     * @param type supported credential type by this store, for example {@link CredentialModel#KERBEROS}.
     * @return <code>true</code> if the credential type is supported by this storage
     */
    boolean supportsCredentialType(String type);

    @Override
    MapKeycloakTransactionWithAuth<V, M> createTransaction(KeycloakSession session);
}
