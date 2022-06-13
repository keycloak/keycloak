/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
