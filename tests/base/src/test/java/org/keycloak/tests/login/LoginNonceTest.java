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

import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.tests.model.CustomProvidersServerConfig;

import org.junit.jupiter.api.Test;

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
}
