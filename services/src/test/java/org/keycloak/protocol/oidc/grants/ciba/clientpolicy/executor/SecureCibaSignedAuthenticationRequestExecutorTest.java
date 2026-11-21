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

package org.keycloak.protocol.oidc.grants.ciba.clientpolicy.executor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.math.BigInteger;
import java.net.URI;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.keycloak.OAuthErrorException;
import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakUriInfo;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.grants.ciba.clientpolicy.context.BackchannelAuthenticationRequestContext;
import org.keycloak.protocol.oidc.grants.ciba.endpoints.request.BackchannelAuthenticationEndpointRequestParser;
import org.keycloak.services.Urls;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.urls.HostnameProvider;
import org.keycloak.urls.UrlType;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Tests the "exp" and "nbf" handling of {@link SecureCibaSignedAuthenticationRequestExecutor},
 * ensuring malformed or out-of-range timestamps cannot overflow the lifetime calculation,
 * while valid CIBA signed authentication requests are accepted.
 */
public class SecureCibaSignedAuthenticationRequestExecutorTest {

    private static final int AVAILABLE_PERIOD = 3600;
    private static final String REALM_NAME = "test";
    private static final String CLIENT_ID = "ciba-client";

    private ObjectNode signedAuthReq;
    private SecureCibaSignedAuthenticationRequestExecutor executor;
    private BackchannelAuthenticationRequestContext context;
    private long now;

    @Before
    public void setup() {
        now = Time.currentTime();
        URI baseUri = URI.create("http://localhost:8080/auth/");

        signedAuthReq = JsonSerialization.createObjectNode();
        signedAuthReq.put("iss", CLIENT_ID);
        signedAuthReq.put("aud", Urls.realmIssuer(baseUri, REALM_NAME));
        signedAuthReq.put("iat", now);
        signedAuthReq.put("jti", "test-jti-uuid");

        HostnameProvider hostnameProvider = proxy(HostnameProvider.class, (proxy, method, args) -> {
            if ("getBaseUri".equals(method.getName())) {
                return baseUri;
            }
            throw new UnsupportedOperationException(method.getName());
        });
        RealmModel realm = proxy(RealmModel.class, (proxy, method, args) -> {
            if ("getName".equals(method.getName())) {
                return REALM_NAME;
            }
            throw new UnsupportedOperationException(method.getName());
        });
        ClientModel client = proxy(ClientModel.class, (proxy, method, args) -> {
            if ("getClientId".equals(method.getName())) {
                return CLIENT_ID;
            }
            throw new UnsupportedOperationException(method.getName());
        });
        KeycloakSession[] sessionHolder = new KeycloakSession[1];
        KeycloakContext keycloakContext = proxy(KeycloakContext.class, (proxy, method, args) -> {
            if ("getUri".equals(method.getName())) {
                return new KeycloakUriInfo(sessionHolder[0], UrlType.FRONTEND, null);
            }
            if ("getRealm".equals(method.getName())) {
                return realm;
            }
            if ("getClient".equals(method.getName())) {
                return client;
            }
            throw new UnsupportedOperationException(method.getName());
        });
        KeycloakSession session = proxy(KeycloakSession.class, (proxy, method, args) -> {
            if ("getAttribute".equals(method.getName()) && args.length == 1) {
                return BackchannelAuthenticationEndpointRequestParser.CIBA_SIGNED_AUTHENTICATION_REQUEST.equals(args[0]) ? signedAuthReq : null;
            }
            if ("getProvider".equals(method.getName()) && args.length == 1 && HostnameProvider.class.equals(args[0])) {
                return hostnameProvider;
            }
            if ("getContext".equals(method.getName())) {
                return keycloakContext;
            }
            throw new UnsupportedOperationException(method.getName());
        });
        sessionHolder[0] = session;

        SecureCibaSignedAuthenticationRequestExecutor.Configuration config = new SecureCibaSignedAuthenticationRequestExecutor.Configuration();
        config.setAvailablePeriod(AVAILABLE_PERIOD);
        executor = new SecureCibaSignedAuthenticationRequestExecutor(session);
        executor.setupConfiguration(config);

        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.putSingle(OIDCLoginProtocol.REQUEST_PARAM, "request");
        context = new BackchannelAuthenticationRequestContext(null, null, params);
    }

    // --- Valid lifetimes ---

    @Test
    public void validLifetime() throws ClientPolicyException {
        signedAuthReq.put("nbf", now);
        signedAuthReq.put("exp", now + 60);
        executor.executeOnEvent(context);
    }

    @Test
    public void lifetimeEqualToAvailablePeriod() throws ClientPolicyException {
        signedAuthReq.put("nbf", now);
        signedAuthReq.put("exp", now + AVAILABLE_PERIOD);
        executor.executeOnEvent(context);
    }

    @Test
    public void zeroLifetime() throws ClientPolicyException {
        signedAuthReq.put("nbf", now);
        signedAuthReq.put("exp", now);
        executor.executeOnEvent(context);
    }

    // --- Overflow and lifetime boundary checks ---

    @Test
    public void lifetimeLongerThanAvailablePeriod() {
        signedAuthReq.put("nbf", now);
        signedAuthReq.put("exp", now + AVAILABLE_PERIOD + 60);
        expectRejected("signed authentication request's available period is long");
    }

    @Test
    public void nbfNearLongMinValueDoesNotOverflowLifetime() {
        signedAuthReq.put("nbf", Long.MIN_VALUE + 1000);
        signedAuthReq.put("exp", now + 60);
        expectRejected("signed authentication request's available period is long");
    }

    @Test
    public void nbfEqualToLongMinValueDoesNotOverflowLifetime() {
        signedAuthReq.put("nbf", Long.MIN_VALUE);
        signedAuthReq.put("exp", now + 60);
        expectRejected("signed authentication request's available period is long");
    }

    @Test
    public void veryOldNbfWithoutOverflowIsRejected() {
        signedAuthReq.put("nbf", 0L);
        signedAuthReq.put("exp", now + 60);
        expectRejected("signed authentication request's available period is long");
    }

    @Test
    public void expired() {
        signedAuthReq.put("nbf", now - 120);
        signedAuthReq.put("exp", now - 60);
        expectRejected("Request Expired");
    }

    @Test
    public void nbfInTheFuture() {
        signedAuthReq.put("nbf", now + 60);
        signedAuthReq.put("exp", now + 120);
        expectRejected("Request not yet being processed");
    }

    @Test
    public void nbfNearLongMaxValueIsInTheFuture() {
        signedAuthReq.put("nbf", Long.MAX_VALUE);
        signedAuthReq.put("exp", Long.MAX_VALUE);
        expectRejected("Request not yet being processed");
    }

    // --- Malformed / out-of-range timestamp values ---

    @Test
    public void nbfAboveLongRange() {
        signedAuthReq.put("nbf", BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.TEN));
        signedAuthReq.put("exp", now + 60);
        expectRejected("Invalid parameter in the signed authentication request: nbf");
    }

    @Test
    public void nbfBelowLongRange() {
        signedAuthReq.put("nbf", BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.TEN));
        signedAuthReq.put("exp", now + 60);
        expectRejected("Invalid parameter in the signed authentication request: nbf");
    }

    @Test
    public void nbfNumericString() {
        signedAuthReq.put("nbf", String.valueOf(now));
        signedAuthReq.put("exp", now + 60);
        expectRejected("Invalid parameter in the signed authentication request: nbf");
    }

    @Test
    public void nbfHugeFloatingPoint() {
        signedAuthReq.put("nbf", -1e30);
        signedAuthReq.put("exp", now + 60);
        expectRejected("Invalid parameter in the signed authentication request: nbf");
    }

    @Test
    public void nbfNotANumber() {
        signedAuthReq.put("nbf", true);
        signedAuthReq.put("exp", now + 60);
        expectRejected("Invalid parameter in the signed authentication request: nbf");
    }

    @Test
    public void expAboveLongRange() {
        signedAuthReq.put("nbf", now);
        signedAuthReq.put("exp", BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.TEN));
        expectRejected("Invalid parameter in the signed authentication request: exp");
    }

    @Test
    public void expBelowLongRange() {
        signedAuthReq.put("nbf", now);
        signedAuthReq.put("exp", BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.TEN));
        expectRejected("Invalid parameter in the signed authentication request: exp");
    }

    @Test
    public void expNumericString() {
        signedAuthReq.put("nbf", now);
        signedAuthReq.put("exp", String.valueOf(now + 60));
        expectRejected("Invalid parameter in the signed authentication request: exp");
    }

    @Test
    public void expHugeFloatingPoint() {
        signedAuthReq.put("nbf", now);
        signedAuthReq.put("exp", 1e30);
        expectRejected("Invalid parameter in the signed authentication request: exp");
    }

    @Test
    public void expNotANumber() {
        signedAuthReq.put("nbf", now);
        signedAuthReq.put("exp", "not-a-number");
        expectRejected("Invalid parameter in the signed authentication request: exp");
    }

    private void expectRejected(String expectedDetail) {
        try {
            executor.executeOnEvent(context);
            fail("Expected CIBA signed authentication request to be rejected with: " + expectedDetail);
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_REQUEST, e.getError());
            assertEquals(expectedDetail, e.getErrorDetail());
        }
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
