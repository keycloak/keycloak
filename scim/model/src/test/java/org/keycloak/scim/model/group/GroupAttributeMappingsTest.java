/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.keycloak.scim.model.group;

import java.util.List;

import org.keycloak.models.ModelValidationException;
import org.keycloak.scim.model.group.GroupAttributeMappings.Mapping;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GroupAttributeMappingsTest {

    private static final String SCHEMA = "urn:example:params:scim:schemas:extension:custom:1.0:Group";

    @Test
    public void testEmpty() {
        assertTrue(GroupAttributeMappings.parse(null).isEmpty());
        assertTrue(GroupAttributeMappings.parse(" ").isEmpty());
        assertTrue(GroupAttributeMappings.parse("[]").isEmpty());
    }

    @Test
    public void testParse() {
        List<Mapping> mappings = GroupAttributeMappings.parse("""
                [
                  {"name": "entryUUID", "scimAttribute": "%1$s:entryUUID", "readOnly": true},
                  {"name": "tags", "scimAttribute": "%1$s:tags", "multivalued": true}
                ]
                """.formatted(SCHEMA));

        assertEquals(List.of(
                new Mapping("entryUUID", SCHEMA + ":entryUUID", true, false),
                new Mapping("tags", SCHEMA + ":tags", false, true)), mappings);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "not json",
            "{}",
            "[\"entryUUID\"]",
            "[{\"scimAttribute\": \"" + SCHEMA + ":entryUUID\"}]",
            "[{\"name\": \"entryUUID\"}]",
            "[{\"name\": \"entryUUID\", \"scimAttribute\": \"" + SCHEMA + ":entryUUID\", \"unknown\": true}]",
            "[{\"name\": \"entryUUID\", \"scimAttribute\": \"" + SCHEMA + ":entryUUID\", \"readOnly\": \"true\"}]",
            "[{\"name\": 1, \"scimAttribute\": \"" + SCHEMA + ":entryUUID\"}]",
            // not prefixed with a schema URN
            "[{\"name\": \"entryUUID\", \"scimAttribute\": \"entryUUID\"}]",
            "[{\"name\": \"entryUUID\", \"scimAttribute\": \"Group:entryUUID\"}]",
            // invalid schema URNs
            "[{\"name\": \"entryUUID\", \"scimAttribute\": \"urn:bad schema:Group:entryUUID\"}]",
            "[{\"name\": \"entryUUID\", \"scimAttribute\": \"urn:example:a\\\"b:Group:entryUUID\"}]",
            "[{\"name\": \"entryUUID\", \"scimAttribute\": \"example:params:Group:entryUUID\"}]",
            "[{\"name\": \"entryUUID\", \"scimAttribute\": \"urn:example::Group:entryUUID\"}]",
            // invalid attribute names, including sub-attributes
            "[{\"name\": \"entryUUID\", \"scimAttribute\": \"" + SCHEMA + ":\"}]",
            "[{\"name\": \"entryUUID\", \"scimAttribute\": \"" + SCHEMA + ":entry.uuid\"}]",
            "[{\"name\": \"entryUUID\", \"scimAttribute\": \"" + SCHEMA + ":1entryUUID\"}]",
            // reserved schemas
            "[{\"name\": \"entryUUID\", \"scimAttribute\": \"urn:ietf:params:scim:schemas:core:2.0:Group:entryUUID\"}]",
            "[{\"name\": \"entryUUID\", \"scimAttribute\": \"urn:ietf:params:scim:schemas:core:2.0:User:entryUUID\"}]",
            "[{\"name\": \"entryUUID\", \"scimAttribute\": \"urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:entryUUID\"}]",
            // reserved group attribute names
            "[{\"name\": \"externalId\", \"scimAttribute\": \"" + SCHEMA + ":externalId\"}]",
            "[{\"name\": \"description\", \"scimAttribute\": \"" + SCHEMA + ":description\"}]",
            // duplicates
            "[{\"name\": \"a\", \"scimAttribute\": \"" + SCHEMA + ":entryUUID\"}, {\"name\": \"b\", \"scimAttribute\": \"" + SCHEMA + ":entryuuid\"}]",
            "[{\"name\": \"a\", \"scimAttribute\": \"" + SCHEMA + ":a\"}, {\"name\": \"a\", \"scimAttribute\": \"" + SCHEMA + ":b\"}]"
    })
    public void testInvalid(String value) {
        assertThrows(ModelValidationException.class, () -> GroupAttributeMappings.parse(value));
    }
}
