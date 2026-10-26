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

package org.keycloak.broker.oidc;

import java.lang.reflect.Proxy;
import java.util.Map;

import org.keycloak.common.enums.SslRequired;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.representations.MTLSEndpointAliases;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OAuth2IdentityProviderConfigMtlsTest {

    private static OAuth2IdentityProviderConfig config(String clientAuthMethod) {
        OAuth2IdentityProviderConfig config = new OAuth2IdentityProviderConfig();
        config.setClientAuthMethod(clientAuthMethod);
        config.setTokenUrl("https://idp/token");
        config.setTokenIntrospectionUrl("https://idp/introspect");
        config.setUserInfoUrl("https://idp/userinfo");
        config.setMtlsTokenUrl("https://mtls.idp/token");
        config.setMtlsTokenIntrospectionUrl("https://mtls.idp/introspect");
        config.setMtlsUserInfoUrl("https://mtls.idp/userinfo");
        return config;
    }

    @Test
    public void mtlsUrlsAreUsedWithTlsClientAuth() {
        OAuth2IdentityProviderConfig config = config(OIDCLoginProtocol.TLS_CLIENT_AUTH);
        assertEquals("https://mtls.idp/token", config.getEffectiveTokenUrl());
        assertEquals("https://mtls.idp/introspect", config.getEffectiveTokenIntrospectionUrl());
        assertEquals("https://mtls.idp/userinfo", config.getEffectiveUserInfoUrl());
    }

    @Test
    public void regularUrlsAreUsedWithOtherClientAuth() {
        OAuth2IdentityProviderConfig config = config(OIDCLoginProtocol.CLIENT_SECRET_POST);
        assertEquals("https://idp/token", config.getEffectiveTokenUrl());
        assertEquals("https://idp/introspect", config.getEffectiveTokenIntrospectionUrl());
        assertEquals("https://idp/userinfo", config.getEffectiveUserInfoUrl());
    }

    @Test
    public void regularUrlsAreUsedWhenNoMtlsUrlIsConfigured() {
        OAuth2IdentityProviderConfig config = config(OIDCLoginProtocol.TLS_CLIENT_AUTH);
        config.setMtlsTokenUrl("");
        config.getConfig().remove(OAuth2IdentityProviderConfig.MTLS_TOKEN_INTROSPECTION_URL);
        assertEquals("https://idp/token", config.getEffectiveTokenUrl());
        assertEquals("https://idp/introspect", config.getEffectiveTokenIntrospectionUrl());
    }

    @Test
    public void mtlsEndpointAliasesAreImportedFromDiscovery() {
        MTLSEndpointAliases aliases = new MTLSEndpointAliases();
        aliases.setTokenEndpoint("https://mtls.idp/token");
        aliases.setIntrospectionEndpoint("https://mtls.idp/introspect");
        aliases.setUserInfoEndpoint("https://mtls.idp/userinfo");

        OAuth2IdentityProviderConfig config = new OAuth2IdentityProviderConfig();
        config.setMtlsEndpointAliases(aliases);

        Map<String, String> map = config.getConfig();
        assertEquals("https://mtls.idp/token", map.get("mtlsTokenUrl"));
        assertEquals("https://mtls.idp/introspect", map.get("mtlsTokenIntrospectionUrl"));
        assertEquals("https://mtls.idp/userinfo", map.get("mtlsUserInfoUrl"));
    }

    @Test
    public void aliasesMissingFromTheDiscoveryDocumentAreCleared() {
        OAuth2IdentityProviderConfig config = config(OIDCLoginProtocol.TLS_CLIENT_AUTH);
        MTLSEndpointAliases aliases = new MTLSEndpointAliases();
        aliases.setTokenEndpoint("https://mtls.idp/token2");

        config.setMtlsEndpointAliases(aliases);

        Map<String, String> map = config.getConfig();
        assertEquals("https://mtls.idp/token2", map.get("mtlsTokenUrl"));
        assertEquals("", map.get("mtlsTokenIntrospectionUrl"));
        assertEquals("", map.get("mtlsUserInfoUrl"));
        assertEquals("https://idp/introspect", config.getEffectiveTokenIntrospectionUrl());
        assertEquals("https://idp/userinfo", config.getEffectiveUserInfoUrl());
    }

    @Test
    public void allAliasesAreClearedWithoutMtlsEndpointAliases() {
        OAuth2IdentityProviderConfig config = config(OIDCLoginProtocol.TLS_CLIENT_AUTH);

        config.setMtlsEndpointAliases(null);

        assertEquals("", config.getMtlsTokenUrl());
        assertEquals("", config.getMtlsTokenIntrospectionUrl());
        assertEquals("", config.getMtlsUserInfoUrl());
        assertEquals("https://idp/token", config.getEffectiveTokenUrl());
    }

    @Test
    public void blankAliasesPassValidation() {
        OAuth2IdentityProviderConfig config = config(OIDCLoginProtocol.CLIENT_SECRET_POST);
        config.setMtlsEndpointAliases(null);
        RealmModel realm = (RealmModel) Proxy.newProxyInstance(RealmModel.class.getClassLoader(), new Class<?>[] { RealmModel.class },
                (proxy, method, args) -> "getSslRequired".equals(method.getName()) ? SslRequired.EXTERNAL : null);

        // passes when validate does not throw, use assertDoesNotThrow once this test is converted to JUnit 5
        config.validate(realm);
    }
}
