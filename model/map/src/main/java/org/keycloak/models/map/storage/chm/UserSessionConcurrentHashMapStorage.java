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

import org.keycloak.models.map.common.StringKeyConverter;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.userSession.MapAuthenticatedClientSessionEntity;
import org.keycloak.models.map.userSession.MapUserSessionEntity;

/**
 * User session storage with a naive implementation of referential integrity in client to user session relation, restricted to
 * ON DELETE CASCADE functionality.
 *
 * @author hmlnarik
 */
public class UserSessionConcurrentHashMapStorage<K> extends ConcurrentHashMapStorage<K, MapUserSessionEntity, UserSessionModel> {

    private final ConcurrentHashMapStorage<K, MapAuthenticatedClientSessionEntity, AuthenticatedClientSessionModel> clientSessionStore;

    @SuppressWarnings("unchecked")
    public UserSessionConcurrentHashMapStorage(ConcurrentHashMapStorage<K, MapAuthenticatedClientSessionEntity, AuthenticatedClientSessionModel> clientSessionStore,
                                               StringKeyConverter<K> keyConverter, DeepCloner cloner) {
        super(UserSessionModel.class, keyConverter, cloner);
        this.clientSessionStore = clientSessionStore;
    }

    @Override
    @SuppressWarnings("unchecked")
    public MapKeycloakTransaction<MapUserSessionEntity, UserSessionModel> createTransaction(KeycloakSession session) {
        MapKeycloakTransaction<MapUserSessionEntity, UserSessionModel> sessionTransaction = session.getAttribute("map-transaction-" + hashCode(), MapKeycloakTransaction.class);

        if (sessionTransaction == null) {
            sessionTransaction = new UserSessionCascadeRemovalTransaction<>(this, clientSessionStore.createTransaction(session), clientSessionStore.getKeyConverter(), cloner, fieldPredicates);
            session.setAttribute("map-transaction-" + hashCode(), sessionTransaction);
        }
        return sessionTransaction;
    }
}
