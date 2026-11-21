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
import org.keycloak.models.CibaConfig;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.grants.ciba.CibaGrantType;
import org.keycloak.protocol.oidc.grants.ciba.clientpolicy.executor.SecureCibaSignedAuthenticationRequestExecutor;
import org.keycloak.protocol.oidc.grants.ciba.clientpolicy.executor.SecureCibaSignedAuthenticationRequestExecutorFactory;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.CibaProvider;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectCibaProvider;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.tests.common.BasicRealmWithUserConfig;
import org.keycloak.testsuite.util.oauth.ciba.AuthenticationRequestAcknowledgement;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.keycloak.tests.utils.ClientPoliciesUtil.createAnyClientConditionConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@KeycloakIntegrationTest(config = SecureCibaSignedAuthenticationRequestExecutorTest.CibaServerConfig.class)
public class SecureCibaSignedAuthenticationRequestExecutorTest extends AbstractClientPoliciesTest {


    private static final int AVAILABLE_PERIOD = 60;
    private static final String BINDING_MESSAGE = "signed-request-test";

    @InjectRealm(config = BasicRealmWithUserConfig.class)
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectCibaProvider
    CibaProvider ciba;

    private KeyWrapper signingKey;

    @Test
    public void validEs256SignedAuthenticationRequestIsAccepted() throws Exception {
        setupClientAndPolicy();

        AuthenticationRequestAcknowledgement response = sendRequest(validRequestObject());
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getAuthReqId());
        assertNotNull(ciba.getAuthChannel(BINDING_MESSAGE));
    }

    @ParameterizedTest
    @ValueSource(strings = {"exp", "nbf"})
    public void missingRequiredTimestampIsRejected(String claim) throws Exception {
        setupClientAndPolicy();

        ObjectNode requestObject = validRequestObject();
        requestObject.remove(claim);
        assertErrorForRequest(requestObject, "Missing parameter in the signed authentication request: " + claim);
    }

    @Test
    public void incorrectAudienceIsRejected() throws Exception {
        setupClientAndPolicy();

        ObjectNode requestObject = validRequestObject();
        requestObject.put("aud", oauth.getEndpoints().getIssuer() + "-other");
        assertErrorForRequest(requestObject, "Invalid parameter in the 'request' object: aud");
    }

    @Test
    public void expiredRequestIsRejected() throws Exception {
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

    @Test
    public void excessiveLifetimeIsRejected() throws Exception {
        setupClientAndPolicy();

        ObjectNode requestObject = validRequestObject();
        requestObject.put("exp", requestObject.get("nbf").asLong() + AVAILABLE_PERIOD + 60);
        assertErrorForRequest(requestObject, "signed authentication request's available period is long");
    }

    @ParameterizedTest
    @ValueSource(longs = {Long.MIN_VALUE, Long.MIN_VALUE + 1000})
    public void overflowingLifetimeIsRejected(long nbf) throws Exception {
        setupClientAndPolicy();

        ObjectNode requestObject = validRequestObject();
        requestObject.put("nbf", nbf);
        assertErrorForRequest(requestObject, "signed authentication request's available period is long");
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
            "exp, \"123\""
    })
    public void malformedTimestampIsRejected(String claim, String jsonValue) throws Exception {
        setupClientAndPolicy();

        ObjectNode requestObject = validRequestObject();
        requestObject.set(claim, JsonSerialization.mapper.readTree(jsonValue));
        assertErrorForRequest(requestObject, "Invalid parameter in the signed authentication request: " + claim);
    }

    private void setupClientAndPolicy() throws Exception {
        KeyPair keyPair = KeyUtils.generateEcKeyPair("secp256r1");
        String keyId = KeycloakModelUtils.generateId();
        signingKey = new KeyWrapper();
        signingKey.setKid(keyId);
        signingKey.setAlgorithm(Algorithm.ES256);
        signingKey.setPrivateKey(keyPair.getPrivate());

        JSONWebKeySet jwks = new JSONWebKeySet();
        jwks.setKeys(new JWK[] {
                JWKBuilder.create().algorithm(Algorithm.ES256).kid(keyId).ec(keyPair.getPublic(), KeyUse.SIG)
        });

        ClientRepresentation clientRep = oauth.clientResource().toRepresentation();
        clientRep.getAttributes().put(CibaConfig.OIDC_CIBA_GRANT_ENABLED, Boolean.TRUE.toString());
        clientRep.getAttributes().put(CibaConfig.CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE_PER_CLIENT, CibaConfig.CIBA_POLL_MODE);
        clientRep.getAttributes().put(CibaConfig.CIBA_BACKCHANNEL_AUTH_REQUEST_SIGNING_ALG, Algorithm.ES256);
        OIDCAdvancedConfigWrapper config = OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep);
        config.setUseJwksString(true);
        config.setJwksString(JsonSerialization.writeValueAsString(jwks));
        oauth.clientResource().update(clientRep);

        SecureCibaSignedAuthenticationRequestExecutor.Configuration executorConfig = new SecureCibaSignedAuthenticationRequestExecutor.Configuration();
        executorConfig.setAvailablePeriod(AVAILABLE_PERIOD);
        setupPolicy(realm, SecureCibaSignedAuthenticationRequestExecutorFactory.PROVIDER_ID, executorConfig,
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
        token.put(OAuth2Constants.SCOPE, "openid");
        token.put(OIDCLoginProtocol.LOGIN_HINT_PARAM, BasicRealmWithUserConfig.USERNAME);
        token.put(CibaGrantType.BINDING_MESSAGE, BINDING_MESSAGE);
        return token;
    }

    private AuthenticationRequestAcknowledgement sendRequest(ObjectNode requestObject) {
        String request = new JWSBuilder().kid(signingKey.getKid()).jsonContent(requestObject)
                .sign(new ECDSASignatureSignerContext(signingKey));
        return oauth.ciba().backchannelAuthenticationRequest(BasicRealmWithUserConfig.USERNAME).request(request).send();
    }

    private void assertErrorForRequest(ObjectNode requestObject, String expectedDescription) {
        AuthenticationRequestAcknowledgement response = sendRequest(requestObject);
        assertEquals(400, response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
        assertEquals(expectedDescription, response.getErrorDescription());
    }

    static class CibaServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.option("spi-ciba-auth-channel-ciba-http-auth-channel-http-authentication-channel-uri",
                    "http://localhost:8500" + CibaProvider.CONTEXT_REQUEST_AUTH_CHANNEL);
        }
    }
}
