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

package org.keycloak.models.utils;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author rmartinc
 */
public class KeycloakModelUtilsTest {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );


    @Test
    public void testGenerateId() {
        final String id = KeycloakModelUtils.generateId();
        Assert.assertEquals(36, id.length());
        final String shortId = KeycloakModelUtils.generateShortId(UUID.fromString(id));
        final UUID uuid = fromShortId(shortId);
        Assert.assertEquals(id, uuid.toString());
    }

    @Test
    public void testGenerateShortId() {
        final String shortId = KeycloakModelUtils.generateShortId();
        final UUID uuid = fromShortId(shortId);
        Assert.assertEquals(shortId, KeycloakModelUtils.generateShortId(uuid));
    }

    private UUID fromShortId(String shortId) {
        Assert.assertEquals(22, shortId.length());
        final byte[] bytes = Base64.getUrlDecoder().decode(shortId);
        Assert.assertEquals(Long.BYTES * 2, bytes.length);
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        final long msb = bb.getLong();
        final long lsb = bb.getLong();
        return new UUID(msb, lsb);
    }

    @Test
    public void testUUIDv7() {
        try {
            String id = KeycloakModelUtils.generateIdv7();
            
            // First, validate it's a proper UUID format
            Assert.assertNotNull("Generated ID should not be null", id);
            Assert.assertEquals("Generated ID should be 36 characters", 36, id.length());
            Assert.assertTrue("Generated ID should match UUID format", UUID_PATTERN.matcher(id).matches());
            
            // Parse as UUID to ensure it's valid
            UUID uuid = UUID.fromString(id);
            Assert.assertNotNull("UUID should parse successfully", uuid);
            
            // Direct test for UUID v7 since we're calling generateIdv7() directly
            Assert.assertEquals("Should generate UUID v7", 7, uuid.version());
            
            // Test UUID v7 variant bits (should be 10xxxxxx in binary)
            long lsb = uuid.getLeastSignificantBits();
            long variantBits = (lsb >>> 62) & 0x3;
            Assert.assertEquals("UUID v7 variant bits should be 10 (binary)", 2, variantBits);
            
            // Test timestamp extraction
            long timestamp = uuid.getMostSignificantBits() >>> 16;
            long currentTime = Instant.now().toEpochMilli();
            Assert.assertTrue("Timestamp should be recent", 
                            Math.abs(currentTime - timestamp) < 5000); // Within 5 seconds
            Assert.assertTrue("Timestamp should be positive", timestamp > 0);
            
            // Test that timestamp is reasonable (after year 2020, before year 2100)
            long year2020 = 1577836800000L; // 2020-01-01 00:00:00 UTC
            long year2100 = 4102444800000L; // 2100-01-01 00:00:00 UTC
            Assert.assertTrue("Timestamp should be after 2020", timestamp > year2020);
            Assert.assertTrue("Timestamp should be before 2100", timestamp < year2100);
            
        } catch (IllegalArgumentException e) {
            Assert.fail("Generated ID should be a valid UUID: " + e.getMessage());
        }
    }
}
