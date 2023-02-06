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
package org.keycloak.models.map.storage.file.common;

import org.keycloak.models.map.common.UndefinedValuesUtils;
import org.keycloak.models.map.storage.file.yaml.YamlParser;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import static org.keycloak.models.map.common.CastUtils.cast;

/**
 * A class implementing a {@code BlockContext} interface represents a transformer
 * from a primitive value / sequence / mapping representation as declared in YAML
 * format into a Java object of type {@code V}, with ability to produce
 * the {@link #getResult() resulting instance} of parsing.
 *
 * <p>
 * This transformer handles only a values of a single node in structured file, i.e.
 * single value (a primitive value, sequence or mapping). The root level
 * is at the beginning of e.g. YAML or JSON document.
 * Every mapping key and every sequence value then represents next level of nesting.
 *
 * @author hmlnarik
 * @param <V> Type of the result
 */
public interface BlockContext<V> {

    /**
     * Writes the given value using {@link WritingMechanism}.
     *
     * @param value
     * @param mech
     */
    void writeValue(V value, WritingMechanism mech);

    /**
     * Called after reading a key of map entry in YAML file and before reading its value.
     * The key of the entry is represented as {@code nameOfSubcontext} parameter, and
     * provides means to specify a {@code YamlContext} for transforming the mapping value
     * into appropriate Java object.
     *
     * @param nameOfSubcontext Key of the map entry
     *
     * @return Context used for transforming the value,
     * or {@code null} if the default primitive / sequence / mapping context should be used instead.
     *
     * @see DefaultObjectContext
     * @see DefaultListContext
     * @see DefaultMapContext
     */
    BlockContext<?> getContext(String nameOfSubcontext);

    /**
     * Modifies the {@link #getResult() result returned} from within this context by
     * providing the read mapping entry {@code name} to given {@code value}.
     * <p>
     * Called after reading a map entry (both key and value) from the YAML file is finished.
     * The entry is represented as {@code name} parameter (key part of the entry)
     * and {@code value} (value part of the entry).
     * <p>
     * The method is called in the same order as the mapping items appear in the source YAML mapping.
     *
     * @param name
     * @param value
     */
    default void add(String name, Object value) { };

    /**
     * Modifies the {@link #getResult() result returned} from within this context by
     * providing the read primitive value or a single sequence item in the {@code value} parameter.
     * <p>
     * Called after reading a primitive value or a single sequence item
     * from the YAML file is finished.
     * <p>
     * If the parsed YAML part was a sequence, this method is called in the same order
     * as the sequence items appear in the source YAML sequence.
     *
     * @param value
     */
    default void add(Object value) { };

    /**
     * Returns the result of parsing the given part of YAML file.
     * @return
     */
    V getResult();

    Class<?> getScalarType();

    public static class DefaultObjectContext<T> implements BlockContext<T> {
        
        private final Class<T> objectType;
        private T result;

        public DefaultObjectContext(Class<T> objectType) {
            this.objectType = objectType;
        }

        public static DefaultObjectContext<Object> newDefaultObjectContext() {
            return new DefaultObjectContext<>(Object.class);
        }

        @Override
        public Class<T> getScalarType() {
            return objectType;
        }

        @Override
        public void add(Object value) {
            result = (T) value;
        }

        @Override
        public T getResult() {
            return result;
        }

        @Override
        public void writeValue(Object value, WritingMechanism mech) {
            if (UndefinedValuesUtils.isUndefined(value)) return;
            mech.writeObject(value);
        }

        @Override
        public BlockContext<?> getContext(String nameOfSubcontext) {
            return null;
        }
    }

    public static class DefaultListContext<T> implements BlockContext<Collection<T>> {
        private final List<T> result = new LinkedList<>();

        protected final Class<T> itemClass;

        public static DefaultListContext<Object> newDefaultListContext() {
            return new DefaultListContext<>(Object.class);
        }

        public DefaultListContext(Class<T> itemClass) {
            this.itemClass = itemClass;
        }

        @Override
        public Class<T> getScalarType() {
            return itemClass;
        }

        @Override
        public void add(Object value) {
            result.add(cast(value, itemClass));
        }

        @Override
        public List<T> getResult() {
            return result;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void writeValue(Collection<T> value, WritingMechanism mech) {
            if (UndefinedValuesUtils.isUndefined(value)) return;
            mech.writeSequence(() -> value.forEach(v -> getContextByValue(v).writeValue(v, mech)));
        }

        @Override
        public BlockContext<?> getContext(String nameOfSubcontext) {
            return null;
        }

        private BlockContext getContextByValue(Object value) {
            BlockContext res = getContext(YamlParser.ARRAY_CONTEXT);
            if (res != null) {
                return res;
            }
            if (value instanceof Collection) {
                return new DefaultListContext<>(itemClass);
            } else if (value instanceof Map) {
                return DefaultMapContext.newDefaultMapContext();
            } else {
                return new DefaultObjectContext<>(itemClass);
            }
        }
    }

    public static class DefaultMapContext<T> implements BlockContext<Map<String, T>> {
        private final Map<String, T> result = new LinkedHashMap<>();

        protected final Class<T> itemClass;

        public static DefaultMapContext<Object> newDefaultMapContext() {
            return new DefaultMapContext<>(Object.class);
        }

        public DefaultMapContext(Class<T> itemClass) {
            this.itemClass = itemClass;
        }

        @Override
        public Class<T> getScalarType() {
            return itemClass;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void add(String name, Object value) {
            result.put(name, (T) value);
        }

        @Override
        public Map<String, T> getResult() {
            return result;
        }

        @Override
        public void writeValue(Map<String, T> value, WritingMechanism mech) {
            if (UndefinedValuesUtils.isUndefined(value)) return;
            mech.writeMapping(() -> {
                final TreeMap<String, Object> sortedMap = new TreeMap<>(value);
                sortedMap.forEach(
                  (key, val) -> mech.writePair(
                                  key,
                                  () -> getContext(key, val).writeValue(val, mech)
                                )
                );
            });
        }

        @Override
        public BlockContext<T> getContext(String nameOfSubcontext) {
            return null;
        }

        private BlockContext getContext(String nameOfSubcontext, Object value) {
            BlockContext res = getContext(nameOfSubcontext);
            if (res != null) {
                return res;
            }
            if (value instanceof Collection) {
                return new DefaultListContext<>(itemClass);
            } else if (value instanceof Map) {
                return DefaultMapContext.newDefaultMapContext();
            } else {
                return new DefaultObjectContext<>(itemClass);
            }
        }
    }

}
