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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

public class MessageAttributePropertiesTest {

    @Test
    public void resolvesMessageKeyBeforeAttribute() {
        Properties messages = new Properties();
        messages.setProperty("greeting", "hello");

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("greeting", "should not be used");

        MessageAttributeProperties properties = new MessageAttributeProperties(messages, attributes);

        assertEquals("hello", properties.getProperty("greeting"));
    }

    @Test
    public void fallsBackToAttributeWhenMessageMissing() {
        Properties messages = new Properties();

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("realm", "example-realm");

        MessageAttributeProperties properties = new MessageAttributeProperties(messages, attributes);

        assertEquals("example-realm", properties.getProperty("realm"));
    }

    @Test
    public void returnsNullWhenKeyIsUnknown() {
        MessageAttributeProperties properties = new MessageAttributeProperties(new Properties(), new HashMap<>());

        assertNull(properties.getProperty("unknown"));
    }

    @Test
    public void resolvesAttributesAsOfRenderTimeNotConstructionTime() {
        Properties messages = new Properties();
        Map<String, Object> attributes = new HashMap<>();

        MessageAttributeProperties properties = new MessageAttributeProperties(messages, attributes);

        assertNull(properties.getProperty("url"));

        // Deliberate: FreeMarkerLoginFormsProvider.processTemplate() resolves native ${...} template
        // expressions against the same, by-then fully-populated attributes map. Attributes added after
        // this instance is constructed (as happens while createCommonAttributes() keeps populating it)
        // must become visible too, so a ${...} placeholder embedded inside a translated message string
        // resolves consistently with the same placeholder written directly in a template file.
        attributes.put("url", "https://example.org");

        assertEquals("https://example.org", properties.getProperty("url"));
    }

    @Test
    public void rebindSwitchesAttributeLookupsToTheGivenMap() {
        Properties messages = new Properties();
        Map<String, Object> original = new HashMap<>();
        original.put("url", "https://original.example.org");

        MessageAttributeProperties properties = new MessageAttributeProperties(messages, original);
        assertEquals("https://original.example.org", properties.getProperty("url"));

        // Simulates FreeMarkerLoginFormsProvider.processTemplate() rebinding to the map returned by a
        // registered attributeMapper, which may be a different instance rather than a mutation of the
        // original: lookups must follow the rebound map, not stay pinned to the one passed to the
        // constructor.
        Map<String, Object> replacement = new HashMap<>();
        replacement.put("url", "https://mapped.example.org");
        properties.rebind(replacement);

        assertEquals("https://mapped.example.org", properties.getProperty("url"));

        // The original map is no longer consulted at all, even if it is still mutated afterwards.
        original.put("url", "https://should-not-be-seen.example.org");
        assertEquals("https://mapped.example.org", properties.getProperty("url"));
    }

    @Test
    public void isReadOnly() {
        MessageAttributeProperties properties = new MessageAttributeProperties(new Properties(), new HashMap<>());

        assertThrows(UnsupportedOperationException.class, () -> properties.setProperty("key", "value"));
        assertThrows(UnsupportedOperationException.class, () -> properties.put("key", "value"));
        assertThrows(UnsupportedOperationException.class, () -> properties.remove("key"));
        assertThrows(UnsupportedOperationException.class, () -> properties.putAll(new HashMap<>()));
        assertThrows(UnsupportedOperationException.class, properties::clear);
        assertThrows(UnsupportedOperationException.class, () -> properties.putIfAbsent("key", "value"));
        assertThrows(UnsupportedOperationException.class, () -> properties.remove("key", "value"));
        assertThrows(UnsupportedOperationException.class, () -> properties.replace("key", "old", "new"));
        assertThrows(UnsupportedOperationException.class, () -> properties.replace("key", "value"));
        assertThrows(UnsupportedOperationException.class, () -> properties.computeIfAbsent("key", k -> "value"));
        assertThrows(UnsupportedOperationException.class, () -> properties.computeIfPresent("key", (k, v) -> "value"));
        assertThrows(UnsupportedOperationException.class, () -> properties.compute("key", (k, v) -> "value"));
        assertThrows(UnsupportedOperationException.class, () -> properties.merge("key", "value", (a, b) -> b));
        assertThrows(UnsupportedOperationException.class, () -> properties.replaceAll((k, v) -> v));
    }

    @Test
    public void getOrDefaultResolvesMessageBeforeAttribute() {
        Properties messages = new Properties();
        messages.setProperty("greeting", "hello");

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("greeting", "should not be used");

        MessageAttributeProperties properties = new MessageAttributeProperties(messages, attributes);

        // MessageFormatterMethod.exec() looks up messages via getOrDefault(), not getProperty() - it must
        // go through the same message-then-attribute lookup, not Hashtable's own (always empty) entries.
        assertEquals("hello", properties.getOrDefault("greeting", "greeting"));
    }

    @Test
    public void getOrDefaultFallsBackToAttribute() {
        Properties messages = new Properties();
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("realm", "example-realm");

        MessageAttributeProperties properties = new MessageAttributeProperties(messages, attributes);

        assertEquals("example-realm", properties.getOrDefault("realm", "realm"));
    }

    @Test
    public void getOrDefaultFallsBackToDefaultValueWhenKeyIsUnknown() {
        MessageAttributeProperties properties = new MessageAttributeProperties(new Properties(), new HashMap<>());

        assertEquals("unknown", properties.getOrDefault("unknown", "unknown"));
    }
}
