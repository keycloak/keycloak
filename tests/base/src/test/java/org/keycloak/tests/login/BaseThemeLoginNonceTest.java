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

import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.tests.utils.InlineScriptNonceUtil;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Asserts that the "base" login theme templates carry a CSP nonce on their inline scripts,
 * as the default theme is "keycloak.v2" when the LOGIN_V2 feature is enabled.
 */
@KeycloakIntegrationTest
public class BaseThemeLoginNonceTest {

    @InjectRealm(config = BaseThemeRealmConfig.class)
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauth;

    @Test
    public void testBaseThemeContainsNoncesInScriptTags() throws IOException {
        String body1 = getLoginPage();
        String body2 = getLoginPage();

        var inlineScriptsWithoutNonce = InlineScriptNonceUtil.getInlineScriptTagsWithoutNonce(body1);
        assertTrue(inlineScriptsWithoutNonce.isEmpty(),
                () -> String.format("Page contains %d scripts without nonce: %s", inlineScriptsWithoutNonce.size(), inlineScriptsWithoutNonce));

        var nonces1 = InlineScriptNonceUtil.getScriptNonceValues(body1);
        var nonces2 = InlineScriptNonceUtil.getScriptNonceValues(body2);

        assertFalse(nonces1.isEmpty(), "Page should contain at least one inline script with a nonce");
        assertEquals(1, nonces1.size());
        assertNotEquals(nonces1, nonces2);
    }

    private String getLoginPage() throws IOException {
        var response = oauth.httpClient().get().execute(new HttpGet(oauth.loginForm().build()));
        assertEquals(200, response.getStatusLine().getStatusCode());
        return EntityUtils.toString(response.getEntity());
    }

    static class BaseThemeRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.loginTheme("base");
        }

    }

}
