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

import java.util.Map;
import java.util.Properties;

/**
 * Read-only {@link Properties} view used to resolve {@code ${...}} placeholders inside message strings
 * (see {@link MessageFormatterMethod}), checking the message bundle first and falling back to arbitrary
 * template attributes (e.g. {@code ${realm.displayName}}).
 * <p>
 * This is a zero-copy alternative to merging the message bundle and the template attributes into a new
 * map: message lookups are delegated to {@code messages} via {@link Properties}' native {@code defaults}
 * chaining, and attribute lookups hold a live reference to the (still-mutable-elsewhere) {@code attributes}
 * map. As a result, attributes added to that map after this instance is constructed remain visible, which
 * matches the point in the request lifecycle (template rendering) at which lookups actually happen.
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
}
