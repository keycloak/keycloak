/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.admin.representations;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.keycloak.representations.admin.v2.BaseRepresentation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

class BaseRepresentationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Create a concrete implementation for testing since BaseRepresentation is abstract
    // through BaseClientRepresentation
    static class TestRepresentation extends BaseRepresentation {
        private String testField;

        public String getTestField() {
            return testField;
        }

        public void setTestField(String testField) {
            this.testField = testField;
        }
    }

    @Test
    void testAdditionalFieldsInitializedAsEmptyMap() {
        TestRepresentation rep = new TestRepresentation();
        
        assertThat(rep.getAdditionalFields(), notNullValue());
        assertThat(rep.getAdditionalFields().entrySet(), empty());
    }

    @Test
    void testAdditionalFieldsIsLinkedHashMap() {
        TestRepresentation rep = new TestRepresentation();
        
        assertThat(rep.getAdditionalFields(), instanceOf(LinkedHashMap.class));
    }

    @Test
    void testSetAdditionalField() {
        TestRepresentation rep = new TestRepresentation();
        
        rep.setAdditionalField("key1", "value1");
        rep.setAdditionalField("key2", 42);
        rep.setAdditionalField("key3", true);
        rep.setAdditionalField("key4", null);
        
        assertThat(rep.getAdditionalFields(), aMapWithSize(4));
        assertThat(rep.getAdditionalFields().get("key1"), is("value1"));
        assertThat(rep.getAdditionalFields().get("key2"), is(42));
        assertThat(rep.getAdditionalFields().get("key3"), is(true));
        assertThat(rep.getAdditionalFields().containsKey("key4"), is(true));
    }

    @Test
    void testSetAdditionalFieldOverwrites() {
        TestRepresentation rep = new TestRepresentation();
        
        rep.setAdditionalField("key", "original");
        rep.setAdditionalField("key", "overwritten");
        
        assertThat(rep.getAdditionalFields().get("key"), is("overwritten"));
    }

    @Test
    void testSetAdditionalFields() {
        TestRepresentation rep = new TestRepresentation();
        Map<String, Object> fields = new HashMap<>();
        fields.put("a", 1);
        fields.put("b", 2);
        
        rep.setAdditionalFields(fields);
        
        assertThat(rep.getAdditionalFields(), is(fields));
    }

    @Test
    void testSetAdditionalFieldsReplacesExisting() {
        TestRepresentation rep = new TestRepresentation();
        rep.setAdditionalField("original", "value");
        
        Map<String, Object> newFields = new HashMap<>();
        newFields.put("new", "newValue");
        rep.setAdditionalFields(newFields);
        
        assertThat(rep.getAdditionalFields(), aMapWithSize(1));
        assertThat(rep.getAdditionalFields().containsKey("original"), is(false));
        assertThat(rep.getAdditionalFields().get("new"), is("newValue"));
    }

    @Test
    void testJsonSerializationWithAdditionalFields() throws JsonProcessingException {
        TestRepresentation rep = new TestRepresentation();
        rep.setTestField("test");
        rep.setAdditionalField("extra1", "extraValue1");
        rep.setAdditionalField("extra2", 123);
        
        String json = MAPPER.writeValueAsString(rep);
        JsonNode node = MAPPER.readTree(json);
        
        assertThat(node.get("testField").asText(), is("test"));
        // Additional fields should be serialized at the top level due to @JsonAnyGetter
        assertThat(node.get("extra1").asText(), is("extraValue1"));
        assertThat(node.get("extra2").asInt(), is(123));
    }

    @Test
    void testJsonDeserializationWithUnknownFields() throws JsonProcessingException {
        String json = """
            {
                "testField": "test",
                "unknownField1": "value1",
                "unknownField2": 42
            }
            """;
        
        TestRepresentation rep = MAPPER.readValue(json, TestRepresentation.class);
        
        assertThat(rep.getTestField(), is("test"));
        // Unknown fields should be captured via @JsonAnySetter
        assertThat(rep.getAdditionalFields().get("unknownField1"), is("value1"));
        assertThat(rep.getAdditionalFields().get("unknownField2"), is(42));
    }

    @Test
    void testJsonSerializationOmitsNullFields() throws JsonProcessingException {
        TestRepresentation rep = new TestRepresentation();
        // testField is null, additionalFields is empty
        
        String json = MAPPER.writeValueAsString(rep);
        JsonNode node = MAPPER.readTree(json);
        
        // Due to @JsonInclude(JsonInclude.Include.NON_ABSENT), null fields should be omitted
        assertThat(node.has("testField"), is(false));
    }

    @Test
    void testAdditionalFieldsPreserveInsertionOrder() {
        TestRepresentation rep = new TestRepresentation();
        
        rep.setAdditionalField("z", 1);
        rep.setAdditionalField("a", 2);
        rep.setAdditionalField("m", 3);
        
        // LinkedHashMap should preserve insertion order
        String[] keys = rep.getAdditionalFields().keySet().toArray(new String[0]);
        assertThat(keys[0], is("z"));
        assertThat(keys[1], is("a"));
        assertThat(keys[2], is("m"));
    }

    @Test
    void testComplexAdditionalFieldValues() throws JsonProcessingException {
        TestRepresentation rep = new TestRepresentation();
        rep.setAdditionalField("nested", Map.of("inner", "value"));
        rep.setAdditionalField("list", java.util.List.of("a", "b", "c"));
        
        String json = MAPPER.writeValueAsString(rep);
        JsonNode node = MAPPER.readTree(json);
        
        assertThat(node.get("nested").isObject(), is(true));
        assertThat(node.get("nested").get("inner").asText(), is("value"));
        assertThat(node.get("list").isArray(), is(true));
        assertThat(node.get("list").size(), is(3));
    }
}
