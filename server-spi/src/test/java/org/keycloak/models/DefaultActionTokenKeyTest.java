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

    @Test
    public void shouldSerializeExpirationAfter2038() {
        long expiration = (long) Integer.MAX_VALUE + 1;
        UUID nonce = UUID.randomUUID();
        DefaultActionTokenKey key = new DefaultActionTokenKey("user-id", "action-id", expiration, nonce);

        DefaultActionTokenKey parsed = DefaultActionTokenKey.from(key.serializeKey());

        Assert.assertNotNull(parsed);
        Assert.assertEquals(Long.valueOf(expiration), parsed.getExp());
        Assert.assertEquals("user-id", parsed.getUserId());
        Assert.assertEquals("action-id", parsed.getActionId());
        Assert.assertEquals(nonce, parsed.getActionVerificationNonce());
    }
}
