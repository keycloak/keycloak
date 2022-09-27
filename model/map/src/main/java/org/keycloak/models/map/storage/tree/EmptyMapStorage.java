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
package org.keycloak.models.map.storage.tree;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.QueryParameters;
import java.util.stream.Stream;

/**
 *
 * @author hmlnarik
 */
public class EmptyMapStorage<V extends AbstractEntity, M> implements MapStorage<V, M> {

    private static final EmptyMapStorage<?, ?> INSTANCE = new EmptyMapStorage<>();

    @SuppressWarnings("unchecked")
    public static <V extends AbstractEntity, M> EmptyMapStorage<V, M> getInstance() {
        return (EmptyMapStorage<V, M>) INSTANCE;
    }

    @Override
    public MapKeycloakTransaction<V, M> createTransaction(KeycloakSession session) {
        return new MapKeycloakTransaction<V, M>() {
            @Override
            public V create(V value) {
                return null;
            }

            @Override
            public V read(String key) {
                return null;
            }

            @Override
            public Stream<V> read(QueryParameters<M> queryParameters) {
                return Stream.empty();
            }

            @Override
            public long getCount(QueryParameters<M> queryParameters) {
                return 0;
            }

            @Override
            public boolean delete(String key) {
                return false;
            }

            @Override
            public long delete(QueryParameters<M> queryParameters) {
                return 0;
            }

            @Override
            public void begin() {
            }

            @Override
            public void commit() {
            }

            @Override
            public void rollback() {
            }

            @Override
            public void setRollbackOnly() {
            }

            @Override
            public boolean getRollbackOnly() {
                return false;
            }

            @Override
            public boolean isActive() {
                return true;
            }
        };
    }

}
