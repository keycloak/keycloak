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

package org.keycloak.models.map.storage.hotRod;

import org.infinispan.client.hotrod.RemoteCache;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.SingleUseObjectValueModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.StringKeyConverter;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.MapStorageProvider;
import org.keycloak.models.map.storage.MapStorageProviderFactory;
import org.keycloak.models.map.storage.chm.ConcurrentHashMapStorage;
import org.keycloak.models.map.storage.chm.MapFieldPredicates;
import org.keycloak.models.map.storage.chm.MapModelCriteriaBuilder;
import org.keycloak.models.map.storage.chm.SingleUseObjectMapStorage;
import org.keycloak.models.map.storage.hotRod.common.AbstractHotRodEntity;
import org.keycloak.models.map.storage.hotRod.common.HotRodEntityDelegate;
import org.keycloak.models.map.storage.hotRod.common.HotRodEntityDescriptor;
import org.keycloak.models.map.storage.hotRod.connections.HotRodConnectionProvider;
import org.keycloak.models.map.storage.hotRod.transaction.AllAreasHotRodStoresWrapper;
import org.keycloak.models.map.storage.hotRod.transaction.HotRodRemoteTransactionWrapper;
import org.keycloak.models.map.storage.hotRod.userSession.HotRodUserSessionMapStorage;
import org.keycloak.storage.SearchableModelField;

import java.util.Map;

import static org.keycloak.models.map.storage.hotRod.HotRodMapStorageProviderFactory.CLIENT_SESSION_PREDICATES;
import static org.keycloak.models.map.storage.hotRod.HotRodMapStorageProviderFactory.CLONER;

public class HotRodMapStorageProvider implements MapStorageProvider {

    private final KeycloakSession session;
    private final HotRodMapStorageProviderFactory factory;
    private final boolean jtaEnabled;
    private final long lockTimeout;
    private AllAreasHotRodStoresWrapper storesWrapper;


    public HotRodMapStorageProvider(KeycloakSession session, HotRodMapStorageProviderFactory factory, boolean jtaEnabled, long lockTimeout) {
        this.session = session;
        this.factory = factory;
        this.jtaEnabled = jtaEnabled;
        this.lockTimeout = lockTimeout;
    }

    @Override
    public <V extends AbstractEntity, M> MapStorage<V, M> getMapStorage(Class<M> modelType, MapStorageProviderFactory.Flag... flags) {
        if (storesWrapper == null) initializeTransactionWrapper(modelType);

        // We need to preload client session store before we load user session store to avoid recursive update of storages map
        if (modelType == UserSessionModel.class) getMapStorage(AuthenticatedClientSessionModel.class, flags);

        return (MapStorage<V, M>) storesWrapper.getOrCreateStoreForModel(modelType, () -> createHotRodMapStorage(session, modelType, flags));
    }

    private void initializeTransactionWrapper(Class<?> modelType) {
        storesWrapper = new AllAreasHotRodStoresWrapper();

        // Enlist the wrapper into prepare phase so the changes in the wrapper are executed before HotRod client provided transaction
        session.getTransactionManager().enlistPrepare(storesWrapper);

        // If JTA is enabled, the HotRod client provided transaction is automatically enlisted into JTA and we don't need to do anything here
        if (!jtaEnabled) {
            // If there is no JTA transaction enabled control HotRod client provided transaction manually using
            //  HotRodRemoteTransactionWrapper
            HotRodConnectionProvider connectionProvider = session.getProvider(HotRodConnectionProvider.class);
            HotRodEntityDescriptor<?, ?> entityDescriptor = factory.getEntityDescriptor(modelType);
            RemoteCache<Object, Object> remoteCache = connectionProvider.getRemoteCache(entityDescriptor.getCacheName());
            session.getTransactionManager().enlist(new HotRodRemoteTransactionWrapper(remoteCache.getTransactionManager()));
        }
    }

    private <K, E extends AbstractHotRodEntity, V extends HotRodEntityDelegate<E> & AbstractEntity, M> ConcurrentHashMapStorage<K, V, M, ?> createHotRodMapStorage(KeycloakSession session, Class<M> modelType, MapStorageProviderFactory.Flag... flags) {
        HotRodConnectionProvider connectionProvider = session.getProvider(HotRodConnectionProvider.class);
        HotRodEntityDescriptor<E, V> entityDescriptor = (HotRodEntityDescriptor<E, V>) factory.getEntityDescriptor(modelType);
        Map<SearchableModelField<? super M>, MapModelCriteriaBuilder.UpdatePredicatesFunc<String, V, M>> fieldPredicates = MapFieldPredicates.getPredicates((Class<M>) entityDescriptor.getModelTypeClass());
        StringKeyConverter<String> kc = StringKeyConverter.StringKey.INSTANCE;

        // TODO: This is messy, we should refactor this so we don't need to pass kc, entityDescriptor, CLONER to both MapStorage and CrudOperations
        if (modelType == SingleUseObjectValueModel.class) {
            return new SingleUseObjectMapStorage(new SingleUseObjectHotRodCrudOperations(session, connectionProvider.getRemoteCache(entityDescriptor.getCacheName()), kc, (HotRodEntityDescriptor) entityDescriptor, CLONER, lockTimeout), kc, CLONER, fieldPredicates);
        } if (modelType == AuthenticatedClientSessionModel.class) {
            return new ConcurrentHashMapStorage(new HotRodCrudOperations(session, connectionProvider.getRemoteCache(entityDescriptor.getCacheName()),
                    kc,
                    entityDescriptor,
                    CLONER, lockTimeout), kc, CLONER, CLIENT_SESSION_PREDICATES);
        } if (modelType == UserSessionModel.class) {
            return new HotRodUserSessionMapStorage(new HotRodCrudOperations(session, connectionProvider.getRemoteCache(entityDescriptor.getCacheName()),
                    kc,
                    entityDescriptor,
                    CLONER, lockTimeout), kc, CLONER, fieldPredicates, storesWrapper.getOrCreateStoreForModel(AuthenticatedClientSessionModel.class, () -> createHotRodMapStorage(session, AuthenticatedClientSessionModel.class, flags)));
        } else {
            return new ConcurrentHashMapStorage(new HotRodCrudOperations<>(session, connectionProvider.getRemoteCache(entityDescriptor.getCacheName()), kc, entityDescriptor, CLONER, lockTimeout), kc, CLONER, fieldPredicates);
        }
    }

    @Override
    public void close() {

    }
}
