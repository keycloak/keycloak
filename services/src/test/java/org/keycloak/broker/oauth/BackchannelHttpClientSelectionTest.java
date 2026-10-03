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

import java.lang.reflect.Proxy;

import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * The certificate-bearing mTLS client is only in scope for the RFC 8705 endpoints (token, user-info,
 * introspection). A backchannel call that is NOT one of those advertised endpoints — RP-initiated /
 * backchannel <em>logout</em> — must use the shared non-mTLS client and therefore must not resolve or
 * present the realm's client certificate, even when the IdP is configured with {@code tls_client_auth}.
 *
 * <p>The distinguishing observable is {@code session.keys()}: only the mTLS path resolves the client
 * certificate through it. The test session's {@code keys()} throws, so a request that touches the mTLS
 * path fails loudly while a request that stays on the shared client succeeds.
 */
public class BackchannelHttpClientSelectionTest {

    private static final class KeysCalledError extends RuntimeException {
        KeysCalledError() {
            super("session.keys() must not be called: mTLS client certificate resolution is out of scope here");
        }
    }

    /** Exposes the protected client-selection method under test. */
    private static final class TestProvider extends OAuth2IdentityProvider {
        TestProvider(KeycloakSession session, OAuth2IdentityProviderConfig config) {
            super(session, config);
        }

        SimpleHttp select(boolean tlsClientAuth) {
            return createBackchannelHttp(tlsClientAuth);
        }
    }

    private static KeycloakSession session() {
        HttpClientProvider httpClientProvider = (HttpClientProvider) Proxy.newProxyInstance(
                BackchannelHttpClientSelectionTest.class.getClassLoader(),
                new Class<?>[] { HttpClientProvider.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getHttpClient" -> sharedClient();
                    case "createHttpClient" -> sharedClient();
                    case "getMaxConsumedResponseSize" -> HttpClientProvider.DEFAULT_MAX_CONSUMED_RESPONSE_SIZE;
                    default -> throw new UnsupportedOperationException("stub: " + method.getName());
                });
        // A bare realm-bearing context so the mTLS path (session.getContext().getRealm()) proceeds to the
        // client-certificate resolution step, which is the observable under test.
        RealmModel realm = (RealmModel) Proxy.newProxyInstance(
                BackchannelHttpClientSelectionTest.class.getClassLoader(),
                new Class<?>[] { RealmModel.class },
                (proxy, method, args) -> { throw new UnsupportedOperationException("stub: " + method.getName()); });
        KeycloakContext context = (KeycloakContext) Proxy.newProxyInstance(
                BackchannelHttpClientSelectionTest.class.getClassLoader(),
                new Class<?>[] { KeycloakContext.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getRealm" -> realm;
                    default -> throw new UnsupportedOperationException("stub: " + method.getName());
                });
        return (KeycloakSession) Proxy.newProxyInstance(
                BackchannelHttpClientSelectionTest.class.getClassLoader(),
                new Class<?>[] { KeycloakSession.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getProvider" -> httpClientProvider;
                    case "getContext" -> context;
                    // Only the mTLS path touches keys() (to resolve the client certificate). Any call here means
                    // the request was (wrongly) routed through the certificate-bearing client.
                    case "keys" -> throw new KeysCalledError();
                    default -> throw new UnsupportedOperationException("stub: " + method.getName());
                });
    }

    private static CloseableHttpClient sharedClient() {
        return HttpClients.createMinimal();
    }

    private static OAuth2IdentityProviderConfig tlsClientAuthConfig() {
        OAuth2IdentityProviderConfig config = new OAuth2IdentityProviderConfig(new IdentityProviderModel());
        config.setClientAuthMethod(OIDCLoginProtocol.TLS_CLIENT_AUTH);
        return config;
    }

    @Test
    public void logoutUsesSharedClientAndDoesNotResolveClientCertificate() {
        TestProvider provider = new TestProvider(session(), tlsClientAuthConfig());
        // Logout is not an RFC 8705 mTLS endpoint: it must stay on the shared client, never resolving the cert.
        assertNotNull(provider.select(false));
    }

    @Test
    public void mtlsEndpointsStillResolveClientCertificate() {
        TestProvider provider = new TestProvider(session(), tlsClientAuthConfig());
        // Token / user-info / introspection DO use the certificate-bearing client, which enters the mTLS
        // client-build path and resolves the realm's client certificate via session.keys(). The test session's
        // keys() throws the sentinel, which buildMtlsHttpClient wraps into an IdentityBrokerException — proving
        // the request reached the certificate-resolution step rather than staying on the shared client.
        IdentityBrokerException thrown =
                assertThrows(IdentityBrokerException.class, () -> provider.select(true));
        assertTrue("the mTLS path must fail because session.keys() (client-certificate resolution) was reached",
                thrown.getCause() instanceof KeysCalledError);
    }
}
