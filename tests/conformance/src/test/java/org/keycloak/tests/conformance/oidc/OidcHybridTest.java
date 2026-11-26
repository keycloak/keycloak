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

package org.keycloak.tests.conformance.oidc;

import java.util.stream.Stream;

import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.conformance.runner.BrowserInteraction;
import org.keycloak.testframework.conformance.runner.ConformanceModuleVariant;
import org.keycloak.testframework.conformance.runner.ConformanceResult;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.tests.conformance.ConformanceServerConfig;
import org.keycloak.tests.conformance.oidc.configs.OidcRealmConfig;

import static org.keycloak.tests.conformance.oidc.configs.OidcRealmConfig.HYBRID_PLAN;

/**
 * The Hybrid OP certification plan (response types {@code code id_token}, {@code code token} and
 * {@code code id_token token}).
 */
@KeycloakIntegrationTest(config = ConformanceServerConfig.class)
public class OidcHybridTest extends AbstractOidcConformanceTest {

    @InjectRealm(config = OidcRealmConfig.class)
    ManagedRealm realm;

    @Override
    protected String plan() {
        return HYBRID_PLAN;
    }

    @Override
    protected Stream<ConformanceModuleVariant> moduleVariants() {
        return Stream.of(
                modules(ConformanceResult.PASSED, BrowserInteraction.LOGIN,
                        "oidcc-userinfo-get",
                        "oidcc-userinfo-post-header",
                        "oidcc-userinfo-post-body",
                        "oidcc-ensure-request-without-nonce-succeeds-for-code-flow",
                        "oidcc-scope-profile",
                        "oidcc-scope-address",
                        "oidcc-scope-phone",
                        "oidcc-scope-all",
                        "oidcc-display-page",
                        "oidcc-display-popup",
                        "oidcc-prompt-none-logged-in",
                        "oidcc-max-age-10000",
                        "oidcc-ensure-request-with-unknown-parameter-succeeds",
                        "oidcc-id-token-hint",
                        "oidcc-login-hint",
                        "oidcc-ui-locales",
                        "oidcc-claims-locales",
                        "oidcc-ensure-request-with-acr-values-succeeds",
                        "oidcc-codereuse",
                        "oidcc-codereuse-30seconds",
                        "oidcc-server-client-secret-post",
                        "oidcc-unsigned-request-object-supported-correctly-or-rejected-as-unsupported",
                        "oidcc-ensure-request-object-with-redirect-uri",
                        "oidcc-refresh-token",
                        "oidcc-ensure-request-with-valid-pkce-succeeds"),
                // Pass with the script based ID token mapper of the realm, see OidcRealmConfig
                modules(ConformanceResult.PASSED, BrowserInteraction.LOGIN,
                        "oidcc-server",
                        "oidcc-scope-email",
                        "oidcc-alternate-happy-flow",
                        "oidcc-claims-essential"),
                modules(ConformanceResult.REVIEW, BrowserInteraction.LOGIN_WITH_SCREENSHOT_ON_SECOND_LOGIN,
                        "oidcc-prompt-login",
                        "oidcc-max-age-1"),
                modules(ConformanceResult.PASSED, BrowserInteraction.ERROR_CALLBACK,
                        "oidcc-response-type-missing",
                        "oidcc-ensure-request-without-nonce-fails",
                        "oidcc-prompt-none-not-logged-in"),
                modules(ConformanceResult.REVIEW, BrowserInteraction.errorPage("Invalid parameter: redirect_uri"),
                        "oidcc-ensure-registered-redirect-uri"))
                .flatMap(group -> group);
    }
}
