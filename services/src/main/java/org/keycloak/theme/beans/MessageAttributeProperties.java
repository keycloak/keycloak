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
 * chaining, and attribute lookups hold a reference to the (still-mutable-elsewhere) {@code attributes}
 * map.
 * <p>
 * That reference is rebindable (see {@link #rebind(Map)}), not just held live from construction: {@code
 * FreeMarkerLoginFormsProvider} constructs this instance in {@code handleThemeResources()}, but the map it
 * actually hands to FreeMarker for rendering (in {@code processTemplate()}) can be a different instance by
 * then, if an {@code attributeMapper} is registered and returns a replacement map rather than mutating the
 * original in place. Without rebinding, a {@code ${...}} placeholder embedded inside a translated message
 * string would keep resolving against the original, pre-mapper map, while the exact same placeholder
 * written directly in a template file resolves against the post-mapper map - the two would silently
 * disagree. {@code FreeMarkerLoginFormsProvider} calls {@link #rebind(Map)} with the post-mapper map right
 * before rendering, so both paths resolve attributes as of the same, final render-time view.
 */
public class MessageAttributeProperties extends Properties {

    private Map<String, Object> attributes;

    public MessageAttributeProperties(Properties messages, Map<String, Object> attributes) {
        super(messages);
        this.attributes = attributes;
    }

    /**
     * Rebinds attribute lookups to a different {@code attributes} map, replacing the one passed to the
     * constructor. Called by {@code FreeMarkerLoginFormsProvider.processTemplate()} once the final,
     * post-{@code attributeMapper} map is known, so lookups made while rendering (e.g. from {@link
     * MessageFormatterMethod}) see the same map FreeMarker itself renders against.
     */
    public void rebind(Map<String, Object> attributes) {
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
