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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
        Assertions.assertFalse(url.contains("code=evil"), "Attacker's code param should be stripped");
        Assertions.assertTrue(url.contains("code=real_code"), "Legitimate code param should be present");
        Assertions.assertTrue(url.contains("legit=keep"), "Non-forbidden param should survive");
    }

    @Test
    public void testQueryModeStripsForbiddenParamsFromFragment() {
        OIDCRedirectUriBuilder builder = OIDCRedirectUriBuilder.fromUri("https://client.com/callback#state=evil",
                OIDCResponseMode.QUERY, null, null);
        builder.addParam("state", "real_state");
        Response response = builder.build();
        URI location = response.getLocation();

        String url = location.toString();
        Assertions.assertFalse(url.contains("state=evil"), "Attacker's state in fragment should be stripped");
        Assertions.assertTrue(url.contains("state=real_state"), "Legitimate state param should be present");
    }

    @Test
    public void testQueryModeStripsEncodedForbiddenParams() {
        OIDCRedirectUriBuilder builder = OIDCRedirectUriBuilder.fromUri("https://client.com/callback?c%6Fde=evil&legit=keep",
                OIDCResponseMode.QUERY, null, null);
        builder.addParam("code", "real_code");
        Response response = builder.build();
        URI location = response.getLocation();

        String url = location.toString();
        Assertions.assertFalse(url.contains("c%6Fde=evil"), "Percent-encoded forbidden param should be stripped");
        Assertions.assertTrue(url.contains("code=real_code"), "Legitimate code param should be present");
        Assertions.assertTrue(url.contains("legit=keep"), "Non-forbidden param should survive");
    }

    @Test
    public void testFragmentModeStripsForbiddenParamsFromFragment() {
        OIDCRedirectUriBuilder builder = OIDCRedirectUriBuilder.fromUri("https://client.com/callback#code=evil&custom=keep",
                OIDCResponseMode.FRAGMENT, null, null);
        builder.addParam("code", "real_code");
        Response response = builder.build();
        URI location = response.getLocation();

        String url = location.toString();
        Assertions.assertFalse(url.contains("code=evil"), "Attacker's code in fragment should be stripped");
        Assertions.assertTrue(url.contains("code=real_code"), "Legitimate code param should be present");
        Assertions.assertTrue(url.contains("custom=keep"), "Non-forbidden fragment param should survive");
    }

    @Test
    public void testFragmentModeStripsForbiddenParamsFromQuery() {
        OIDCRedirectUriBuilder builder = OIDCRedirectUriBuilder.fromUri("https://client.com/callback?code=evil",
                OIDCResponseMode.FRAGMENT, null, null);
        builder.addParam("code", "real_code");
        Response response = builder.build();
        URI location = response.getLocation();

        String url = location.toString();
        Assertions.assertFalse(url.contains("code=evil"), "Attacker's code in query should be stripped");
        Assertions.assertTrue(url.contains("code=real_code"), "Legitimate code param should be present");
    }

    @Test
    public void testFormPostModeStripsQueryAndFragmentFromActionUrl() {
        OIDCRedirectUriBuilder builder = OIDCRedirectUriBuilder.fromUri("https://client.com/callback?code=evil#state=evil",
                OIDCResponseMode.FORM_POST, null, null);
        builder.addParam("code", "real_code");
        builder.addParam("state", "real_state");
        Response response = builder.build();
        String html = (String) response.getEntity();

        Assertions.assertTrue(html.contains("ACTION=\"https://client.com/callback\""), "Form action should point to callback URL");
        Assertions.assertFalse(html.contains("code=evil"), "Forbidden code param should be stripped by fromUri");
        Assertions.assertFalse(html.contains("state=evil"), "Forbidden state param should be stripped by fromUri");
        Assertions.assertTrue(html.contains("VALUE=\"real_code\""), "Legitimate code should be in hidden field");
        Assertions.assertTrue(html.contains("VALUE=\"real_state\""), "Legitimate state should be in hidden field");
    }

    @Test
    public void testFormPostModePreservesNonForbiddenQueryParams() {
        OIDCRedirectUriBuilder builder = OIDCRedirectUriBuilder.fromUri("https://client.com/callback?tab=profile&code=evil",
                OIDCResponseMode.FORM_POST, null, null);
        builder.addParam("code", "real_code");
        Response response = builder.build();
        String html = (String) response.getEntity();

        Assertions.assertFalse(html.contains("code=evil"), "Forbidden param should be stripped from action URL");
        Assertions.assertTrue(html.contains("tab=profile"), "Non-forbidden query param should survive in form action");
        Assertions.assertTrue(html.contains("VALUE=\"real_code\""), "Legitimate code should be in hidden field");
    }

    @Test
    public void testAllForbiddenParamsStrippedFromQuery() {
        OIDCRedirectUriBuilder builder = OIDCRedirectUriBuilder.fromUri(
                "https://client.com/callback?code=a&state=b&iss=c&access_token=d&id_token=e&response=f&session_state=g&legit=keep",
                OIDCResponseMode.QUERY, null, null);
        Response response = builder.build();
        URI location = response.getLocation();

        String url = location.toString();
        Assertions.assertFalse(url.contains("code=a"), "code should be stripped");
        Assertions.assertFalse(url.contains("state=b"), "state should be stripped");
        Assertions.assertFalse(url.contains("iss=c"), "iss should be stripped");
        Assertions.assertFalse(url.contains("access_token=d"), "access_token should be stripped");
        Assertions.assertFalse(url.contains("id_token=e"), "id_token should be stripped");
        Assertions.assertFalse(url.contains("response=f"), "response should be stripped");
        Assertions.assertFalse(url.contains("session_state=g"), "session_state should be stripped");
        Assertions.assertTrue(url.contains("legit=keep"), "Non-forbidden param should survive");
    }

    @Test
    public void testCleanUriPassesThroughUnchanged() {
        OIDCRedirectUriBuilder builder = OIDCRedirectUriBuilder.fromUri("https://client.com/callback?tab=profile",
                OIDCResponseMode.QUERY, null, null);
        builder.addParam("code", "real_code");
        Response response = builder.build();
        URI location = response.getLocation();

        String url = location.toString();
        Assertions.assertTrue(url.contains("tab=profile"), "Non-forbidden query param should survive");
        Assertions.assertTrue(url.contains("code=real_code"), "Added param should be present");
    }
}
