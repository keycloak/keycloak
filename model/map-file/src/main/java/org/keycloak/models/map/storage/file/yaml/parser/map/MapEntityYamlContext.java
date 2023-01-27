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
package org.keycloak.models.map.storage.file.yaml.parser.map;

import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.EntityField;
import org.keycloak.models.map.role.MapRoleEntityFields;
import org.keycloak.models.map.storage.ModelEntityUtil;
import org.keycloak.models.map.storage.file.yaml.parser.YamlContext;
import org.keycloak.models.map.storage.file.yaml.parser.YamlContext.DefaultListContext;
import org.keycloak.models.map.storage.file.yaml.parser.YamlContext.DefaultMapContext;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

/**
 * {@link YamlContext} which handles any entity accompanied with {@link EntityField} field getters and setters,
 * namely {@code Map*Entity} classes.
 * @author hmlnarik
 */
public class MapEntityYamlContext<T> implements YamlContext<T> {

    private static final Logger LOG = Logger.getLogger(MapEntityYamlContext.class);
    
    private final Map<String, EntityField<?>> nameToEntityField;
    private final Map<String, Supplier<? extends YamlContext<?>>> contextCreators;

    protected final T result;
    private static final Map<Class, Map<String, EntityField<?>>> CACHE_FIELD_TO_EF = new IdentityHashMap<>();
    private static final Map<Class, Map<String, Supplier<? extends YamlContext<?>>>> CACHE_CLASS_TO_CC = new IdentityHashMap<>();

    @SuppressWarnings("unchecked")
    public MapEntityYamlContext(Class<T> clazz) {
        this.result = DeepCloner.DUMB_CLONER.newInstance(clazz);
        this.nameToEntityField = CACHE_FIELD_TO_EF.computeIfAbsent(clazz, MapEntityYamlContext::fieldsToEntityField);
        this.contextCreators = CACHE_CLASS_TO_CC.computeIfAbsent(clazz, MapEntityYamlContext::fieldsToContextCreators);
    }

    public MapEntityYamlContext(
      Class<T> clazz,
      Map<String, EntityField<?>> nameToEntityField,
      Map<String, Supplier<? extends YamlContext<?>>> contextCreators) {
        this.result = DeepCloner.DUMB_CLONER.newInstance(clazz);
        this.nameToEntityField = nameToEntityField;
        this.contextCreators = contextCreators;
    }

    protected static <T> Map<String, Supplier<? extends YamlContext<?>>> fieldsToContextCreators(Class<T> type) {
        if (! ModelEntityUtil.entityFieldsKnown(type)) {
            return Collections.emptyMap();
        }

        return ModelEntityUtil.getEntityFields(type)
          .map(ef -> Map.entry(ef, Optional.ofNullable(getDefaultContextCreator(ef))))
          .filter(me -> me.getValue().isPresent())
          .collect(Collectors.toMap(me -> me.getKey().getNameCamelCase(), me -> me.getValue().get()));
    }

    private static <T> Supplier<? extends YamlContext<?>> getDefaultContextCreator(EntityField<? super T> ef) {
        final Class<?> collectionElementClass = ef.getCollectionElementClass();
        if (collectionElementClass != Void.class) {
            if (ModelEntityUtil.entityFieldsKnown(collectionElementClass)) {
                return () -> new MapEntitySequenceYamlContext<>(collectionElementClass);
            }
        }

        final Class<?> mapValueClass = ef.getMapValueClass();
        if (mapValueClass != Void.class) {
            if (ModelEntityUtil.entityFieldsKnown(mapValueClass)) {
                return () -> new MapEntityMappingYamlContext<>(mapValueClass);
            } else if (ATTRIBUTES_NAME.equals(ef.getName())) {
                return AttributesLikeYamlContext::new;
            }
        }

        return null;
    }

    protected static final String ATTRIBUTES_NAME = MapRoleEntityFields.ATTRIBUTES.getName();

    public static <T> Map<String, EntityField<?>> fieldsToEntityField(Class<T> type) {
        return ModelEntityUtil.getEntityFields(type).collect(Collectors.toUnmodifiableMap(EntityField::getNameCamelCase, Function.identity()));
    }

    @SuppressWarnings("unchecked")
    public static <T> boolean setEntityField(T result, EntityField<? super T> ef, Object value) {
        if (ef == null) {
            return false;
        }

        if (ef.getCollectionElementClass() != Void.class && value instanceof Collection) {
            ((Collection) value).forEach(v -> ef.collectionAdd(result, v));
        } else if (ef.getMapKeyClass() != Void.class && value instanceof Map) {
            ((Map) value).forEach((k, v) -> ef.mapPut(result, k, v));
        } else {
            final Object origValue = ef.get(result);
            if (origValue != null) {
                LOG.warnf("Overwriting value of %s field", ef.getNameCamelCase());
            }
            ef.set(result, value);
        }
        return true;
    }

    @Override
    public void add(String name, Object value) {
        @SuppressWarnings("unchecked")
        EntityField<? super T> ef = (EntityField<? super T>) nameToEntityField.get(name);

        if (! setEntityField(result, ef, value)) {
            LOG.warnf("Ignoring field %s", name);
        }
    }

    @Override
    public T getResult() {
        return this.result;
    }

    @Override
    public YamlContext<?> getContext(String nameOfSubcontext) {
        Supplier<? extends YamlContext<?>> cc = contextCreators.get(nameOfSubcontext);
        if (cc != null) {
            return cc.get();
        }
        EntityField<?> ef = nameToEntityField.get(nameOfSubcontext);
        if (ef != null) {
            if (ef.getCollectionElementClass() != Void.class) {
                return new DefaultListContext();
            } else if (ef.getMapValueClass() != Void.class) {
                return new DefaultMapContext();
            }
            return new DefaultObjectContext();
        }
        LOG.warnf("No special context set for field %s", nameOfSubcontext);
        return null;
    }

    public static class MapEntitySequenceYamlContext<T> extends DefaultListContext {

        private final Class<T> collectionElementClass;

        public MapEntitySequenceYamlContext(Class<T> collectionElementClass) {
            this.collectionElementClass = collectionElementClass;
        }

        @Override
        public YamlContext<?> getContext(String nameOfSubcontext) {
            return new MapEntityYamlContext<>(collectionElementClass);
        }

        @Override
        public void add(String name, Object value) {
            if (value instanceof AbstractEntity) {
                ((AbstractEntity) value).setId(name);
                add(value);
            } else {
                throw new IllegalArgumentException("Sequence expected, mapping with " + name + " key found instead.");
            }
        }
    }

    public static class MapEntityMappingYamlContext<T> extends DefaultMapContext {

        private final Class<T> mapValueClass;

        public MapEntityMappingYamlContext(Class<T> mapValueClass) {
            this.mapValueClass = mapValueClass;
        }

        @Override
        public YamlContext<?> getContext(String nameOfSubcontext) {
            return new MapEntityYamlContext<>(mapValueClass);
        }
    }

}
