/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JsonUtilsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Reproduces issue #41321: when a first mapper stores a JSON ObjectNode at a claim path
     * and a second mapper tries to add a nested claim through that same path, the traversal
     * code must not throw a ClassCastException when it encounters the ObjectNode.
     */
    @Test
    public void testMapClaim_nestedObjectNode_noClassCastException() throws Exception {
        Map<String, Object> claims = new HashMap<>();

        // Mapper 1: sets "claim" to an ObjectNode (simulates JSON-type UserSessionNoteMapper)
        ObjectNode firstValue = (ObjectNode) MAPPER.readTree("{\"nested1\":{\"nested2\":{\"nested3\":{\"field1\":\"test\"}}}}");
        List<String> path1 = JsonUtils.splitClaimPath("claim");
        JsonUtils.mapClaim(path1, firstValue, claims, false);

        // Verify mapper 1 stored the ObjectNode
        assertTrue(claims.get("claim") instanceof ObjectNode);

        // Mapper 2: attempts to set "claim.nested1.nested2.nested3" — traverses through the ObjectNode
        ObjectNode secondValue = (ObjectNode) MAPPER.readTree("{\"field2\":\"test2\"}");
        List<String> path2 = JsonUtils.splitClaimPath("claim.nested1.nested2.nested3");

        // Must not throw ClassCastException
        JsonUtils.mapClaim(path2, secondValue, claims, false);

        // Verify the nested structure is correct
        @SuppressWarnings("unchecked")
        Map<String, Object> claimMap = (Map<String, Object>) claims.get("claim");
        assertNotNull("claim should be a Map after second mapper ran", claimMap);

        @SuppressWarnings("unchecked")
        Map<String, Object> nested1 = (Map<String, Object>) claimMap.get("nested1");
        assertNotNull(nested1);

        @SuppressWarnings("unchecked")
        Map<String, Object> nested2 = (Map<String, Object>) nested1.get("nested2");
        assertNotNull(nested2);

        // The second mapper's value should be at nested3
        assertEquals(secondValue, nested2.get("nested3"));
    }

    @Test
    public void testMapClaim_nestedObjectNode_preservesExistingFields() throws Exception {
        Map<String, Object> claims = new HashMap<>();

        // Mapper 1: sets "data" to an ObjectNode with multiple fields
        ObjectNode firstValue = (ObjectNode) MAPPER.readTree("{\"field1\":\"a\",\"sub\":{\"key\":\"val\"}}");
        JsonUtils.mapClaim(List.of("data"), firstValue, claims, false);

        // Mapper 2: adds a new sibling field under "data" via "data.field2"
        JsonUtils.mapClaim(List.of("data", "field2"), "added", claims, false);

        @SuppressWarnings("unchecked")
        Map<String, Object> dataMap = (Map<String, Object>) claims.get("data");
        assertNotNull(dataMap);
        assertEquals("added", dataMap.get("field2"));
        // field1 from the original ObjectNode must still be present
        assertNotNull("field1 from original ObjectNode should be preserved", dataMap.get("field1"));
    }

    @Test
    public void testMapClaim_regularNestedMap_unchanged() {
        Map<String, Object> claims = new HashMap<>();

        // Standard case: two mappers both writing into the same nested Map path
        JsonUtils.mapClaim(List.of("a", "b"), "first", claims, false);
        JsonUtils.mapClaim(List.of("a", "c"), "second", claims, false);

        @SuppressWarnings("unchecked")
        Map<String, Object> aMap = (Map<String, Object>) claims.get("a");
        assertEquals("first", aMap.get("b"));
        assertEquals("second", aMap.get("c"));
    }
}
