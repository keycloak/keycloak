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

package org.keycloak.json;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author <a href="mailto:urs.honegger@starmind.com">Urs Honegger</a>
 */
public class StringListMapDeserializerTest {

    private ObjectMapper mapper;

    @Before
    public void setUp() {
        mapper = new ObjectMapper();
    }

    @After
    public void tearDown() {
        mapper = null;
    }

    @Test
    public void nonNullValue() throws IOException {
        Map<String, List<String>> attributes = deserialize("\"foo\": \"bar\"");
        assertTrue(attributes.containsKey("foo"));
        List<String> foo = attributes.get("foo");
        assertEquals(1, foo.size());
        assertEquals("bar", foo.get(0));

    }

    @Test
    public void nonNullValueArray() throws IOException {
        Map<String, List<String>> attributes = deserialize("\"foo\": [ \"bar\", \"baz\" ]");
        assertTrue(attributes.containsKey("foo"));
        List<String> foo = attributes.get("foo");
        assertEquals(2, foo.size());
        assertEquals("baz", foo.get(1));
    }

    @Test
    public void nullValue() throws IOException {
        // null values must deserialize to null
        Map<String, List<String>> attributes = deserialize("\"foo\": null");
        assertTrue(attributes.containsKey("foo"));
        List<String> foo = attributes.get("foo");
        assertEquals(1, foo.size());
        assertNull(foo.get(0));
    }

    @Test
    public void nullValueArray() throws IOException {
        // null values must deserialize to null
        Map<String, List<String>> attributes = deserialize("\"foo\": [ null, \"something\", null ]");
        assertTrue(attributes.containsKey("foo"));
        List<String> foo = attributes.get("foo");
        assertEquals(3, foo.size());
        assertEquals("something", foo.get(1));
        assertNull(foo.get(2));
    }

    private Map<String, List<String>> deserialize(String attributeKeyValueString) throws IOException {
        TestObject testObject = mapper.readValue("{ \"attributes\": {" + attributeKeyValueString + " } }", TestObject.class);

        return testObject.getAttributes();
    }

    private static class TestObject {

        @JsonDeserialize(using = StringListMapDeserializer.class)
        private final Map<String, List<String>> attributes = null;

        public Map<String, List<String>> getAttributes() {
            return attributes;
        }
    }

}
