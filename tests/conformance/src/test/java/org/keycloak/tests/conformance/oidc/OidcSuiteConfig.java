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

import java.net.URI;
import java.util.List;

import org.keycloak.testframework.conformance.runner.BrowserFlow;
import org.keycloak.testframework.conformance.runner.BrowserInteraction;
import org.keycloak.testframework.conformance.runner.SuiteOptions;
import org.keycloak.tests.conformance.oidc.configs.OidcDynamicRealmConfig;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import static org.keycloak.tests.conformance.oidc.configs.OidcRealmConfig.CLIENT;
import static org.keycloak.tests.conformance.oidc.configs.OidcRealmConfig.CLIENT2;
import static org.keycloak.tests.conformance.oidc.configs.OidcRealmConfig.CLIENT2_SECRET;
import static org.keycloak.tests.conformance.oidc.configs.OidcRealmConfig.CLIENT_SECRET;
import static org.keycloak.tests.conformance.oidc.configs.OidcRealmConfig.CONFORMANCE_CALLBACK;
import static org.keycloak.tests.conformance.oidc.configs.OidcRealmConfig.PASSWORD;
import static org.keycloak.tests.conformance.oidc.configs.OidcRealmConfig.REALM;
import static org.keycloak.tests.conformance.oidc.configs.OidcRealmConfig.USER;

/**
 * The test configuration uploaded to the conformance suite for the OpenID Connect Core plans, using discovery and
 * statically registered clients. The client_secret_post modules use their own client entry; Keycloak's client secret
 * authenticator accepts both client_secret_basic and client_secret_post, so it reuses the first client. In the Dynamic
 * OP plan the suite registers its clients itself, so its configuration has none.
 */
record OidcSuiteConfig(
        String alias,
        Server server,
        Client client,
        Client client2,
        @JsonProperty("client_secret_post") Client clientSecretPost,
        List<BrowserFlow> browser,
        SuiteOptions options) {

    static OidcSuiteConfig create(URI keycloakBaseUri, BrowserInteraction browserInteraction) {
        return new OidcSuiteConfig(
                "keycloak",
                server(keycloakBaseUri, REALM),
                new Client(CLIENT, CLIENT_SECRET),
                new Client(CLIENT2, CLIENT2_SECRET),
                new Client(CLIENT, CLIENT_SECRET),
                browserFlows(REALM, browserInteraction),
                SuiteOptions.WITHOUT_BROWSER_CSS);
    }

    static OidcSuiteConfig createDynamic(URI keycloakBaseUri, BrowserInteraction browserInteraction) {
        return new OidcSuiteConfig(
                "keycloak",
                server(keycloakBaseUri, OidcDynamicRealmConfig.REALM),
                null,
                null,
                null,
                browserFlows(OidcDynamicRealmConfig.REALM, browserInteraction),
                SuiteOptions.WITHOUT_BROWSER_CSS);
    }

    private static Server server(URI keycloakBaseUri, String realm) {
        return new Server(keycloakBaseUri + "/realms/" + realm + "/.well-known/openid-configuration");
    }

    private static List<BrowserFlow> browserFlows(String realm, BrowserInteraction browserInteraction) {
        return browserInteraction.browserFlows(new BrowserInteraction.BrowserContext(
                realm,
                USER,
                PASSWORD,
                CONFORMANCE_CALLBACK));
    }

    JsonNode toJson() {
        return JsonSerialization.writeValueAsNode(this);
    }

    record Server(@JsonProperty("discoveryUrl") String discoveryUrl) {
    }

    record Client(
            @JsonProperty("client_id") String clientId,
            @JsonProperty("client_secret") String clientSecret) {
    }
}
