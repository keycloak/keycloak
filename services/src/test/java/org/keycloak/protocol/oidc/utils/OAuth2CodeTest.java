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

package org.keycloak.protocol.oidc.utils;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class OAuth2CodeTest {

    @Test
    public void testSerializeAndDeserializeWithUserId() {
        OAuth2Code code = new OAuth2Code("code-id", "client-uuid", 1000, "nonce-123", "openid", "resource-1",
                "https://redirect", "challenge", "S256", "dpop", "user-session-id", "user-id-456");

        Assert.assertEquals("user-id-456", code.getUserId());

        Map<String, String> serialized = code.serializeCode();
        Assert.assertEquals("user-id-456", serialized.get(OAuth2Code.USER_ID_NOTE));

        OAuth2Code deserialized = OAuth2Code.deserializeCode(serialized);
        Assert.assertEquals("code-id", deserialized.getId());
        Assert.assertEquals("client-uuid", deserialized.getClientUUID());
        Assert.assertEquals(1000, deserialized.getExpiration());
        Assert.assertEquals("nonce-123", deserialized.getNonce());
        Assert.assertEquals("openid", deserialized.getScope());
        Assert.assertEquals("resource-1", deserialized.getResource());
        Assert.assertEquals("https://redirect", deserialized.getRedirectUriParam());
        Assert.assertEquals("challenge", deserialized.getCodeChallenge());
        Assert.assertEquals("S256", deserialized.getCodeChallengeMethod());
        Assert.assertEquals("dpop", deserialized.getDpopJkt());
        Assert.assertEquals("user-session-id", deserialized.getUserSessionId());
        Assert.assertEquals("user-id-456", deserialized.getUserId());
    }

    @Test
    public void testDeserializeWithoutUserId() {
        Map<String, String> data = new HashMap<>();
        data.put(OAuth2Code.ID_NOTE, "code-id");
        data.put("client_uuid", "client-uuid");
        data.put(OAuth2Code.EXPIRATION_NOTE, "1000");
        data.put("nonce", "nonce-123");
        data.put("scope", "openid");
        data.put("resource", "resource-1");
        data.put("redirectUri", "https://redirect");
        data.put("code_challenge", "challenge");
        data.put("code_challenge_method", "S256");
        data.put("dpop_jkt", "dpop");
        data.put(OAuth2Code.USER_SESSION_ID_NOTE, "user-session-id");

        OAuth2Code deserialized = OAuth2Code.deserializeCode(data);
        Assert.assertEquals("code-id", deserialized.getId());
        Assert.assertEquals("user-session-id", deserialized.getUserSessionId());
        Assert.assertNull(deserialized.getUserId());
    }
}
