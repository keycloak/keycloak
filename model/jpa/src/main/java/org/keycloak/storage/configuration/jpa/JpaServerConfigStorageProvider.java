/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.storage.configuration.jpa;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.keycloak.storage.configuration.ServerConfigStorageProvider;
import org.keycloak.storage.configuration.jpa.entity.ServerConfigEntity;

/**
 * A {@link ServerConfigStorageProvider} that stores its data in the database, using the {@link EntityManager}.
 */
public class JpaServerConfigStorageProvider implements ServerConfigStorageProvider {

    private final EntityManager entityManager;

    public JpaServerConfigStorageProvider(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager);
    }

    @Override
    public Optional<String> find(String key) {
        return Optional.ofNullable(getEntity(key, LockModeType.READ))
                .map(ServerConfigEntity::getValue);
    }

    @Override
    public void store(String key, String value) {
        var entity = getEntity(key, LockModeType.WRITE);
        if (entity == null) {
            entity = new ServerConfigEntity();
            entity.setId(Objects.requireNonNull(key));
            entity.setValue(Objects.requireNonNull(value));
            entityManager.persist(entity);
            return;
        }
        entity.setValue(Objects.requireNonNull(value));
        entityManager.merge(entity);
    }

    @Override
    public void remove(String key) {
        var entity = getEntity(key, LockModeType.WRITE);
        if (entity != null) {
            entityManager.remove(entity);
        }
    }

    @Override
    public String loadOrCreate(String key, Supplier<String> valueGenerator) {
        var entity = getEntity(key, LockModeType.WRITE);
        if (entity != null) {
            return entity.getValue();
        }
        var value = Objects.requireNonNull(valueGenerator.get());
        entity = new ServerConfigEntity();
        entity.setId(Objects.requireNonNull(key));
        entity.setValue(value);
        entityManager.persist(entity);
        return value;
    }

    @Override
    public void close() {
        //no-op
    }

    private ServerConfigEntity getEntity(String key, LockModeType lockModeType) {
        return entityManager.find(ServerConfigEntity.class, Objects.requireNonNull(key), lockModeType);
    }
}
