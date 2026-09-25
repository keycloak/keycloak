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
package org.keycloak.tests.login;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.tests.common.CustomProvidersServerConfig;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = CustomProvidersServerConfig.class)
public class LoginNonceTest {

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    protected ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectPage
    protected LoginPage loginPage;

    @Test
    public void testTemplateContainsNoncesInScriptTags() {
        oauth.openLoginForm();
        loginPage.assertCurrent();

        var inlineScriptsWithoutNonce = loginPage.getInlineScriptsWithoutNonce();

        assertTrue(
                inlineScriptsWithoutNonce.isEmpty(),
                String.format("Page contains %d scripts without nonce: %s",
                        inlineScriptsWithoutNonce.size(),
                        inlineScriptsWithoutNonce.stream()
                                .map(s -> s.getAttribute("outerHTML"))
                                .toList()
                )
        );
    }

    @Test
    public void testMultiplePageInvocationsContainDifferentNonces() {
        oauth.openLoginForm();
        loginPage.assertCurrent();
        var nonces1 = getNonces(loginPage);

        oauth.openLoginForm();
        loginPage.assertCurrent();
        var nonces2 = getNonces(loginPage);

        assertEquals(1, nonces1.size());
        assertEquals(1, nonces2.size());
        assertNotEquals(nonces1, nonces2);
    }

    @Test
    public void testLoginPageNotCacheable() throws IOException {
        var response = oauth.httpClient().get().execute(new HttpGet(oauth.loginForm().build()));
        try {
            assertEquals(200, response.getStatusLine().getStatusCode());
            String cacheControl = response.getFirstHeader("Cache-Control").getValue();
            assertFalse(cacheControl.matches(".*max-age=[1-9][0-9]*.*"), "Login page must not be cacheable, was: " + cacheControl);
            assertTrue(cacheControl.contains("no-store"), "Login page must not be cacheable, was: " + cacheControl);
        } finally {
            EntityUtils.consume(response.getEntity());
        }
    }

    private Set<String> getNonces(LoginPage loginPage) {
        return loginPage.getScripts()
                .stream()
                .map(s -> s.getDomAttribute("nonce"))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
