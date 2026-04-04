/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.client.admin.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ReflectionUtilTest {

    @Test
    public void testValueToJsonNode() {
        assertEquals("x", ReflectionUtil.valueToJsonNode("x").asText());
        assertEquals("x", ReflectionUtil.valueToJsonNode("'x'").asText());
        assertEquals("x", ReflectionUtil.valueToJsonNode("\"x\"").asText());
        assertEquals("x\"y", ReflectionUtil.valueToJsonNode("\"x\"y\"").asText());
        // should preserve the leading 0
        assertEquals("0123", ReflectionUtil.valueToJsonNode("0123").asText());
        JsonNode value = ReflectionUtil.valueToJsonNode("123");
        assertTrue(value instanceof NumericNode);
        assertEquals(123, value.asInt());
        value = ReflectionUtil.valueToJsonNode("[\"x\",\"y\"]");
        assertTrue(value instanceof ArrayNode);
        assertEquals("y", value.get(1).textValue());
    }

}
