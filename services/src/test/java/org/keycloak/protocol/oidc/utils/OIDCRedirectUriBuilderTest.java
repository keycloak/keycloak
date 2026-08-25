/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

import java.net.URI;

import jakarta.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for stripping of forbidden OIDC parameters in OIDCRedirectUriBuilder.
 */
public class OIDCRedirectUriBuilderTest {

    @Test
    public void testQueryModeStripsForbiddenParamsFromQuery() {
        OIDCRedirectUriBuilder builder = OIDCRedirectUriBuilder.fromUri("https://client.com/callback?code=evil&legit=keep",
                OIDCResponseMode.QUERY, null, null);
        builder.addParam("code", "real_code");
        Response response = builder.build();
        URI location = response.getLocation();

        String url = location.toString();
        Assert.assertFalse("Attacker's code param should be stripped", url.contains("code=evil"));
        Assert.assertTrue("Legitimate code param should be present", url.contains("code=real_code"));
        Assert.assertTrue("Non-forbidden param should survive", url.contains("legit=keep"));
    }

    @Test
    public void testQueryModeStripsForbiddenParamsFromFragment() {
        OIDCRedirectUriBuilder builder = OIDCRedirectUriBuilder.fromUri("https://client.com/callback#state=evil",
                OIDCResponseMode.QUERY, null, null);
        builder.addParam("state", "real_state");
        Response response = builder.build();
        URI location = response.getLocation();

        String url = location.toString();
        Assert.assertFalse("Attacker's state in fragment should be stripped", url.contains("state=evil"));
        Assert.assertTrue("Legitimate state param should be present", url.contains("state=real_state"));
    }

    @Test
    public void testQueryModeStripsEncodedForbiddenParams() {
        OIDCRedirectUriBuilder builder = OIDCRedirectUriBuilder.fromUri("https://client.com/callback?c%6Fde=evil&legit=keep",
                OIDCResponseMode.QUERY, null, null);
        builder.addParam("code", "real_code");
        Response response = builder.build();
        URI location = response.getLocation();

        String url = location.toString();
        Assert.assertFalse("Percent-encoded forbidden param should be stripped", url.contains("evil"));
        Assert.assertTrue("Legitimate code param should be present", url.contains("code=real_code"));
        Assert.assertTrue("Non-forbidden param should survive", url.contains("legit=keep"));
    }

    @Test
    public void testFragmentModeStripsForbiddenParamsFromFragment() {
        OIDCRedirectUriBuilder builder = OIDCRedirectUriBuilder.fromUri("https://client.com/callback#code=evil&custom=keep",
                OIDCResponseMode.FRAGMENT, null, null);
        builder.addParam("code", "real_code");
        Response response = builder.build();
        URI location = response.getLocation();

        String url = location.toString();
        Assert.assertFalse("Attacker's code in fragment should be stripped", url.contains("code=evil"));
        Assert.assertTrue("Legitimate code param should be present", url.contains("code=real_code"));
        Assert.assertTrue("Non-forbidden fragment param should survive", url.contains("custom=keep"));
    }

    @Test
    public void testFragmentModeStripsForbiddenParamsFromQuery() {
        OIDCRedirectUriBuilder builder = OIDCRedirectUriBuilder.fromUri("https://client.com/callback?code=evil",
                OIDCResponseMode.FRAGMENT, null, null);
        builder.addParam("code", "real_code");
        Response response = builder.build();
        URI location = response.getLocation();

        String url = location.toString();
        Assert.assertFalse("Attacker's code in query should be stripped", url.contains("code=evil"));
        Assert.assertTrue("Legitimate code param should be present", url.contains("code=real_code"));
    }

    @Test
    public void testFormPostModeStripsQueryAndFragmentFromActionUrl() {
        OIDCRedirectUriBuilder builder = OIDCRedirectUriBuilder.fromUri("https://client.com/callback?code=evil#state=evil",
                OIDCResponseMode.FORM_POST, null, null);
        builder.addParam("code", "real_code");
        builder.addParam("state", "real_state");
        Response response = builder.build();
        String html = (String) response.getEntity();

        Assert.assertTrue("Form action should point to callback URL", html.contains("ACTION=\"https://client.com/callback\""));
        Assert.assertFalse("Query params should not appear in form action", html.contains("code=evil"));
        Assert.assertFalse("Fragment should not appear in form action", html.contains("state=evil"));
        Assert.assertTrue("Legitimate code should be in hidden field", html.contains("VALUE=\"real_code\""));
        Assert.assertTrue("Legitimate state should be in hidden field", html.contains("VALUE=\"real_state\""));
    }

    @Test
    public void testFormPostModePreservesNonForbiddenQueryParams() {
        OIDCRedirectUriBuilder builder = OIDCRedirectUriBuilder.fromUri("https://client.com/callback?tab=profile&code=evil",
                OIDCResponseMode.FORM_POST, null, null);
        builder.addParam("code", "real_code");
        Response response = builder.build();
        String html = (String) response.getEntity();

        Assert.assertFalse("Forbidden param should be stripped from action URL", html.contains("code=evil"));
        Assert.assertFalse("Query should be fully stripped from form action", html.contains("tab=profile"));
        Assert.assertTrue("Legitimate code should be in hidden field", html.contains("VALUE=\"real_code\""));
    }

    @Test
    public void testAllForbiddenParamsStrippedFromQuery() {
        OIDCRedirectUriBuilder builder = OIDCRedirectUriBuilder.fromUri(
                "https://client.com/callback?code=a&state=b&iss=c&access_token=d&id_token=e&response=f&session_state=g&legit=keep",
                OIDCResponseMode.QUERY, null, null);
        Response response = builder.build();
        URI location = response.getLocation();

        String url = location.toString();
        Assert.assertFalse("code should be stripped", url.contains("code=a"));
        Assert.assertFalse("state should be stripped", url.contains("state=b"));
        Assert.assertFalse("iss should be stripped", url.contains("iss=c"));
        Assert.assertFalse("access_token should be stripped", url.contains("access_token=d"));
        Assert.assertFalse("id_token should be stripped", url.contains("id_token=e"));
        Assert.assertFalse("response should be stripped", url.contains("response=f"));
        Assert.assertFalse("session_state should be stripped", url.contains("session_state=g"));
        Assert.assertTrue("Non-forbidden param should survive", url.contains("legit=keep"));
    }

    @Test
    public void testCleanUriPassesThroughUnchanged() {
        OIDCRedirectUriBuilder builder = OIDCRedirectUriBuilder.fromUri("https://client.com/callback?tab=profile",
                OIDCResponseMode.QUERY, null, null);
        builder.addParam("code", "real_code");
        Response response = builder.build();
        URI location = response.getLocation();

        String url = location.toString();
        Assert.assertTrue("Non-forbidden query param should survive", url.contains("tab=profile"));
        Assert.assertTrue("Added param should be present", url.contains("code=real_code"));
    }
}
