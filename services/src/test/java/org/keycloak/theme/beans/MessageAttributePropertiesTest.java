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
    public void reflectsAttributesAddedAfterConstruction() {
        Properties messages = new Properties();
        Map<String, Object> attributes = new HashMap<>();

        MessageAttributeProperties properties = new MessageAttributeProperties(messages, attributes);

        assertNull(properties.getProperty("url"));

        // Attributes are looked up live: values added to the map after construction (as happens while
        // createResponse() keeps populating it) must become visible once template rendering resolves them.
        attributes.put("url", "https://example.org");

        assertEquals("https://example.org", properties.getProperty("url"));
    }

    @Test
    public void isReadOnly() {
        MessageAttributeProperties properties = new MessageAttributeProperties(new Properties(), new HashMap<>());

        assertThrows(UnsupportedOperationException.class, () -> properties.setProperty("key", "value"));
        assertThrows(UnsupportedOperationException.class, () -> properties.put("key", "value"));
        assertThrows(UnsupportedOperationException.class, () -> properties.remove("key"));
        assertThrows(UnsupportedOperationException.class, () -> properties.putAll(new HashMap<>()));
        assertThrows(UnsupportedOperationException.class, properties::clear);
    }
}
