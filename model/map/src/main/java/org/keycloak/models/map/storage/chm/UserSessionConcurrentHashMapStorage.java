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

import org.keycloak.models.map.common.StringKeyConvertor;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator;
import org.keycloak.models.map.storage.QueryParameters;
import org.keycloak.models.map.storage.chm.MapModelCriteriaBuilder.UpdatePredicatesFunc;
import org.keycloak.models.map.storage.criteria.DefaultModelCriteria;
import org.keycloak.models.map.userSession.MapAuthenticatedClientSessionEntity;
import org.keycloak.models.map.userSession.MapUserSessionEntity;
import org.keycloak.storage.SearchableModelField;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.keycloak.models.map.storage.QueryParameters.withCriteria;

/**
 * User session storage with a naive implementation of referential integrity in client to user session relation, restricted to
 * ON DELETE CASCADE functionality.
 *
 * @author hmlnarik
 */
public class UserSessionConcurrentHashMapStorage<K> extends ConcurrentHashMapStorage<K, MapUserSessionEntity, UserSessionModel> {

    private final ConcurrentHashMapStorage<K, MapAuthenticatedClientSessionEntity, AuthenticatedClientSessionModel> clientSessionStore;

    private class Transaction extends ConcurrentHashMapKeycloakTransaction<K, MapUserSessionEntity, UserSessionModel> {

        private final MapKeycloakTransaction<MapAuthenticatedClientSessionEntity, AuthenticatedClientSessionModel> clientSessionTr;

        public Transaction(MapKeycloakTransaction<MapAuthenticatedClientSessionEntity, AuthenticatedClientSessionModel> clientSessionTr,
                           StringKeyConvertor<K> keyConvertor,
                           DeepCloner cloner,
                           Map<SearchableModelField<? super UserSessionModel>,
                               UpdatePredicatesFunc<K,
                                       MapUserSessionEntity,
                                       UserSessionModel>> fieldPredicates) {
            super(UserSessionConcurrentHashMapStorage.this, keyConvertor, cloner, fieldPredicates);
            this.clientSessionTr = clientSessionTr;
        }

        @Override
        public long delete(QueryParameters<UserSessionModel> queryParameters) {
            Set<String> ids = read(queryParameters).map(AbstractEntity::getId).collect(Collectors.toSet());
            DefaultModelCriteria<AuthenticatedClientSessionModel> csMcb = DefaultModelCriteria.<AuthenticatedClientSessionModel>criteria()
                    .compare(AuthenticatedClientSessionModel.SearchableFields.USER_SESSION_ID, Operator.IN, ids);
            clientSessionTr.delete(withCriteria(csMcb));
            return super.delete(queryParameters);
        }

        @Override
        public boolean delete(String key) {
            DefaultModelCriteria<AuthenticatedClientSessionModel> csMcb = DefaultModelCriteria.<AuthenticatedClientSessionModel>criteria()
                    .compare(AuthenticatedClientSessionModel.SearchableFields.USER_SESSION_ID, Operator.EQ, key);
            clientSessionTr.delete(withCriteria(csMcb));
            return super.delete(key);
        }

    }

    @SuppressWarnings("unchecked")
    public UserSessionConcurrentHashMapStorage(ConcurrentHashMapStorage<K, MapAuthenticatedClientSessionEntity, AuthenticatedClientSessionModel> clientSessionStore,
      StringKeyConvertor<K> keyConvertor, DeepCloner cloner) {
        super(UserSessionModel.class, keyConvertor, cloner);
        this.clientSessionStore = clientSessionStore;
    }

    @Override
    @SuppressWarnings("unchecked")
    public MapKeycloakTransaction<MapUserSessionEntity, UserSessionModel> createTransaction(KeycloakSession session) {
        MapKeycloakTransaction<MapUserSessionEntity, UserSessionModel> sessionTransaction = session.getAttribute("map-transaction-" + hashCode(), MapKeycloakTransaction.class);
        return sessionTransaction == null ? new Transaction(clientSessionStore.createTransaction(session), clientSessionStore.getKeyConvertor(), cloner, fieldPredicates) : sessionTransaction;
    }
}
