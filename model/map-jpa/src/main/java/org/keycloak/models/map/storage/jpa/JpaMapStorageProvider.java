/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.jpa;

import javax.persistence.EntityManager;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.MapStorageProvider;
import org.keycloak.models.map.storage.MapStorageProviderFactory.Flag;

public class JpaMapStorageProvider implements MapStorageProvider {

    private final JpaMapStorageProviderFactory factory;
    private final KeycloakSession session;
    private final EntityManager em;
    private final String sessionTxKey;

    public JpaMapStorageProvider(JpaMapStorageProviderFactory factory, KeycloakSession session, EntityManager em, String sessionTxKey) {
        this.factory = factory;
        this.session = session;
        this.em = em;
        this.sessionTxKey = sessionTxKey;
    }

    @Override
    public void close() {
        em.close();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V extends AbstractEntity, M> MapStorage<V, M> getStorage(Class<M> modelType, Flag... flags) {
        // validate and update the schema for the storage.
        this.factory.validateAndUpdateSchema(this.session, modelType);
        // create the JPA transaction and enlist it if needed.
        if (session.getAttribute(this.sessionTxKey) == null) {
            KeycloakTransaction jpaTransaction = new JpaTransactionWrapper(em.getTransaction());
            session.getTransactionManager().enlist(jpaTransaction);
            session.setAttribute(this.sessionTxKey, jpaTransaction);
        }
        return new MapStorage<V, M>() {
            @Override
            public MapKeycloakTransaction<V, M> createTransaction(KeycloakSession session) {
                return factory.createTransaction(session, modelType, em);
            }
        };
    }
}
