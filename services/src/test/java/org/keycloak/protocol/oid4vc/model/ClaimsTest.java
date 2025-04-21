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
package org.keycloak.protocol.oid4vc.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.keycloak.util.JsonSerialization;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
public class ClaimsTest {

    @Test
    public void toJsonString() throws JsonProcessingException {
        Claims claims = new Claims();
        claims.put("firstName", new Claim());
        claims.put("lastName", new Claim());
        claims.put("email", new Claim());
        String jsonString = claims.toJsonString();
        JsonNode jsonNode = JsonSerialization.mapper.readTree(jsonString);
        assertNotNull(jsonNode.get("firstName"));
        assertNotNull(jsonNode.get("lastName"));
        assertNotNull(jsonNode.get("email"));
    }

    @Test
    public void fromJsonString() {
        final String serializeForm = "{ \"firstName\": {}, \"lastName\": {}, \"email\": {} }";
        Claims claims = Claims.fromJsonString(serializeForm);
        assertNotNull(claims);
        assertNotNull(claims.get("firstName"));
        assertNotNull(claims.get("lastName"));
        assertNotNull(claims.get("email"));
    }

    @Test
    public void fromJsonStringDeepClaim() {
        final String serializeForm = "{ \"firstName\": {\"mandatory\":false}, \"lastName\": {\"mandatory\":false}, \"email\": {\"mandatory\":true} }";
        Claims claims = Claims.fromJsonString(serializeForm);
        assertNotNull(claims);
        assertNotNull(claims.get("firstName"));
        assertFalse(claims.get("firstName").getMandatory());
        assertNotNull(claims.get("lastName"));
        assertFalse(claims.get("lastName").getMandatory());
        assertNotNull(claims.get("email"));
        assertTrue(claims.get("email").getMandatory());
    }
}
