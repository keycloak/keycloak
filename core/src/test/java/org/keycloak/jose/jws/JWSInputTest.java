/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.jose.jws;

import java.nio.charset.StandardCharsets;

import org.keycloak.common.util.Base64Url;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

public class JWSInputTest {

    private static final String HEADER = Base64Url.encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
    private static final String PAYLOAD = Base64Url.encode("{\"sub\":\"alice\"}".getBytes(StandardCharsets.UTF_8));

    // 32 bytes: 43 base64url chars, the last one carrying 2 unused bits
    private static final byte[] SIGNATURE = new byte[] {
            (byte) 0xfb, (byte) 0xff, 0x3e, 0x01, 0x02, 0x03, 0x04, 0x05,
            0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d,
            0x0e, 0x0f, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15,
            0x16, 0x17, 0x18, 0x19, 0x1a, 0x1b, (byte) 0xfe, (byte) 0xf0 };
    private static final String ENCODED_SIGNATURE = Base64Url.encode(SIGNATURE);

    @Test
    public void acceptsCanonicalSignature() throws Exception {
        JWSInput input = new JWSInput(HEADER + "." + PAYLOAD + "." + ENCODED_SIGNATURE);
        assertArrayEquals(SIGNATURE, input.getSignature());
        assertEquals(ENCODED_SIGNATURE, input.getEncodedSignature());
        assertEquals(HEADER + "." + PAYLOAD, input.getEncodedSignatureInput());
    }

    @Test
    public void acceptsUnsignedToken() throws Exception {
        JWSInput input = new JWSInput(HEADER + "." + PAYLOAD + ".");
        assertNull(input.getSignature());
    }

    @Test
    public void rejectsPaddedSignature() {
        assertRejected(ENCODED_SIGNATURE + "=");
    }

    @Test
    public void rejectsTrailingDataAfterPadding() {
        assertRejected(ENCODED_SIGNATURE + "=anything-at-all");
    }

    @Test
    public void rejectsStandardBase64Alphabet() {
        String standard = ENCODED_SIGNATURE.replace('-', '+').replace('_', '/');
        assertRejected(standard);
    }

    @Test
    public void rejectsNonZeroUnusedBits() {
        char last = ENCODED_SIGNATURE.charAt(ENCODED_SIGNATURE.length() - 1);
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
        // same top 4 bits, different value in the 2 unused low bits
        char other = alphabet.charAt(alphabet.indexOf(last) ^ 1);
        assertRejected(ENCODED_SIGNATURE.substring(0, ENCODED_SIGNATURE.length() - 1) + other);
    }

    @Test
    public void rejectsInvalidLength() {
        // 45 characters, one more than a whole quantum can encode
        assertRejected(ENCODED_SIGNATURE + "AA");
    }

    private static void assertRejected(String encodedSignature) {
        assertThrows(JWSInputException.class, () -> new JWSInput(HEADER + "." + PAYLOAD + "." + encodedSignature));
    }
}
