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
package org.keycloak.tests.client.policies;

import java.security.KeyPair;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.ECDSASignatureSignerContext;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.executor.SecureRequestObjectExecutorFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.tests.common.BasicRealmWithUserConfig;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.keycloak.tests.utils.ClientPoliciesUtil.createAnyClientConditionConfig;
import static org.keycloak.tests.utils.ClientPoliciesUtil.createSecureRequestObjectExecutorConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Exercises signed request objects through the authorization endpoint, including timestamp validation errors.
 */
@KeycloakIntegrationTest
public class SecureRequestObjectExecutorTest extends AbstractClientPoliciesTest {

    private static final String SCOPE = "openid";
    private static final String RESPONSE_TYPE = "code";

    @InjectRealm(config = BasicRealmWithUserConfig.class)
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauth;

    private KeyWrapper signingKey;


    @Test
    public void validEs256RequestObjectIsAccepted() throws Exception {
        setupClientAndPolicy();

        oauth.loginForm().request(sign(validRequestObject())).open();
        oauth.fillLoginForm(BasicRealmWithUserConfig.USERNAME, BasicRealmWithUserConfig.PASSWORD);
        assertNotNull(oauth.parseLoginResponse().getCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"exp", "nbf"})
    public void missingRequiredTimestampIsRejected(String claim) throws Exception {
        setupClientAndPolicy();

        ObjectNode requestObject = validRequestObject();
        requestObject.remove(claim);
        assertErrorForRequest(requestObject, "Missing parameter in the 'request' object: " + claim);
    }

    @Test
    public void incorrectAudienceIsRejected() throws Exception {
        setupClientAndPolicy();

        ObjectNode requestObject = validRequestObject();
        requestObject.put("aud", oauth.getEndpoints().getIssuer() + "-other");
        assertErrorForRequest(requestObject, "Invalid parameter in the 'request' object: aud");
    }

    @Test
    public void expiredRequestObjectIsRejected() throws Exception {
        setupClientAndPolicy();

        ObjectNode requestObject = validRequestObject();
        requestObject.put("exp", 0L);
        assertErrorForRequest(requestObject, "Request Expired");
    }

    @Test
    public void futureNbfIsRejected() throws Exception {
        setupClientAndPolicy();

        ObjectNode requestObject = validRequestObject();
        requestObject.put("nbf", (long) Time.currentTime() + 600);
        assertErrorForRequest(requestObject, "Request not yet being processed");
    }

    @ParameterizedTest
    @ValueSource(longs = {Long.MIN_VALUE, Long.MIN_VALUE + 15})
    public void overflowingLifetimeIsRejected(long nbf) throws Exception {
        setupClientAndPolicy();

        ObjectNode requestObject = validRequestObject();
        requestObject.put("nbf", nbf);
        assertErrorForRequest(requestObject, "Request's available period is long");
    }

    @Test
    public void expirationBeforeNbfIsRejected() throws Exception {
        setupClientAndPolicy();

        ObjectNode requestObject = validRequestObject();
        long now = Time.currentTime();
        requestObject.put("nbf", now + 10);
        requestObject.put("exp", now + 5);
        assertErrorForRequest(requestObject, "Invalid parameter in the 'request' object: exp is before nbf");
    }

    @ParameterizedTest
    @CsvSource({
            "nbf, 9223372036854775817",
            "nbf, -9223372036854775818",
            "nbf, 1.5",
            "nbf, \"123\"",
            "exp, 9223372036854775817",
            "exp, -9223372036854775818",
            "exp, 1.5",
            "exp, \"123\"",
            "iat, 9223372036854775817",
            "iat, -9223372036854775818",
            "iat, 1.5",
            "iat, \"123\""
    })
    public void malformedTimestampIsRejected(String claim, String jsonValue) throws Exception {
        setupClientAndPolicy();

        ObjectNode requestObject = validRequestObject();
        requestObject.set(claim, JsonSerialization.mapper.readTree(jsonValue));
        assertErrorForRequest(requestObject, "Invalid parameter in the 'request' object: " + claim);
    }

    @Test
    public void overflowingFutureIatIsRejected() throws Exception {
        setupClientAndPolicy();

        ObjectNode requestObject = validRequestObject();
        requestObject.put("iat", Long.MAX_VALUE);
        assertErrorForRequest(requestObject, "Request issued in the future");
    }

    private void setupClientAndPolicy() throws Exception {
        KeyPair keyPair = KeyUtils.generateEcKeyPair("secp256r1");
        signingKey = new KeyWrapper();
        signingKey.setAlgorithm(Algorithm.ES256);
        signingKey.setPrivateKey(keyPair.getPrivate());

        JSONWebKeySet jwks = new JSONWebKeySet();
        jwks.setKeys(new JWK[] {
                JWKBuilder.create().algorithm(Algorithm.ES256).kid(KeycloakModelUtils.generateId()).ec(keyPair.getPublic(), KeyUse.SIG)
        });

        ClientRepresentation clientRep = oauth.clientResource().toRepresentation();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestObjectSignatureAlg(Algorithm.ES256);
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUseJwksString(true);
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setJwksString(JsonSerialization.writeValueAsString(jwks));
        oauth.clientResource().update(clientRep);

        setupPolicy(realm, SecureRequestObjectExecutorFactory.PROVIDER_ID, createSecureRequestObjectExecutorConfig(60, true),
                AnyClientConditionFactory.PROVIDER_ID, createAnyClientConditionConfig());
    }

    private ObjectNode validRequestObject() {
        long now = Time.currentTime();
        ObjectNode token = JsonSerialization.createObjectNode();
        token.put("jti", KeycloakModelUtils.generateId());
        token.put("iat", now);
        token.put("nbf", now);
        token.put("exp", now + 30);
        token.put("iss", oauth.getClientId());
        token.put("aud", oauth.getEndpoints().getIssuer());
        token.put(OIDCLoginProtocol.CLIENT_ID_PARAM, oauth.getClientId());
        token.put(OIDCLoginProtocol.RESPONSE_TYPE_PARAM, RESPONSE_TYPE);
        token.put(OIDCLoginProtocol.REDIRECT_URI_PARAM, oauth.getRedirectUri());
        token.put(OAuth2Constants.SCOPE, SCOPE);
        token.put(OIDCLoginProtocol.NONCE_PARAM, KeycloakModelUtils.generateId());
        return token;
    }

    private String sign(ObjectNode token) {
        return new JWSBuilder().jsonContent(token).sign(new ECDSASignatureSignerContext(signingKey));
    }

    private void assertErrorForRequest(ObjectNode requestObject, String expectedDescription) {
        oauth.loginForm().request(sign(requestObject)).open();
        AuthorizationEndpointResponse response = oauth.parseLoginResponse();
        assertEquals(OAuthErrorException.INVALID_REQUEST_OBJECT, response.getError());
        assertEquals(expectedDescription, response.getErrorDescription());
    }
}
