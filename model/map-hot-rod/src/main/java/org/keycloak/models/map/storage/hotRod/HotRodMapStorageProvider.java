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
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.MapStorageProvider;
import org.keycloak.models.map.storage.MapStorageProviderFactory;
import org.keycloak.models.map.storage.hotRod.common.HotRodEntityDescriptor;
import org.keycloak.models.map.storage.hotRod.connections.HotRodConnectionProvider;
import org.keycloak.models.map.storage.hotRod.transaction.HotRodRemoteTransactionWrapper;
import org.keycloak.models.map.storage.hotRod.transaction.AllAreasHotRodTransactionsWrapper;

public class HotRodMapStorageProvider implements MapStorageProvider {

    private final KeycloakSession session;
    private final HotRodMapStorageProviderFactory factory;
    private final String hotRodConfigurationIdentifier;
    private final boolean jtaEnabled;

    public HotRodMapStorageProvider(KeycloakSession session, HotRodMapStorageProviderFactory factory, String hotRodConfigurationIdentifier, boolean jtaEnabled) {
        this.session = session;
        this.factory = factory;
        this.hotRodConfigurationIdentifier = hotRodConfigurationIdentifier;
        this.jtaEnabled = jtaEnabled;
    }

    @Override
    public <V extends AbstractEntity, M> MapStorage<V, M> getStorage(Class<M> modelType, MapStorageProviderFactory.Flag... flags) {
        // Check if HotRod transaction was already initialized for this configuration within this session
        AllAreasHotRodTransactionsWrapper txWrapper = session.getAttribute(this.hotRodConfigurationIdentifier, AllAreasHotRodTransactionsWrapper.class);
        if (txWrapper == null) {
            // If not create new AllAreasHotRodTransactionsWrapper and put it into session, so it is created only once
            txWrapper = new AllAreasHotRodTransactionsWrapper();
            session.setAttribute(this.hotRodConfigurationIdentifier, txWrapper);

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

        return (MapStorage<V, M>) factory.getHotRodStorage(session, modelType, txWrapper, flags);
    }

    @Override
    public void close() {

    }
}
