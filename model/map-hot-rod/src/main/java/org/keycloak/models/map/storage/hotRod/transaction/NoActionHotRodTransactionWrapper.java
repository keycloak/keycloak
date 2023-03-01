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

package org.keycloak.models.map.storage.hotRod.transaction;

import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.QueryParameters;
import org.keycloak.models.map.storage.chm.ConcurrentHashMapKeycloakTransaction;

import java.util.stream.Stream;

/**
 * This is used to return ConcurrentHashMapTransaction (used for operating
 * RemoteCache) functionality to providers but not enlist actualTx the way
 * we need: in prepare phase.
 */
public class NoActionHotRodTransactionWrapper<K, V extends AbstractEntity & UpdatableEntity, M> implements MapKeycloakTransaction<V, M> {


    private final ConcurrentHashMapKeycloakTransaction<K, V, M> actualTx;

    public NoActionHotRodTransactionWrapper(ConcurrentHashMapKeycloakTransaction<K, V, M> actualTx) {
        this.actualTx = actualTx;
    }

    @Override
    public V create(V value) {
        return actualTx.create(value);
    }

    @Override
    public V read(String key) {
        return actualTx.read(key);
    }

    @Override
    public Stream<V> read(QueryParameters<M> queryParameters) {
        return actualTx.read(queryParameters);
    }

    @Override
    public long getCount(QueryParameters<M> queryParameters) {
        return actualTx.getCount(queryParameters);
    }

    @Override
    public boolean delete(String key) {
        return actualTx.delete(key);
    }

    @Override
    public long delete(QueryParameters<M> queryParameters) {
        return actualTx.delete(queryParameters);
    }

    @Override
    public void begin() {
        // Does nothing
    }

    @Override
    public void commit() {
        // Does nothing
    }

    @Override
    public void rollback() {
        // Does nothing
    }

    @Override
    public void setRollbackOnly() {
        actualTx.setRollbackOnly();
    }

    @Override
    public boolean getRollbackOnly() {
        return actualTx.getRollbackOnly();
    }

    @Override
    public boolean isActive() {
        return actualTx.isActive();
    }
}
