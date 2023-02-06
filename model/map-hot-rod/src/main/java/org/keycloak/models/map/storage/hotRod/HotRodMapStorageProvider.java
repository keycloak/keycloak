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
import org.keycloak.models.map.common.SessionAttributesUtils;
import org.keycloak.models.map.common.StringKeyConverter;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.MapStorageProvider;
import org.keycloak.models.map.storage.MapStorageProviderFactory;
import org.keycloak.models.map.storage.chm.ConcurrentHashMapKeycloakTransaction;
import org.keycloak.models.map.storage.chm.MapFieldPredicates;
import org.keycloak.models.map.storage.chm.MapModelCriteriaBuilder;
import org.keycloak.models.map.storage.hotRod.common.AbstractHotRodEntity;
import org.keycloak.models.map.storage.hotRod.common.HotRodEntityDelegate;
import org.keycloak.models.map.storage.hotRod.common.HotRodEntityDescriptor;
import org.keycloak.models.map.storage.hotRod.connections.HotRodConnectionProvider;
import org.keycloak.models.map.storage.hotRod.singleUseObject.HotRodSingleUseObjectEntity;
import org.keycloak.models.map.storage.hotRod.singleUseObject.HotRodSingleUseObjectEntityDelegate;
import org.keycloak.models.map.storage.hotRod.transaction.AllAreasHotRodTransactionsWrapper;
import org.keycloak.models.map.storage.hotRod.transaction.HotRodRemoteTransactionWrapper;
import org.keycloak.models.map.storage.hotRod.userSession.HotRodUserSessionTransaction;
import org.keycloak.models.map.userSession.MapUserSessionEntity;
import org.keycloak.storage.SearchableModelField;

import java.util.Map;

import static org.keycloak.models.map.storage.hotRod.HotRodMapStorageProviderFactory.CLIENT_SESSION_PREDICATES;
import static org.keycloak.models.map.storage.hotRod.HotRodMapStorageProviderFactory.CLONER;

public class HotRodMapStorageProvider implements MapStorageProvider {

    private final KeycloakSession session;
    private final HotRodMapStorageProviderFactory factory;
    private final int factoryId;
    private final boolean jtaEnabled;
    private final long lockTimeout;
    private AllAreasHotRodTransactionsWrapper txWrapper;
    

    public HotRodMapStorageProvider(KeycloakSession session, HotRodMapStorageProviderFactory factory, int factoryId, boolean jtaEnabled, long lockTimeout) {
        this.session = session;
        this.factory = factory;
        this.jtaEnabled = jtaEnabled;
        this.factoryId = factoryId;
        this.lockTimeout = lockTimeout;
    }

    @Override
    public <V extends AbstractEntity, M> MapKeycloakTransaction<V, M> getEnlistedTransaction(Class<M> modelType, MapStorageProviderFactory.Flag... flags) {
        if (txWrapper == null) initializeTransactionWrapper(modelType);

        // We need to preload client session store before we load user session store to avoid recursive update of storages map
        if (modelType == UserSessionModel.class) getEnlistedTransaction(AuthenticatedClientSessionModel.class, flags);

        return (MapKeycloakTransaction<V, M>) SessionAttributesUtils.getOrCreateTransaction(session, getClass(), modelType, factoryId, () -> getHotRodTransaction(session, modelType, flags));
    }
    
    private void initializeTransactionWrapper(Class<?> modelType) {
        txWrapper = new AllAreasHotRodTransactionsWrapper();

        // Enlist the wrapper into prepare phase so it is executed before HotRod client provided transaction
        session.getTransactionManager().enlistPrepare(txWrapper);

        if (!jtaEnabled) {
            // If there is no JTA transaction enabled control HotRod client provided transaction manually using
            //  HotRodRemoteTransactionWrapper
            HotRodConnectionProvider connectionProvider = session.getProvider(HotRodConnectionProvider.class);
            HotRodEntityDescriptor<?, ?> entityDescriptor = factory.getEntityDescriptor(modelType);
            RemoteCache<Object, Object> remoteCache = connectionProvider.getRemoteCache(entityDescriptor.getCacheName());
            session.getTransactionManager().enlist(new HotRodRemoteTransactionWrapper(remoteCache.getTransactionManager()));
        }
    }

    public <E extends AbstractHotRodEntity, V extends HotRodEntityDelegate<E> & AbstractEntity, M> MapKeycloakTransaction<V, M> getHotRodTransaction(KeycloakSession session, Class<M> modelType, MapStorageProviderFactory.Flag... flags) {
        HotRodMapStorage<String, E, V, M> hotRodStorage = createHotRodStorage(session, modelType, flags);
        MapKeycloakTransaction<V, M> transaction = hotRodStorage.createTransaction(session);
        return transaction;
    }

    private <E extends AbstractHotRodEntity, V extends HotRodEntityDelegate<E> & AbstractEntity, M> HotRodMapStorage<String, E, V, M> createHotRodStorage(KeycloakSession session, Class<M> modelType, MapStorageProviderFactory.Flag... flags) {
        HotRodConnectionProvider connectionProvider = session.getProvider(HotRodConnectionProvider.class);
        HotRodEntityDescriptor<E, V> entityDescriptor = (HotRodEntityDescriptor<E, V>) factory.getEntityDescriptor(modelType);

        if (modelType == SingleUseObjectValueModel.class) {
            return (HotRodMapStorage) new SingleUseObjectHotRodMapStorage(session, connectionProvider.getRemoteCache(entityDescriptor.getCacheName()), StringKeyConverter.StringKey.INSTANCE, (HotRodEntityDescriptor) entityDescriptor, CLONER, txWrapper, lockTimeout);
        } if (modelType == AuthenticatedClientSessionModel.class) {
            return new HotRodMapStorage(session, connectionProvider.getRemoteCache(entityDescriptor.getCacheName()),
                    StringKeyConverter.StringKey.INSTANCE,
                    entityDescriptor,
                    CLONER, txWrapper, lockTimeout) {
                @Override
                public MapKeycloakTransaction createTransaction(KeycloakSession session) {
                    return new ConcurrentHashMapKeycloakTransaction(this, keyConverter, cloner, CLIENT_SESSION_PREDICATES);
                }
            };
        } if (modelType == UserSessionModel.class) {
            return new HotRodMapStorage(session, connectionProvider.getRemoteCache(entityDescriptor.getCacheName()),
                    StringKeyConverter.StringKey.INSTANCE,
                    entityDescriptor,
                    CLONER, txWrapper, lockTimeout) {
                @Override
                public MapKeycloakTransaction createTransaction(KeycloakSession session) {
                    Map<SearchableModelField<? super UserSessionModel>, MapModelCriteriaBuilder.UpdatePredicatesFunc<String, MapUserSessionEntity, UserSessionModel>> fieldPredicates = MapFieldPredicates.getPredicates((Class<UserSessionModel>) storedEntityDescriptor.getModelTypeClass());
                    return new HotRodUserSessionTransaction(this, keyConverter, cloner, fieldPredicates, getHotRodTransaction(session, AuthenticatedClientSessionModel.class));
                }
            };
        }
        return new HotRodMapStorage<>(session, connectionProvider.getRemoteCache(entityDescriptor.getCacheName()), StringKeyConverter.StringKey.INSTANCE, entityDescriptor, CLONER, txWrapper, lockTimeout);
    }

    @Override
    public void close() {

    }
}
