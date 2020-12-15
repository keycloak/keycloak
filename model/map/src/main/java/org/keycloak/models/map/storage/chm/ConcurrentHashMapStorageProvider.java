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
package org.keycloak.models.map.storage;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.Serialization;
import org.keycloak.storage.SearchableModelField;
import com.fasterxml.jackson.databind.JavaType;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.jboss.logging.Logger;

/**
 *
 * @author hmlnarik
 */
public class ConcurrentHashMapStorageProvider implements MapStorageProvider {

    public static class ConcurrentHashMapStorage<K, V> implements MapStorage<K, V> {

        private final ConcurrentMap<K, V> store = new ConcurrentHashMap<>();

        @Override
        public V create(K key, V value) {
            return store.putIfAbsent(key, value);
        }

        @Override
        public V read(K key) {
            return store.get(key);
        }

        @Override
        public V update(K key, V value) {
            return store.replace(key, value);
        }

        @Override
        public V delete(K key) {
            return store.remove(key);
        }

        @Override
        public ModelCriteriaBuilder createCriteriaBuilder() {
            return new MapModelCriteriaBuilder(null);
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return store.entrySet();
        }

        @Override
        public Stream<V> read(ModelCriteriaBuilder criteria) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    private static class MapModelCriteriaBuilder<M> implements ModelCriteriaBuilder {

        @FunctionalInterface
        public interface TriConsumer<A extends MapModelCriteriaBuilder<?>,B,C> { A apply(A a, B b, C c); }

        private static final Predicate<Object> ALWAYS_TRUE = e -> true;
        private static final Predicate<Object> ALWAYS_FALSE = e -> false;

        private final Predicate<? super String> indexFilter;
        private final Predicate<? super M> modelFilter;

        private final Map<String, TriConsumer<MapModelCriteriaBuilder<M>, Operator, Object>> fieldPredicates;

        public MapModelCriteriaBuilder(Map<String, TriConsumer<MapModelCriteriaBuilder<M>, Operator, Object>> fieldPredicates) {
            this(fieldPredicates, ALWAYS_TRUE, ALWAYS_TRUE);
        }

        private MapModelCriteriaBuilder(Map<String, TriConsumer<MapModelCriteriaBuilder<M>, Operator, Object>> fieldPredicates,
          Predicate<? super String> indexReadFilter, Predicate<? super M> sequentialReadFilter) {
            this.fieldPredicates = fieldPredicates;
            this.indexFilter = indexReadFilter;
            this.modelFilter = sequentialReadFilter;
        }

        @Override
        public ModelCriteriaBuilder compare(SearchableModelField modelField, Operator op, Object... value) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    @Override
    public MapModelCriteriaBuilder<M> and(ModelCriteriaBuilder... builders) {
        Predicate<? super String> resIndexFilter = Stream.of(builders)
          .map(MapModelCriteriaBuilder.class::cast)
          .map(MapModelCriteriaBuilder::getIndexFilter)
          .reduce(ALWAYS_TRUE, (p1, p2) -> p1.and(p2));
        Predicate<? super M> resModelFilter = Stream.of(builders)
          .map(MapModelCriteriaBuilder.class::cast)
          .map(MapModelCriteriaBuilder::getModelFilter)
          .reduce(ALWAYS_TRUE, (p1, p2) -> p1.and(p2));
        return new MapModelCriteriaBuilder<>(fieldPredicates, resIndexFilter, resModelFilter);
    }

    @Override
    public MapModelCriteriaBuilder<M> or(ModelCriteriaBuilder... builders) {
        Predicate<? super String> resIndexFilter = Stream.of(builders)
          .map(MapModelCriteriaBuilder.class::cast)
          .map(MapModelCriteriaBuilder::getIndexFilter)
          .reduce(ALWAYS_FALSE, (p1, p2) -> p1.or(p2));
        Predicate<? super M> resModelFilter = Stream.of(builders)
          .map(MapModelCriteriaBuilder.class::cast)
          .map(MapModelCriteriaBuilder::getModelFilter)
          .reduce(ALWAYS_FALSE, (p1, p2) -> p1.or(p2));
        return new MapModelCriteriaBuilder<>(fieldPredicates, resIndexFilter, resModelFilter);
    }

    @Override
    public MapModelCriteriaBuilder<M> not(ModelCriteriaBuilder builder) {
        MapModelCriteriaBuilder b = builder.unwrap(MapModelCriteriaBuilder.class);
        Predicate<? super String> resIndexFilter = b.getIndexFilter() == ALWAYS_TRUE ? ALWAYS_TRUE : b.getIndexFilter().negate();
        Predicate<? super M> resModelFilter = b.getModelFilter() == ALWAYS_TRUE ? ALWAYS_TRUE : b.getModelFilter().negate();
        return new MapModelCriteriaBuilder<>(fieldPredicates, resIndexFilter, resModelFilter);
    }

        public Predicate<? super String> getIndexFilter() {
            return indexFilter;
        }

        public Predicate<? super M> getModelFilter() {
            return modelFilter;
        }
    }

    private static final String PROVIDER_ID = "concurrenthashmap";

    private static final Logger LOG = Logger.getLogger(ConcurrentHashMapStorageProvider.class);

    private final ConcurrentHashMap<String, ConcurrentHashMapStorage<?,?>> storages = new ConcurrentHashMap<>();

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

    private void storeMap(String fileName, ConcurrentHashMapStorage<?, ?> store) {
        if (fileName != null) {
            File f = getFile(fileName);
            try {
                if (storageDirectory != null && storageDirectory.exists()) {
                    LOG.debugf("Storing contents to %s", f.getCanonicalPath());
                    Serialization.MAPPER.writeValue(f, store.entrySet().stream().map(Map.Entry::getValue));
                } else {
                    LOG.debugf("Not storing contents of %s because directory %s does not exist", fileName, this.storageDirectory);
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private <K, V extends AbstractEntity<K>> ConcurrentHashMapStorage<K, V> loadMap(String fileName, Class<V> valueType, EnumSet<Flag> flags) {
        ConcurrentHashMapStorage<K, V> store = new ConcurrentHashMapStorage<>();

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
    public <K, V extends AbstractEntity<K>> ConcurrentHashMapStorage<K, V> getStorage(String name, Class<K> keyType, Class<V> valueType, Flag... flags) {
        EnumSet<Flag> f = flags == null || flags.length == 0 ? EnumSet.noneOf(Flag.class) : EnumSet.of(flags[0], flags);
        return (ConcurrentHashMapStorage<K, V>) storages.computeIfAbsent(name, n -> loadMap(name, valueType, f));
    }

    private File getFile(String fileName) {
        return storageDirectory == null
          ? null
          : new File(storageDirectory, "map-" + fileName + ".json");
    }

}
