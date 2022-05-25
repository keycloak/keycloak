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

package org.keycloak.models.map.storage.hotRod.userSession;

import org.infinispan.client.hotrod.RemoteCache;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.StringKeyConverter;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.chm.MapFieldPredicates;
import org.keycloak.models.map.storage.chm.MapModelCriteriaBuilder;
import org.keycloak.models.map.storage.chm.UserSessionCascadeRemovalTransaction;
import org.keycloak.models.map.storage.hotRod.HotRodMapStorage;
import org.keycloak.models.map.storage.hotRod.common.HotRodEntityDescriptor;
import org.keycloak.storage.SearchableModelField;

import java.util.Map;

public class HotRodUserSessionMapStorage<K> extends HotRodMapStorage<K, HotRodUserSessionEntity, HotRodUserSessionEntityDelegate, UserSessionModel> {

    private final HotRodMapStorage<K, HotRodAuthenticatedClientSessionEntity, HotRodAuthenticatedClientSessionEntityDelegate, AuthenticatedClientSessionModel> hotRodClientSessionStore;

    public HotRodUserSessionMapStorage(HotRodMapStorage<K, HotRodAuthenticatedClientSessionEntity, HotRodAuthenticatedClientSessionEntityDelegate, AuthenticatedClientSessionModel> hotRodClientSessionStore,
                                       RemoteCache<K, HotRodUserSessionEntity> remoteCache,
                                       StringKeyConverter<K> keyConverter,
                                       HotRodEntityDescriptor<HotRodUserSessionEntity, HotRodUserSessionEntityDelegate> storedEntityDescriptor,
                                       DeepCloner cloner
                                       ) {
        super(remoteCache, keyConverter, storedEntityDescriptor, cloner);
        this.hotRodClientSessionStore = hotRodClientSessionStore;
    }

    @Override
    public MapKeycloakTransaction<HotRodUserSessionEntityDelegate, UserSessionModel> createTransaction(KeycloakSession session) {
        MapKeycloakTransaction<HotRodUserSessionEntityDelegate, UserSessionModel> sessionTransaction = session.getAttribute("map-transaction-" + hashCode(), MapKeycloakTransaction.class);

        if (sessionTransaction == null) {
            Map<SearchableModelField<? super UserSessionModel>, MapModelCriteriaBuilder.UpdatePredicatesFunc<K, HotRodUserSessionEntityDelegate, UserSessionModel>> fieldPredicates = MapFieldPredicates.getPredicates((Class<UserSessionModel>) storedEntityDescriptor.getModelTypeClass());
            sessionTransaction = new UserSessionCascadeRemovalTransaction<>(this, hotRodClientSessionStore.createTransaction(session), keyConverter, cloner, fieldPredicates);
            session.setAttribute("map-transaction-" + hashCode(), sessionTransaction);
        }

        return sessionTransaction;
    }
}
