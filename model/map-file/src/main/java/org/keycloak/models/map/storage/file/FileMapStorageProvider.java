/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.ExpirableEntity;
import org.keycloak.models.map.common.SessionAttributesUtils;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.MapStorageProvider;
import org.keycloak.models.map.storage.MapStorageProviderFactory;
import org.keycloak.models.map.storage.ModelEntityUtil;
import org.keycloak.models.map.storage.chm.ConcurrentHashMapStorage;

import java.util.function.Function;

import static org.keycloak.models.map.storage.ModelEntityUtil.getModelName;
import static org.keycloak.models.map.storage.file.FileMapStorageProviderFactory.UNIQUE_HUMAN_READABLE_NAME_FIELD;

/**
 * File-based {@link MapStorageProvider} implementation.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class FileMapStorageProvider implements MapStorageProvider {

    private final KeycloakSession session;
    private final FileMapStorageProviderFactory factory;
    private final int factoryId;

    public FileMapStorageProvider(KeycloakSession session, FileMapStorageProviderFactory factory, int factoryId) {
        this.session = session;
        this.factory = factory;
        this.factoryId = factoryId;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V extends AbstractEntity, M> MapStorage<V, M> getMapStorage(Class<M> modelType, MapStorageProviderFactory.Flag... flags) {
        return (MapStorage<V, M>) SessionAttributesUtils.createMapStorageIfAbsent(session, getClass(), modelType, factoryId, () -> createFileMapStorage(modelType));
    }

    private <V extends AbstractEntity & UpdatableEntity, M> ConcurrentHashMapStorage<?, V, M> createFileMapStorage(Class<M> modelType) {
        String areaName = getModelName(modelType, modelType.getSimpleName());
        final Class<V> et = ModelEntityUtil.getEntityType(modelType);
        Function<V, String[]> uniqueHumanReadableField = (Function<V, String[]>) UNIQUE_HUMAN_READABLE_NAME_FIELD.get(et);

        ConcurrentHashMapStorage mapStorage = FileMapStorage.newInstance(et,
                factory.getDataDirectoryFunc(areaName),
                ((uniqueHumanReadableField == null) ? v -> v.getId() == null ? null : new String[]{v.getId()} : uniqueHumanReadableField),
                ExpirableEntity.class.isAssignableFrom(et));
        session.getTransactionManager().enlist(mapStorage);
        return mapStorage;
    }

    @Override
    public void close() {
    }
}
