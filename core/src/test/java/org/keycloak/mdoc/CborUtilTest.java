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
package org.keycloak.mdoc;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CborUtilTest {

    @Test
    public void shouldEncodeIntegerMapAsDefiniteMinimalCbor() {
        Map<Integer, Integer> value = new HashMap<>();
        value.put(1, -7);

        // a1 opens a map with exactly one entry, 01 is the integer key 1, 26 the value negative 7
        assertCborHex("a1 01 26", CborUtil.encodeIntegerMap(value));
    }

    @Test
    public void shouldSortIntegerMapKeysBeforeEncoding() {
        Map<Integer, Integer> value = new HashMap<>();
        value.put(33, -257);
        value.put(1, -7);

        // a2 opens a map with exactly two entries, key 1 with value negative 7 comes before
        // key 33 (18 21) with value negative 257 (39 0100)
        assertCborHex("a2 01 26 1821 390100", CborUtil.encodeIntegerMap(value));
    }

    @Test
    public void shouldEncodeMapsWithDefiniteLength() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("a", 1);

        // a1 opens a map with exactly one entry, 61 61 is the text key "a", 01 the value 1.
        // An indefinite length map would start with bf instead, which ISO mdoc forbids.
        assertCborHex("a1 6161 01", CborUtil.encode(value));
    }

    @Test
    public void shouldEncodeIntegerMapKeysAsCborIntegers() {
        Map<Integer, Integer> value = new LinkedHashMap<>();
        value.put(1, 2);

        // a1 opens a map with exactly one entry, 01 is the integer key 1, 02 the value 2
        assertCborHex("a1 01 02", CborUtil.encode(value));
    }

    @Test
    public void shouldTruncateTdateToWholeSeconds() {
        CborUtil.Tagged tdate = CborUtil.tdate(Instant.parse("2025-07-01T20:00:00.123Z"));

        assertEquals("2025-07-01T20:00:00Z", tdate.value());
    }

    private static void assertCborHex(String expectedHex, byte[] actual) {
        StringBuilder actualHex = new StringBuilder();
        for (byte b : actual) {
            actualHex.append(String.format("%02x", b));
        }
        assertEquals(expectedHex.replace(" ", ""), actualHex.toString());
    }
}
