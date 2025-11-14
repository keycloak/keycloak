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

import org.keycloak.jose.jws.crypto.HashUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
public class SdJwtUtilsTest {
    /**
     * Verify hash production and base 64 url encoding
     * Verify algorithm denomination for keycloak encoding.
     */
    @Test
    public void testHashDisclosure() {
        String expected = "uutlBuYeMDyjLLTpf6Jxi7yNkEF35jdyWMn9U7b_RYY";
    byte[] hash = HashUtils.hash("SHA-256", SdJwtUtils.utf8Bytes("WyI2cU1RdlJMNWhhaiIsICJmYW1pbHlfbmFtZSIsICJNw7ZiaXVzIl0"));
    assertEquals(expected, SdJwtUtils.encodeNoPad(hash));
    }

    /**
     * Verify hash production and base 64 url encoding
     * Verify algorithm denomination for keycloak encoding.
     */
    @Test
    public void testHashDisclosure2() {
        String expected = "w0I8EKcdCtUPkGCNUrfwVp2xEgNjtoIDlOxc9-PlOhs";
    byte[] hash = HashUtils.hash("SHA-256", SdJwtUtils.utf8Bytes("WyJsa2x4RjVqTVlsR1RQVW92TU5JdkNBIiwgIkZSIl0"));
    assertEquals(expected, SdJwtUtils.encodeNoPad(hash));
    }

    /**
     * Test the base64 URL encoding of this json string from the spec,
     * with whitespace between array elements.
     * 
     * ["_26bc4LT-ac6q2KI6cBW5es", "family_name", "Möbius"]
     * 
     * shall produce
     * WyJfMjZiYzRMVC1hYzZxMktJNmNCVzVlcyIsICJmYW1pbHlfbmFtZSIsICJNw7ZiaXVzIl0
     * 
     * There is no padding in the expected string.
     * 
     * see
     * https://drafts.oauth.net/oauth-selective-disclosure-jwt/draft-ietf-oauth-selective-disclosure-jwt.html#section-5.2.1
     * 
     * @throws IOException
     */
    @Test
    public void testBase64urlEncodedObjectWhiteSpacedJsonArray() {
        String input = "[\"_26bc4LT-ac6q2KI6cBW5es\", \"family_name\", \"Möbius\"]";

        // Expected Base64 URL encoded string
        String expected = "WyJfMjZiYzRMVC1hYzZxMktJNmNCVzVlcyIsICJmYW1pbHlfbmFtZSIsICJNw7ZiaXVzIl0";

        // Assert that the base64 URL encoded string from the object matches the
        // expected string
    assertEquals(expected, SdJwtUtils.encodeNoPad(input));
    }

    /**
     * As we are expexting json serializer to behave differently
     * 
     * https://drafts.oauth.net/oauth-selective-disclosure-jwt/draft-ietf-oauth-selective-disclosure-jwt.html#section-5.2.1
     * 
     * @throws IOException
     */
    @Test
    public void testBase64urlEncodedObjectNoWhiteSpacedJsonArray() {
        // Test the base64 URL encoding of this json string from the spec,
        // no whitespace between array elements
        String input = "[\"_26bc4LT-ac6q2KI6cBW5es\",\"family_name\",\"Möbius\"]";

        // Expected Base64 URL encoded string
        String expected = "WyJfMjZiYzRMVC1hYzZxMktJNmNCVzVlcyIsImZhbWlseV9uYW1lIiwiTcO2Yml1cyJd";

        // Assert that the base64 URL encoded string from the object matches the
        // expected string
    assertEquals(expected, SdJwtUtils.encodeNoPad(input));
    }

    @Test
    public void testBase64urlEncodedArrayElementWhiteSpacedJsonArray() {
        String input = "[\"lklxF5jMYlGTPUovMNIvCA\", \"FR\"]";

        // Expected Base64 URL encoded string
        String expected = "WyJsa2x4RjVqTVlsR1RQVW92TU5JdkNBIiwgIkZSIl0";

        // Assert that the base64 URL encoded string from the object matches the
        // expected string
    assertEquals(expected, SdJwtUtils.encodeNoPad(input));
    }

    @Test
    public void testBase64urlEncodedArrayElementNoWhiteSpacedJsonArray() {
        String input = "[\"lklxF5jMYlGTPUovMNIvCA\",\"FR\"]";

        // Expected Base64 URL encoded string
        String expected = "WyJsa2x4RjVqTVlsR1RQVW92TU5JdkNBIiwiRlIiXQ";

        // Assert that the base64 URL encoded string from the object matches the
        // expected string
    assertEquals(expected, SdJwtUtils.encodeNoPad(input));
    }
}
