/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.chm;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.ModelCriteriaBuilder;
import org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator;
import org.keycloak.models.map.storage.QueryParameters;
import org.keycloak.models.map.storage.StringKeyConvertor;
import org.keycloak.models.map.userSession.MapAuthenticatedClientSessionEntity;
import org.keycloak.models.map.userSession.MapUserSessionEntity;
import java.util.Set;
import java.util.stream.Collectors;

import static org.keycloak.models.map.storage.QueryParameters.withCriteria;

/**
 * User session storage with a naive implementation of referential integrity in client to user session relation, restricted to
 * ON DELETE CASCADE functionality.
 *
 * @author hmlnarik
 */
public class UserSessionConcurrentHashMapStorage<K> extends ConcurrentHashMapStorage<K, MapUserSessionEntity<K>, UserSessionModel> {

    private final ConcurrentHashMapStorage<K, MapAuthenticatedClientSessionEntity<K>, AuthenticatedClientSessionModel> clientSessionStore;

    private class Transaction extends ConcurrentHashMapKeycloakTransaction<K, MapUserSessionEntity<K>, UserSessionModel> {

        private final MapKeycloakTransaction<K, MapAuthenticatedClientSessionEntity<K>, AuthenticatedClientSessionModel> clientSessionTr;

        public Transaction(MapKeycloakTransaction<K, MapAuthenticatedClientSessionEntity<K>, AuthenticatedClientSessionModel> clientSessionTr) {
            super(UserSessionConcurrentHashMapStorage.this);
            this.clientSessionTr = clientSessionTr;
        }

        @Override
        public long delete(K artificialKey, QueryParameters<UserSessionModel> queryParameters) {
            ModelCriteriaBuilder<UserSessionModel> mcb = queryParameters.getModelCriteriaBuilder();

            Set<K> ids = read(queryParameters).map(AbstractEntity::getId).collect(Collectors.toSet());
            ModelCriteriaBuilder<AuthenticatedClientSessionModel> csMcb = clientSessionStore.createCriteriaBuilder().compare(AuthenticatedClientSessionModel.SearchableFields.USER_SESSION_ID, Operator.IN, ids);
            clientSessionTr.delete(artificialKey, withCriteria(csMcb));
            return super.delete(artificialKey, queryParameters);
        }

        @Override
        public void delete(K key) {
            ModelCriteriaBuilder<AuthenticatedClientSessionModel> csMcb = clientSessionStore.createCriteriaBuilder().compare(AuthenticatedClientSessionModel.SearchableFields.USER_SESSION_ID, Operator.EQ, key);
            clientSessionTr.delete(key, withCriteria(csMcb));
            super.delete(key);
        }

    }

    @SuppressWarnings("unchecked")
    public UserSessionConcurrentHashMapStorage(ConcurrentHashMapStorage<K, MapAuthenticatedClientSessionEntity<K>, AuthenticatedClientSessionModel> clientSessionStore,
      StringKeyConvertor<K> keyConvertor) {
        super(UserSessionModel.class, keyConvertor);
        this.clientSessionStore = clientSessionStore;
    }

    @Override
    @SuppressWarnings("unchecked")
    public MapKeycloakTransaction<K, MapUserSessionEntity<K>, UserSessionModel> createTransaction(KeycloakSession session) {
        MapKeycloakTransaction<K, MapUserSessionEntity<K>, UserSessionModel> sessionTransaction = session.getAttribute("map-transaction-" + hashCode(), MapKeycloakTransaction.class);
        return sessionTransaction == null ? new Transaction(clientSessionStore.createTransaction(session)) : sessionTransaction;
    }
}
