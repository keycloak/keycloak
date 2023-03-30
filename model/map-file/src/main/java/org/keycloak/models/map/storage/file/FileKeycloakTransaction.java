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
package org.keycloak.models.map.storage.file;

import java.util.stream.Stream;

import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.QueryParameters;

/**
 * {@link MapKeycloakTransaction} implementation used with the file map storage.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class FileKeycloakTransaction<V extends AbstractEntity, M> implements MapKeycloakTransaction<V, M> {

    private boolean active;
    private boolean rollback;

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
        return null;
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
        active = true;
    }

    @Override
    public void commit() {
        if (rollback) {
            throw new RuntimeException("Rollback only!");
        }
    }

    @Override
    public void rollback() {
    }

    @Override
    public void setRollbackOnly() {
        rollback = true;
    }

    @Override
    public boolean getRollbackOnly() {
        return rollback;
    }

    @Override
    public boolean isActive() {
        return active;
    }
}
