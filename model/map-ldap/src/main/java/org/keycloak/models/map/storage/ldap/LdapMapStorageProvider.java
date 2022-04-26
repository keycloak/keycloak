/*
 * Copyright 2022. Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.ldap;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.MapStorageProvider;
import org.keycloak.models.map.storage.MapStorageProviderFactory.Flag;

public class LdapMapStorageProvider implements MapStorageProvider {

    private final LdapMapStorageProviderFactory factory;
    private final String sessionTxPrefix;

    public LdapMapStorageProvider(LdapMapStorageProviderFactory factory, String sessionTxPrefix) {
        this.factory = factory;
        this.sessionTxPrefix = sessionTxPrefix;
    }

    @Override
    public void close() {
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V extends AbstractEntity, M> MapStorage<V, M> getStorage(Class<M> modelType, Flag... flags) {
        // MapStorage is not a functional interface, therefore don't try to convert it to a lambda as additional methods might be added in the future
        //noinspection Convert2Lambda
        return new MapStorage<V, M>() {
            @Override
            public MapKeycloakTransaction<V, M> createTransaction(KeycloakSession session) {
                MapKeycloakTransaction<V, M> sessionTx = session.getAttribute(sessionTxPrefix + modelType.hashCode(), MapKeycloakTransaction.class);
                if (sessionTx == null) {
                    sessionTx = factory.createTransaction(session, modelType);
                    session.setAttribute(sessionTxPrefix + modelType.hashCode(), sessionTx);
                }
                return sessionTx;
            }
        };
    }

}
