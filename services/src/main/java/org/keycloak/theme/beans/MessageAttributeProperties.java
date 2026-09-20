/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.theme.beans;

import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Read-only {@link Properties} view used to resolve {@code ${...}} placeholders inside message strings
 * (see {@link MessageFormatterMethod}), checking the message bundle first and falling back to arbitrary
 * template attributes (e.g. {@code ${realm.displayName}}).
 * <p>
 * This is a zero-copy alternative to merging the message bundle and the template attributes into a new
 * map: message lookups are delegated to {@code messages} via {@link Properties}' native {@code defaults}
 * chaining, and attribute lookups hold a live reference to the (still-mutable-elsewhere) {@code attributes}
 * map.
 * <p>
 * Holding a live reference (rather than a snapshot taken when this instance is constructed) is a deliberate
 * choice, not just a side effect of avoiding a copy: {@code FreeMarkerLoginFormsProvider} only calls
 * {@code freeMarker.processTemplate(attributes, ...)} (which resolves native FreeMarker {@code ${...}}
 * expressions directly against {@code attributes}) after all attributes have been populated. A snapshot
 * taken earlier, when this instance used to be constructed in {@code handleThemeResources()}, would let a
 * {@code ${...}} placeholder embedded inside a translated message string see a different, incomplete view
 * of the same attributes than the exact same placeholder written directly in a template file. The live
 * reference removes that inconsistency: both paths now resolve attributes as of render time.
 */
public class MessageAttributeProperties extends Properties {

    private final Map<String, Object> attributes;

    public MessageAttributeProperties(Properties messages, Map<String, Object> attributes) {
        super(messages);
        this.attributes = attributes;
    }

    @Override
    public String getProperty(String key) {
        String value = super.getProperty(key);
        if (value != null) {
            return value;
        }
        Object attribute = attributes.get(key);
        return attribute != null ? attribute.toString() : null;
    }

    /**
     * {@link Hashtable#getOrDefault(Object, Object)} only consults this instance's own (always empty) entries,
     * ignoring the {@code Properties} {@code defaults} chain that {@link #getProperty(String)} relies on.
     * Route it through {@link #getProperty(String)} instead, so callers using either method see the same result.
     */
    @Override
    public Object getOrDefault(Object key, Object defaultValue) {
        if (key instanceof String) {
            String value = getProperty((String) key);
            if (value != null) {
                return value;
            }
        }
        return defaultValue;
    }

    @Override
    public synchronized Object setProperty(String key, String value) {
        throw new UnsupportedOperationException("MessageAttributeProperties is read-only");
    }

    @Override
    public synchronized Object put(Object key, Object value) {
        throw new UnsupportedOperationException("MessageAttributeProperties is read-only");
    }

    @Override
    public synchronized Object remove(Object key) {
        throw new UnsupportedOperationException("MessageAttributeProperties is read-only");
    }

    @Override
    public synchronized void putAll(Map<?, ?> t) {
        throw new UnsupportedOperationException("MessageAttributeProperties is read-only");
    }

    @Override
    public synchronized void clear() {
        throw new UnsupportedOperationException("MessageAttributeProperties is read-only");
    }

    @Override
    public synchronized Object putIfAbsent(Object key, Object value) {
        throw new UnsupportedOperationException("MessageAttributeProperties is read-only");
    }

    @Override
    public synchronized boolean remove(Object key, Object value) {
        throw new UnsupportedOperationException("MessageAttributeProperties is read-only");
    }

    @Override
    public synchronized boolean replace(Object key, Object oldValue, Object newValue) {
        throw new UnsupportedOperationException("MessageAttributeProperties is read-only");
    }

    @Override
    public synchronized Object replace(Object key, Object value) {
        throw new UnsupportedOperationException("MessageAttributeProperties is read-only");
    }

    @Override
    public synchronized Object computeIfAbsent(Object key, Function<? super Object, ?> mappingFunction) {
        throw new UnsupportedOperationException("MessageAttributeProperties is read-only");
    }

    @Override
    public synchronized Object computeIfPresent(Object key, BiFunction<? super Object, ? super Object, ?> remappingFunction) {
        throw new UnsupportedOperationException("MessageAttributeProperties is read-only");
    }

    @Override
    public synchronized Object compute(Object key, BiFunction<? super Object, ? super Object, ?> remappingFunction) {
        throw new UnsupportedOperationException("MessageAttributeProperties is read-only");
    }

    @Override
    public synchronized Object merge(Object key, Object value, BiFunction<? super Object, ? super Object, ?> remappingFunction) {
        throw new UnsupportedOperationException("MessageAttributeProperties is read-only");
    }

    @Override
    public synchronized void replaceAll(BiFunction<? super Object, ? super Object, ?> function) {
        throw new UnsupportedOperationException("MessageAttributeProperties is read-only");
    }
}
