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
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Optional;

import org.keycloak.common.VerificationException;
import org.keycloak.common.util.Base64Url;
import org.keycloak.jose.jws.crypto.HashUtils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 *
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
public class SdJwtUtils {

    public static final ObjectMapper mapper = new ObjectMapper();
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String encodeNoPad(byte[] bytes) {
        return Base64Url.encode(bytes);
    }

    public static String encodeNoPad(String input) {
        return encodeNoPad(utf8Bytes(input));
    }

    public static byte[] decodeNoPad(String encoded) {
        return Base64Url.decode(encoded);
    }

    public static String hashAndBase64EncodeNoPad(byte[] disclosureBytes, String hashAlg) {
        return encodeNoPad(HashUtils.hash(hashAlg, disclosureBytes));
    }

    public static String hashAndBase64EncodeNoPad(String disclosure, String hashAlg) {
        return hashAndBase64EncodeNoPad(utf8Bytes(disclosure), hashAlg);
    }

    public static byte[] utf8Bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    public static String requireNonEmpty(String str, String message) {
        return Optional.ofNullable(str)
                .filter(s -> !s.isEmpty())
                .orElseThrow(() -> new IllegalArgumentException(message));
    }

    public static String randomSalt() {
        // 16 bytes for 128-bit entropy.
        // Base64url-encoded
        return encodeNoPad(randomBytes(16));
    }

    public static byte[] randomBytes(int size) {
        byte[] bytes = new byte[size];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    public static String printJsonArray(Object[] array) throws JsonProcessingException {
        if (arrayEltSpaced) {
            return arraySpacedPrettyPrinter.writer.writeValueAsString(array);
        } else {
            return mapper.writeValueAsString(array);
        }
    }

    public static ArrayNode decodeDisclosureString(String disclosure) throws VerificationException {
        JsonNode jsonNode;

    // Decode Base64URL-encoded disclosure using UTF-8
    String decoded = new String(decodeNoPad(disclosure), StandardCharsets.UTF_8);

        // Parse the disclosure string into a JSON array
        try {
            jsonNode = mapper.readTree(decoded);
        } catch (JsonProcessingException e) {
            throw new VerificationException("Disclosure is not a valid JSON", e);
        }

        // Check if the parsed JSON is an array
        if (!jsonNode.isArray()) {
            throw new VerificationException("Disclosure is not a JSON array");
        }

        return (ArrayNode) jsonNode;
    }

    public static long readTimeClaim(JsonNode payload, String claimName) throws VerificationException {
        JsonNode claim = payload.get(claimName);
        if (claim == null || !claim.isNumber()) {
            throw new VerificationException("Missing or invalid '" + claimName + "' claim");
        }

        return claim.asLong();
    }

    public static String readClaim(JsonNode payload, String claimName) throws VerificationException {
        JsonNode claim = payload.get(claimName);
        if (claim == null) {
            throw new VerificationException("Missing '" + claimName + "' claim");
        }

        return claim.textValue();
    }

    public static JsonNode deepClone(JsonNode node) {
        try {
            byte[] serializedNode = mapper.writeValueAsBytes(node);
            return mapper.readTree(serializedNode);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static ArraySpacedPrettyPrinter arraySpacedPrettyPrinter = new ArraySpacedPrettyPrinter();

    static class ArraySpacedPrettyPrinter extends MinimalPrettyPrinter {
        final ObjectMapper prettyPrinObjectMapper;
        final ObjectWriter writer;

        public ArraySpacedPrettyPrinter() {
            prettyPrinObjectMapper = new ObjectMapper();
            prettyPrinObjectMapper.setDefaultPrettyPrinter(this);
            writer = prettyPrinObjectMapper.writer(this);
        }

        @Override
        public void writeArrayValueSeparator(JsonGenerator jg) throws IOException {
            jg.writeRaw(',');
            jg.writeRaw(' ');
        }

        @Override
        public void writeObjectEntrySeparator(JsonGenerator jg) throws IOException {
            jg.writeRaw(',');
            jg.writeRaw(' '); // Add a space after comma
        }

        @Override
        public void writeObjectFieldValueSeparator(JsonGenerator jg) throws IOException {
            jg.writeRaw(':');
            jg.writeRaw(' '); // Add a space after comma
        }
    }

    public static boolean arrayEltSpaced = true;
}
