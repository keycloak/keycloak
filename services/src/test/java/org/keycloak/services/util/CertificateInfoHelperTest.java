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

package org.keycloak.services.util;

import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CertificateInfoHelperTest {

    @Test
    public void stripsPrivateMaterialFromEveryJwkAndPreservesPublicParameters() throws Exception {
        String jwks = """
                {"keys":[
                  {"kty":"RSA","kid":"rsa","n":"modulus","e":"AQAB",
                   "d":"private","p":"prime","q":"prime","dp":"exponent","dq":"exponent","qi":"coefficient",
                   "oth":[{"r":"prime","d":"exponent","t":"coefficient"}]},
                  {"kty":"EC","kid":"ec","crv":"P-256","x":"public-x","y":"public-y","d":"private"},
                  {"kty":"OKP","kid":"okp","crv":"Ed25519","x":"public-x","d":"private"},
                  {"kty":"oct","kid":"symmetric","k":"secret"}
                ]}
                """;
        String expected = """
                {"keys":[
                  {"kty":"RSA","kid":"rsa","n":"modulus","e":"AQAB"},
                  {"kty":"EC","kid":"ec","crv":"P-256","x":"public-x","y":"public-y"},
                  {"kty":"OKP","kid":"okp","crv":"Ed25519","x":"public-x"},
                  {"kty":"oct","kid":"symmetric"}
                ]}
                """;

        String sanitized = CertificateInfoHelper.stripPrivateKeyParams(jwks);

        assertEquals(JsonSerialization.readValue(expected, JsonNode.class),
                JsonSerialization.readValue(sanitized, JsonNode.class));
    }
}
