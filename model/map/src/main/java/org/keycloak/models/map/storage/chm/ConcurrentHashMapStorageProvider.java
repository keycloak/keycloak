/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.chm;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.Serialization;
import com.fasterxml.jackson.databind.JavaType;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.jboss.logging.Logger;
import org.keycloak.models.map.storage.MapStorageProvider;
import org.keycloak.models.map.storage.ModelCriteriaBuilder;

/**
 *
 * @author hmlnarik
 */
public class ConcurrentHashMapStorageProvider implements MapStorageProvider {

    public static final String PROVIDER_ID = "concurrenthashmap";

    private static final Logger LOG = Logger.getLogger(ConcurrentHashMapStorageProvider.class);

    private final ConcurrentHashMap<String, ConcurrentHashMapStorage<?,?,?>> storages = new ConcurrentHashMap<>();

    private File storageDirectory;

    @Override
    public MapStorageProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Scope config) {
        File f = new File(config.get("dir"));
        try {
            this.storageDirectory = f.exists()
              ? f
              : Files.createTempDirectory("storage-map-chm-").toFile();
        } catch (IOException ex) {
            this.storageDirectory = null;
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
        storages.forEach(this::storeMap);
    }

    private void storeMap(String fileName, ConcurrentHashMapStorage<?, ?, ?> store) {
        if (fileName != null) {
            File f = getFile(fileName);
            try {
                if (storageDirectory != null && storageDirectory.exists()) {
                    LOG.debugf("Storing contents to %s", f.getCanonicalPath());
                    @SuppressWarnings("unchecked")
                    final ModelCriteriaBuilder readAllCriteria = store.createCriteriaBuilder();
                    Serialization.MAPPER.writeValue(f, store.read(readAllCriteria));
                } else {
                    LOG.debugf("Not storing contents of %s because directory %s does not exist", fileName, this.storageDirectory);
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private <K, V extends AbstractEntity<K>, M> ConcurrentHashMapStorage<K, V, M> loadMap(String fileName,
      Class<V> valueType, Class<M> modelType, EnumSet<Flag> flags) {
        ConcurrentHashMapStorage<K, V, M> store = new ConcurrentHashMapStorage<>(modelType);

        if (! flags.contains(Flag.INITIALIZE_EMPTY)) {
            final File f = getFile(fileName);
            if (f != null && f.exists()) {
                try {
                    LOG.debugf("Restoring contents from %s", f.getCanonicalPath());
                    JavaType type = Serialization.MAPPER.getTypeFactory().constructCollectionType(List.class, valueType);

                    List<V> values = Serialization.MAPPER.readValue(f, type);
                    values.forEach((V mce) -> store.create(mce.getId(), mce));
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

        return store;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V extends AbstractEntity<K>, M> ConcurrentHashMapStorage<K, V, M> getStorage(String name,
      Class<K> keyType, Class<V> valueType, Class<M> modelType, Flag... flags) {
        EnumSet<Flag> f = flags == null || flags.length == 0 ? EnumSet.noneOf(Flag.class) : EnumSet.of(flags[0], flags);
        return (ConcurrentHashMapStorage<K, V, M>) storages.computeIfAbsent(name, n -> loadMap(name, valueType, modelType, f));
    }

    private File getFile(String fileName) {
        return storageDirectory == null
          ? null
          : new File(storageDirectory, "map-" + fileName + ".json");
    }

}
