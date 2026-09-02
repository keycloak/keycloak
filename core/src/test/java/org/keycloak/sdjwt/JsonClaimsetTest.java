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
package org.keycloak.sdjwt;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
public class JsonClaimsetTest {

    @Test
    public void testRead61ClaimSet() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("sdjwt/s6.1-holder-claims.json");
        JsonNode claimSet = SdJwtUtils.mapper.readTree(is);

        // Test reading a String
        String expected_sub_claim = "user_42";
        JsonNode sub_claim = claimSet.get("sub");
        assertEquals(JsonNodeType.STRING, sub_claim.getNodeType());
        assertEquals(expected_sub_claim, sub_claim.asText());

        // Test reading a boolean
        JsonNode phone_number_verified_claim = claimSet.get("phone_number_verified");
        Boolean expected_phone_number_verified_claim = true;
        assertEquals(JsonNodeType.BOOLEAN, phone_number_verified_claim.getNodeType());
        assertEquals(expected_phone_number_verified_claim, phone_number_verified_claim.asBoolean());

        // Test reading an object
        JsonNode address_claim = claimSet.get("address");
        assertEquals(JsonNodeType.OBJECT, address_claim.getNodeType());
        JsonNode street_address_claim = address_claim.get("street_address");
        assertEquals(JsonNodeType.STRING, street_address_claim.getNodeType());
        String expected_street_address_claim = "123 Main St";
        assertEquals(expected_street_address_claim, street_address_claim.asText());

        // Test reading a number
        JsonNode updated_at_claim = claimSet.get("updated_at");
        int expected_updated_at_claim = 1570000000;
        assertEquals(JsonNodeType.NUMBER, updated_at_claim.getNodeType());
        assertEquals(expected_updated_at_claim, updated_at_claim.asInt());

        // Test reading an array
        JsonNode nationalities_claim = claimSet.get("nationalities");
        assertEquals(JsonNodeType.ARRAY, nationalities_claim.getNodeType());
        assertEquals(2, nationalities_claim.size());
        JsonNode element_0_nationalities_claim = nationalities_claim.get(0);
        assertEquals(JsonNodeType.STRING, element_0_nationalities_claim.getNodeType());
        String expected_element_0_nationalities_claim = "US";
        assertEquals(expected_element_0_nationalities_claim, element_0_nationalities_claim.asText());
        JsonNode element_1_nationalities_claim = nationalities_claim.get(1);
        assertEquals(JsonNodeType.STRING, element_1_nationalities_claim.getNodeType());
        String expected_element_1_nationalities_claim = "DE";
        assertEquals(expected_element_1_nationalities_claim, element_1_nationalities_claim.asText());
    }
}
