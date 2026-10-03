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
package org.keycloak.broker.oauth;

import java.util.Map;

import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link OAuth2IdentityProviderFactory#parseConfig} focused on the RFC 8705
 * {@code mtls_endpoint_aliases} handling for tls_client_auth. A generic OAuth2 IdP imported from a
 * discovery document must preserve the certificate-authenticated endpoints exactly like the OIDC
 * factory does; otherwise backchannel requests would silently use the non-mTLS endpoints.
 */
public class OAuth2IdentityProviderFactoryTest {

    @Test
    public void mtlsEndpointAliasesArePreservedFromDiscovery() {
        String discovery = "{"
                + "\"issuer\":\"https://idp\","
                + "\"authorization_endpoint\":\"https://idp/auth\","
                + "\"token_endpoint\":\"https://idp/token\","
                + "\"userinfo_endpoint\":\"https://idp/userinfo\","
                + "\"introspection_endpoint\":\"https://idp/introspect\","
                + "\"mtls_endpoint_aliases\":{"
                + "  \"token_endpoint\":\"https://mtls.idp/token\","
                + "  \"userinfo_endpoint\":\"https://mtls.idp/userinfo\","
                + "  \"introspection_endpoint\":\"https://mtls.idp/introspect\""
                + "}"
                + "}";

        Map<String, String> config = new OAuth2IdentityProviderFactory().parseConfig(null, discovery);

        assertEquals("https://idp/token", config.get(OAuth2IdentityProviderConfig.TOKEN_ENDPOINT_URL));
        assertEquals("https://mtls.idp/token", config.get(OAuth2IdentityProviderConfig.MTLS_TOKEN_ENDPOINT_URL));
        assertEquals("https://mtls.idp/userinfo", config.get(OAuth2IdentityProviderConfig.MTLS_USER_INFO_URL));
        assertEquals("https://mtls.idp/introspect", config.get(OAuth2IdentityProviderConfig.MTLS_TOKEN_INTROSPECTION_URL));
    }

    @Test
    public void noMtlsAliasesWhenDiscoveryOmitsThem() {
        String discovery = "{"
                + "\"issuer\":\"https://idp\","
                + "\"authorization_endpoint\":\"https://idp/auth\","
                + "\"token_endpoint\":\"https://idp/token\","
                + "\"userinfo_endpoint\":\"https://idp/userinfo\""
                + "}";

        Map<String, String> config = new OAuth2IdentityProviderFactory().parseConfig(null, discovery);

        assertEquals("https://idp/token", config.get(OAuth2IdentityProviderConfig.TOKEN_ENDPOINT_URL));
        // The alias keys are explicitly cleared (set to empty) rather than omitted, so that re-importing a
        // discovery document without mtls_endpoint_aliases overwrites any previously imported aliases instead
        // of leaving stale endpoints behind.
        assertEquals("", config.get(OAuth2IdentityProviderConfig.MTLS_TOKEN_ENDPOINT_URL));
        assertEquals("", config.get(OAuth2IdentityProviderConfig.MTLS_USER_INFO_URL));
        assertEquals("", config.get(OAuth2IdentityProviderConfig.MTLS_TOKEN_INTROSPECTION_URL));
    }
}
