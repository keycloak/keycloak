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

package org.keycloak.services.clientpolicy.executor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.math.BigInteger;
import java.net.URI;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.keycloak.OAuthErrorException;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakUriInfo;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.endpoints.request.AuthzEndpointRequestParser;
import org.keycloak.services.Urls;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.AuthorizationRequestContext;
import org.keycloak.urls.HostnameProvider;
import org.keycloak.urls.UrlType;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Tests the "exp" / "nbf" / "iat" handling of {@link SecureRequestObjectExecutor}, in particular that malformed or
 * out-of-range values cannot overflow the lifetime calculation, while valid request objects are still accepted.
 * <p>
 * The request object used here is complete (scope, audience pointing to the realm issuer), so a request that passes
 * all the checks makes {@code executeOnEvent} return normally.
 */
public class SecureRequestObjectExecutorTest {

    private static final int AVAILABLE_PERIOD = 60;
    private static final int ALLOWED_CLOCK_SKEW = 15;
    private static final String REALM_NAME = "test";

    private ObjectNode requestObject;
    private SecureRequestObjectExecutor executor;
    private AuthorizationRequestContext context;
    private long now;

    @Before
    public void setup() {
        now = Time.currentTime();
        URI baseUri = URI.create("http://localhost:8080/auth/");

        requestObject = JsonSerialization.createObjectNode();
        requestObject.put(OIDCLoginProtocol.SCOPE_PARAM, "openid");
        requestObject.put("aud", Urls.realmIssuer(baseUri, REALM_NAME));

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
        KeycloakSession[] sessionHolder = new KeycloakSession[1];
        KeycloakContext keycloakContext = proxy(KeycloakContext.class, (proxy, method, args) -> {
            if ("getUri".equals(method.getName())) {
                return new KeycloakUriInfo(sessionHolder[0], UrlType.FRONTEND, null);
            }
            if ("getRealm".equals(method.getName())) {
                return realm;
            }
            throw new UnsupportedOperationException(method.getName());
        });
        KeycloakSession session = proxy(KeycloakSession.class, (proxy, method, args) -> {
            if ("getAttribute".equals(method.getName()) && args.length == 1) {
                return AuthzEndpointRequestParser.AUTHZ_REQUEST_OBJECT.equals(args[0]) ? requestObject : null;
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

        SecureRequestObjectExecutor.Configuration config = new SecureRequestObjectExecutor.Configuration();
        config.setVerifyNbf(true);
        config.setAvailablePeriod(AVAILABLE_PERIOD);
        config.setAllowedClockSkew(ALLOWED_CLOCK_SKEW);
        executor = new SecureRequestObjectExecutor(session);
        executor.setupConfiguration(config);

        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.putSingle(OIDCLoginProtocol.REQUEST_PARAM, "request");
        context = new AuthorizationRequestContext(null, null, null, params, null);
    }

    @Test
    public void validLifetime() throws ClientPolicyException {
        requestObject.put("nbf", now);
        requestObject.put("exp", now + 30);
        requestObject.put("iat", now);
        executor.executeOnEvent(context);
    }

    @Test
    public void lifetimeEqualToAvailablePeriod() throws ClientPolicyException {
        requestObject.put("nbf", now);
        requestObject.put("exp", now + AVAILABLE_PERIOD);
        executor.executeOnEvent(context);
    }

    @Test
    public void zeroLifetime() throws ClientPolicyException {
        requestObject.put("nbf", now);
        requestObject.put("exp", now + 1);
        executor.executeOnEvent(context);
    }

    @Test
    public void nbfAndIatAheadWithinAllowedClockSkew() throws ClientPolicyException {
        requestObject.put("nbf", now + ALLOWED_CLOCK_SKEW - 5);
        requestObject.put("exp", now + ALLOWED_CLOCK_SKEW + 30);
        requestObject.put("iat", now + ALLOWED_CLOCK_SKEW - 5);
        executor.executeOnEvent(context);
    }

    @Test
    public void nbfInThePast() throws ClientPolicyException {
        requestObject.put("nbf", now - 30);
        requestObject.put("exp", now + 10);
        executor.executeOnEvent(context);
    }

    @Test
    public void iatNearLongMinValueIsNotInTheFuture() throws ClientPolicyException {
        requestObject.put("nbf", now);
        requestObject.put("exp", now + 30);
        requestObject.put("iat", Long.MIN_VALUE);
        executor.executeOnEvent(context);
    }

    @Test
    public void iatIsOptional() throws ClientPolicyException {
        requestObject.put("nbf", now);
        requestObject.put("exp", now + 30);
        executor.executeOnEvent(context);
    }

    @Test
    public void lifetimeLongerThanAvailablePeriod() {
        requestObject.put("nbf", now);
        requestObject.put("exp", now + AVAILABLE_PERIOD + 60);
        expectRejected("Request's available period is long");
    }

    @Test
    public void nbfNearLongMinValueDoesNotOverflowLifetime() {
        requestObject.put("nbf", Long.MIN_VALUE + ALLOWED_CLOCK_SKEW);
        requestObject.put("exp", now + 30);
        expectRejected("Request's available period is long");
    }

    @Test
    public void nbfEqualToLongMinValueDoesNotOverflowLifetime() {
        requestObject.put("nbf", Long.MIN_VALUE);
        requestObject.put("exp", now + 30);
        expectRejected("Request's available period is long");
    }

    @Test
    public void veryOldNbfWithoutOverflowIsRejected() {
        requestObject.put("nbf", 0L);
        requestObject.put("exp", now + 30);
        expectRejected("Request's available period is long");
    }

    @Test
    public void expBeforeNbf() {
        requestObject.put("nbf", now + 12);
        requestObject.put("exp", now + 10);
        expectRejected("Invalid parameter in the 'request' object: exp is before nbf");
    }

    @Test
    public void expired() {
        requestObject.put("nbf", now - 60);
        requestObject.put("exp", now - 30);
        expectRejected("Request Expired");
    }

    // --- nbf / iat in the future

    @Test
    public void nbfBeyondAllowedClockSkew() {
        requestObject.put("nbf", now + ALLOWED_CLOCK_SKEW + 60);
        requestObject.put("exp", now + ALLOWED_CLOCK_SKEW + 90);
        expectRejected("Request not yet being processed");
    }

    @Test
    public void nbfNearLongMaxValueIsInTheFuture() {
        requestObject.put("nbf", Long.MAX_VALUE);
        requestObject.put("exp", Long.MAX_VALUE);
        expectRejected("Request not yet being processed");
    }

    @Test
    public void iatBeyondAllowedClockSkew() {
        requestObject.put("nbf", now);
        requestObject.put("exp", now + 30);
        requestObject.put("iat", now + ALLOWED_CLOCK_SKEW + 60);
        expectRejected("Request issued in the future");
    }

    @Test
    public void iatNearLongMaxValueIsInTheFuture() {
        requestObject.put("nbf", now);
        requestObject.put("exp", now + 30);
        requestObject.put("iat", Long.MAX_VALUE);
        expectRejected("Request issued in the future");
    }

    // --- malformed timestamps

    @Test
    public void nbfAboveLongRange() {
        requestObject.put("nbf", BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.TEN));
        requestObject.put("exp", now + 30);
        expectRejected("Invalid parameter in the 'request' object: nbf");
    }

    @Test
    public void nbfBelowLongRange() {
        requestObject.put("nbf", BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.TEN));
        requestObject.put("exp", now + 30);
        expectRejected("Invalid parameter in the 'request' object: nbf");
    }

    @Test
    public void nbfNumericString() {
        requestObject.put("nbf", String.valueOf(now));
        requestObject.put("exp", now + 30);
        expectRejected("Invalid parameter in the 'request' object: nbf");
    }

    @Test
    public void nbfHugeFloatingPoint() {
        requestObject.put("nbf", -1e30);
        requestObject.put("exp", now + 30);
        expectRejected("Invalid parameter in the 'request' object: nbf");
    }

    @Test
    public void nbfNotANumber() {
        requestObject.put("nbf", true);
        requestObject.put("exp", now + 30);
        expectRejected("Invalid parameter in the 'request' object: nbf");
    }

    @Test
    public void expAboveLongRange() {
        requestObject.put("nbf", now);
        requestObject.put("exp", BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.TEN));
        expectRejected("Invalid parameter in the 'request' object: exp");
    }

    @Test
    public void expNumericString() {
        requestObject.put("nbf", now);
        requestObject.put("exp", String.valueOf(now + 30));
        expectRejected("Invalid parameter in the 'request' object: exp");
    }

    @Test
    public void iatAboveLongRange() {
        requestObject.put("nbf", now);
        requestObject.put("exp", now + 30);
        requestObject.put("iat", BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.TEN));
        expectRejected("Invalid parameter in the 'request' object: iat");
    }

    private void expectRejected(String expectedDetail) {
        try {
            executor.executeOnEvent(context);
            fail("Expected the request object to be rejected with: " + expectedDetail);
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_REQUEST_OBJECT, e.getError());
            assertEquals(expectedDetail, e.getErrorDetail());
        }
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
