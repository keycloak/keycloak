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
package org.keycloak.sdjwt.sdjwtvp;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.common.util.Base64Url;
import org.keycloak.sdjwt.IssuerSignedJWT;
import org.keycloak.sdjwt.SdJwtUtils;
import org.keycloak.sdjwt.vp.SdJwtVP;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * This class will try to test conformity to the spec by comparing json objects.
 * We are facing the situation that:
 * - json produced are not normalized. But we can compare them by matching their
 * content once loaded into a json object.
 * - ecdsa signature contains random component. We can't compare them directly.
 * Even if we had the same input byte
 * - The no rationale for ordering the disclosures. So we can only make sure
 * each of them is present and that the json content matches.
 * Warning: in other to produce the same disclosure strings and hashes like in
 * the spec, i had to produce
 * the same print. This is by no way reliable enough to be used to test
 * conformity to the spec.
 * 
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
public class TestCompareSdJwt {

    public static void compare(SdJwtVP expectedSdJwt, SdJwtVP actualSdJwt) {
        try {
            compareIssuerSignedJWT(expectedSdJwt.getIssuerSignedJWT(), actualSdJwt.getIssuerSignedJWT());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        compareDisclosures(expectedSdJwt, actualSdJwt);

    }

    private static void compareIssuerSignedJWT(IssuerSignedJWT e, IssuerSignedJWT a)
            throws JsonMappingException, JsonProcessingException {

        assertEquals(e.getPayload(), a.getPayload());

        List<String> expectedJwsStrings = Arrays.asList(e.toJws().split("\\."));
        List<String> actualJwsStrings = Arrays.asList(a.toJws().split("\\."));

        // compare json content of header
        assertEquals(toJsonNode(expectedJwsStrings.get(0)), toJsonNode(actualJwsStrings.get(0)));

        // compare payload
        assertEquals(toJsonNode(expectedJwsStrings.get(1)), toJsonNode(actualJwsStrings.get(1)));

        // We wont compare signatures.
    }

    private static void compareDisclosures(SdJwtVP expectedSdJwt, SdJwtVP actualSdJwt) {
        Set<JsonNode> expectedDisclosures = expectedSdJwt.getDisclosuresString().stream()
                .map(TestCompareSdJwt::toJsonNode)
                .collect(Collectors.toSet());
        Set<JsonNode> actualDisclosures = actualSdJwt.getDisclosuresString().stream()
                .map(TestCompareSdJwt::toJsonNode)
                .collect(Collectors.toSet());

        assertEquals(expectedDisclosures.size(), actualDisclosures.size());

        boolean foundEqualPair = false;
        for (JsonNode a : expectedDisclosures) {
            for (JsonNode b : actualDisclosures) {
                if (a.equals(b)) {
                    foundEqualPair = true;
                    break;
                }
            }
        }

        assertTrue("The set should contain equal elements", foundEqualPair);
    }

    private static JsonNode toJsonNode(String base64EncodedString) {
        try {
            return SdJwtUtils.mapper.readTree(Base64Url.decode(base64EncodedString));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
