/*
 * Copyright 2022. Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.ldap;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.QueryParameters;

public abstract class LdapMapKeycloakTransaction<RE, E extends AbstractEntity & UpdatableEntity, M> implements MapKeycloakTransaction<E, M> {

    private boolean active;
    private boolean rollback;

    public LdapMapKeycloakTransaction() {
    }

    protected abstract static class MapTaskWithValue {
        public abstract void execute();
    }

    protected abstract static class DeleteOperation extends MapTaskWithValue {
    }

    protected final LinkedList<MapTaskWithValue> tasksOnRollback = new LinkedList<>();

    protected final LinkedList<MapTaskWithValue> tasksOnCommit = new LinkedList<>();

    protected final Map<String, RE> entities = new HashMap<>();

    public long getCount(QueryParameters<M> queryParameters) {
        return read(queryParameters).count();
    }

    public long delete(QueryParameters<M> queryParameters) {
        return read(queryParameters).map(m -> delete(m.getId()) ? 1 : 0).collect(Collectors.summarizingLong(val -> val)).getSum();
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
