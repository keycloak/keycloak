/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.protocol.oidc.encode;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.keycloak.OAuth2Constants;
import org.keycloak.util.JsonSerialization;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DefaultTokenContextEncoderProviderTest {

    private DefaultTokenContextEncoderProvider provider;

    @Before
    public void before() {
        DefaultTokenContextEncoderProviderFactory factory = new DefaultTokenContextEncoderProviderFactory();
        factory.init(null);
        factory.grantsByShortcuts = new HashMap<>();
        factory.grantsByShortcuts.put("ro", OAuth2Constants.PASSWORD);
        factory.grantsByShortcuts.put("cc", OAuth2Constants.CLIENT_CREDENTIALS);
        factory.grantsByShortcuts.put(DefaultTokenContextEncoderProvider.UNKNOWN, DefaultTokenContextEncoderProvider.UNKNOWN);
        factory.grantsToShortcuts = factory.grantsByShortcuts.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        provider = new DefaultTokenContextEncoderProvider(null, factory);
    }

    @Test
    public void testSuccessClientCredentialsToken() {
        String tokenId = "trltcc:1234";
        AccessTokenContext ctx = provider.getTokenContextFromTokenId(tokenId);
        Assert.assertEquals(ctx.getSessionType(), AccessTokenContext.SessionType.TRANSIENT);
        Assert.assertEquals(ctx.getTokenType(), AccessTokenContext.TokenType.LIGHTWEIGHT);
        Assert.assertEquals(ctx.getGrantType(), OAuth2Constants.CLIENT_CREDENTIALS);
        Assert.assertEquals(ctx.getRawTokenId(), "1234");

        Assert.assertEquals(tokenId, provider.encodeTokenId(ctx));
    }

    @Test
    public void testSuccessOfflineToken() {
        String tokenId = "ofrtro:5678";
        AccessTokenContext ctx = provider.getTokenContextFromTokenId(tokenId);
        Assert.assertEquals(ctx.getSessionType(), AccessTokenContext.SessionType.OFFLINE);
        Assert.assertEquals(ctx.getTokenType(), AccessTokenContext.TokenType.REGULAR);
        Assert.assertEquals(ctx.getGrantType(), OAuth2Constants.PASSWORD);
        Assert.assertEquals(ctx.getRawTokenId(), "5678");

        Assert.assertEquals(tokenId, provider.encodeTokenId(ctx));
    }

    @Test
    public void testJsonSerialization() throws IOException {
        String tokenId = "trltcc:1234";
        AccessTokenContext ctx = provider.getTokenContextFromTokenId(tokenId);

        String s = JsonSerialization.writeValueAsString(ctx);
        AccessTokenContext deserialized = JsonSerialization.readValue(s, AccessTokenContext.class);
        Assert.assertEquals(ctx, deserialized);
    }

    @Test
    public void testIncorrectGrantType() {
        try {
            String tokenId = "ofrtac:5678";
            AccessTokenContext ctx = provider.getTokenContextFromTokenId(tokenId);
            Assert.fail("Not expected to success due incorrect grant type");
        } catch (RuntimeException iae) {
            // ignored
        }
    }

    @Test
    public void testUnknownGrantType() {
        String tokenId = "onrtna:5678";
        AccessTokenContext ctx = provider.getTokenContextFromTokenId(tokenId);
        Assert.assertEquals(ctx.getSessionType(), AccessTokenContext.SessionType.ONLINE);
        Assert.assertEquals(ctx.getTokenType(), AccessTokenContext.TokenType.REGULAR);
        Assert.assertEquals(ctx.getGrantType(), DefaultTokenContextEncoderProvider.UNKNOWN);
        Assert.assertEquals(ctx.getRawTokenId(), "5678");

        Assert.assertEquals(tokenId, provider.encodeTokenId(ctx));
    }

    @Test
    public void testOldToken() {
        AccessTokenContext ctx = provider.getTokenContextFromTokenId("1234");
        Assert.assertEquals(ctx.getSessionType(), AccessTokenContext.SessionType.UNKNOWN);
        Assert.assertEquals(ctx.getTokenType(), AccessTokenContext.TokenType.UNKNOWN);
        Assert.assertEquals(ctx.getGrantType(), DefaultTokenContextEncoderProvider.UNKNOWN);
        Assert.assertEquals(ctx.getRawTokenId(), "1234");

        try {
            provider.encodeTokenId(ctx);
            Assert.fail("Should not be possible to encode from ctx with unknown types");
        } catch (IllegalStateException expected) {
            // ignore
        }
    }
}
