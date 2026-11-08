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
package org.keycloak.models;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

public class DefaultActionTokenKeyTest {

    // A timestamp beyond the 2038 int overflow that Time.currentTimeSeconds() can return
    private static final long POST_2038_EXPIRATION = (long) Integer.MAX_VALUE + 300;

    @Test
    public void longExpirationIsKept() {
        DefaultActionTokenKey key = new DefaultActionTokenKey("user-id", "action-id", POST_2038_EXPIRATION, UUID.randomUUID());

        Assert.assertEquals(Long.valueOf(POST_2038_EXPIRATION), key.getExp());
    }

    @Test
    public void intExpirationStillWorks() {
        int expiration = 1_800_000_000;
        DefaultActionTokenKey key = new DefaultActionTokenKey("user-id", "action-id", expiration, UUID.randomUUID());

        Assert.assertEquals(Long.valueOf(expiration), key.getExp());
    }

    @Test
    public void serializedKeyRoundTripsLongExpiration() {
        DefaultActionTokenKey key = new DefaultActionTokenKey("user-id", "action-id", POST_2038_EXPIRATION, UUID.randomUUID());

        DefaultActionTokenKey parsed = DefaultActionTokenKey.from(key.serializeKey());

        Assert.assertEquals(Long.valueOf(POST_2038_EXPIRATION), parsed.getExp());
        Assert.assertEquals(key.getActionVerificationNonce(), parsed.getActionVerificationNonce());
        Assert.assertEquals("user-id", parsed.getUserId());
        Assert.assertEquals("action-id", parsed.getActionId());
    }
}
