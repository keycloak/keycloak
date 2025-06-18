/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.sessions.infinispan.changes.remote.remover.query;

import java.util.Map;
import java.util.Objects;

import org.keycloak.models.sessions.infinispan.changes.remote.remover.ConditionalRemover;
import org.keycloak.models.sessions.infinispan.entities.ClientSessionKey;
import org.keycloak.models.sessions.infinispan.entities.RemoteAuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.query.ClientSessionQueries;

/**
 * A {@link ConditionalRemover} implementation to remove {@link RemoteAuthenticatedClientSessionEntity} based on some
 * filters over its state.
 * <p>
 * This implementation uses Infinispan Ickle Queries to perform the removal operation. Indexing is not required.
 */
public class ClientSessionQueryConditionalRemover extends MultipleConditionQueryRemover<ClientSessionKey, RemoteAuthenticatedClientSessionEntity> {

    public ClientSessionQueryConditionalRemover() {
        super();
    }

    @Override
    String getEntity() {
        return ClientSessionQueries.CLIENT_SESSION;
    }

    public void removeByUserSessionId(String userSessionId) {
        add(new RemoveByUserSession(nextParameter(), userSessionId));
    }

    public void removeByRealmId(String realmId) {
        add(new RemoveByRealm(nextParameter(), realmId));
    }

    public void removeByUserId(String realmId, String userId) {
        add(new RemoveByUser(nextParameter(), realmId, nextParameter(), userId));
    }

    private record RemoveByUserSession(String userSessionParameter,
                                       String userSessionId) implements RemoveCondition<ClientSessionKey, RemoteAuthenticatedClientSessionEntity> {

        @Override
        public String getConditionalClause() {
            return "(userSessionId = :%s)".formatted(userSessionParameter);
        }

        @Override
        public void addParameters(Map<String, Object> parameters) {
            parameters.put(userSessionParameter, userSessionId);
        }

        @Override
        public boolean willRemove(ClientSessionKey key, RemoteAuthenticatedClientSessionEntity value) {
            return Objects.equals(value.getUserSessionId(), userSessionId);
        }
    }

    private record RemoveByRealm(String realmParameter,
                                 String realmId) implements RemoveCondition<ClientSessionKey, RemoteAuthenticatedClientSessionEntity> {

        @Override
        public String getConditionalClause() {
            return "(realmId = :%s)".formatted(realmParameter);
        }

        @Override
        public void addParameters(Map<String, Object> parameters) {
            parameters.put(realmParameter, realmId);
        }

        @Override
        public boolean willRemove(ClientSessionKey key, RemoteAuthenticatedClientSessionEntity value) {
            return Objects.equals(value.getRealmId(), realmId);
        }
    }

    private record RemoveByUser(String realmParameter, String realmId, String userParameter,
                                String userId) implements RemoveCondition<ClientSessionKey, RemoteAuthenticatedClientSessionEntity> {

        @Override
        public String getConditionalClause() {
            return "(userId = :%s && realmId = :%s)".formatted(userParameter, realmParameter);
        }

        @Override
        public void addParameters(Map<String, Object> parameters) {
            parameters.put(realmParameter, realmId);
            parameters.put(userParameter, userId);
        }

        @Override
        public boolean willRemove(ClientSessionKey key, RemoteAuthenticatedClientSessionEntity value) {
            return Objects.equals(value.getUserId(), userId) && Objects.equals(value.getRealmId(), realmId);
        }
    }
}
