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
import java.util.Base64;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author rmartinc
 */
public class KeycloakModelUtilsTest {

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
}
