/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.oauth;

import java.util.Collections;
import java.util.HashMap;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientSecretAuthenticator;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.Algorithm;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.ClientSecretConstants;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.client.authentication.JWTClientSecretCredentialsProvider;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.client.ClientSecretRotationUtils;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.util.TokenUtil;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

@KeycloakIntegrationTest
public class ClientAuthSecretSignedJWTTest {

    private static final String CLIENT_SECRET = "atleast-14chars-password";

    @InjectRealm(config = ClientAuthSecretSignedJWTRealmConfig.class)
    protected ManagedRealm realm;

    @InjectOAuthClient(config = OAuthClientConfig.class)
    OAuthClient oauth;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @InjectEvents
    Events events;

    // TEST SUCCESS

    @Test
    public void testCodeToTokenRequestSuccess() {
        testCodeToTokenRequestSuccess(Algorithm.HS256);
    }

    @Test
    public void testCodeToTokenRequestSuccessHS384() {
        testCodeToTokenRequestSuccess(Algorithm.HS384);
    }

    @Test
    public void testCodeToTokenRequestSuccessHS512() {
        testCodeToTokenRequestSuccess(Algorithm.HS512);
    }

    // Issue 34547
    @Test
    public void testCodeToTokenRequestSuccessWhenClientHasGeneratedKeys() {
        // Test when client has public/private keys generated despite the fact that it uses client-secret for the client authentication (and not those keys)
        oauth.clientResource().getCertficateResource("jwt.credential").generate();
        realm.cleanup().add(this::removeCertificateInformation);

        testCodeToTokenRequestSuccess(Algorithm.HS256);
    }

    @Test
    public void testCodeToTokenRequestFailureWhenClientHasPrivateKeyJWT() {
        // Setup client for "private_key_jwt" authentication
        oauth.clientResource().getCertficateResource("jwt.credential").generate();
        ClientRepresentation clientRep = oauth.clientResource().toRepresentation();
        clientRep.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID);
        oauth.clientResource().update(clientRep);
        realm.cleanup().add(this::removeCertificateInformation);

        // Client should not be able to authenticate with "client_secret_jwt"oauth.client("test-app", "password");
        doLogin();

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.accessTokenRequest(code)
                .signedJwt(getClientSignedJWT(CLIENT_SECRET, Algorithm.HS256))
                .send();
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_CLIENT, response.getError());
    }

    @Test
    public void testInvalidIssuer() {
        doLogin();

        String code = oauth.parseLoginResponse().getCode();
        JWTClientSecretCredentialsProvider jwtProvider = new JWTClientSecretCredentialsProvider() {
            @Override
            protected JsonWebToken createRequestToken(String clientId, String realmInfoUrl) {
                JsonWebToken jwt = super.createRequestToken(clientId, realmInfoUrl);

                jwt.issuer("bad-issuer");

                return jwt;
            }
        };
        String algorithm = Algorithm.HS256;
        jwtProvider.setClientSecret(CLIENT_SECRET, algorithm);
        String jwt = jwtProvider.createSignedRequestToken(oauth.getClientId(), realm.getBaseUrl(), algorithm);
        AccessTokenResponse response = oauth.accessTokenRequest(code).signedJwt(jwt).send();

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatusCode());
        assertEquals("invalid_client", response.getError());
    }

    @Test
    public void testCodeToTokenRequestFailureHS384Enforced() {
        setAlgorithmForClient(Algorithm.HS384);

        testCodeToTokenRequestSuccess(Algorithm.HS384);
    }

    @Test
    public void testCodeToTokenRequestFailureHS512Enforced() {
        setAlgorithmForClient(Algorithm.HS512);

        doLogin();

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.accessTokenRequest(code)
                .signedJwt(getClientSignedJWT(CLIENT_SECRET, Algorithm.HS256))
                .send();
        assertEquals(400, response.getStatusCode());
        assertEquals("invalid_client", response.getError());
    }

    private void testCodeToTokenRequestSuccess(String algorithm) {
        EventRepresentation loginEvent = doLogin();

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.accessTokenRequest(code)
                .signedJwt(getClientSignedJWT(CLIENT_SECRET, algorithm))
                .send();

        assertEquals(200, response.getStatusCode());

        oauth.verifyToken(response.getAccessToken());
        oauth.parseRefreshToken(response.getRefreshToken());
        EventAssertion.expectCodeToTokenSuccess(events.poll())
                .sessionId(loginEvent.getSessionId())
                .clientId(oauth.getClientId())
                .details(Details.CODE_ID, loginEvent.getDetails().get(Details.CODE_ID))
                .details(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_REFRESH)
                .details(Details.CLIENT_AUTH_METHOD, JWTClientSecretAuthenticator.PROVIDER_ID);
    }

    /**
     * When there is a client secret rotation, the client must be able to authenticate itself by the rotated secret and the new secret. (As long as both secrets remain valid)
     */
    @Test
    public void authenticateWithValidClientSecretWhenRotationPolicyIsEnableForHS256() {
        processAuthenticateWithAlgorithm(Algorithm.HS256);
    }

    @Test
    public void authenticateWithValidClientSecretWhenRotationPolicyIsEnableForHS384() {
        processAuthenticateWithAlgorithm(Algorithm.HS384);
    }

    @Test
    public void authenticateWithValidClientSecretWhenRotationPolicyIsEnableForHS512() {
        processAuthenticateWithAlgorithm(Algorithm.HS512);
    }

    @Test
    public void regenerateClientSecretForClientSecretBasicWithIdTokenHs512() {
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId("basic-hs512-client");
        clientRep.setName("basic-hs512-client");
        clientRep.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        clientRep.setBearerOnly(Boolean.FALSE);
        clientRep.setPublicClient(Boolean.FALSE);
        clientRep.setClientAuthenticatorType("client-secret");
        clientRep.setStandardFlowEnabled(Boolean.TRUE);
        clientRep.setAttributes(new HashMap<>());
        clientRep.getAttributes().put(OIDCConfigAttributes.ID_TOKEN_SIGNED_RESPONSE_ALG, Algorithm.HS512);
        clientRep.setRedirectUris(Collections.singletonList("http://127.0.0.1:8500/callback/oauth"));

        String clientUuid;
        try (Response resp = realm.admin().clients().create(clientRep)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), resp.getStatus());
            clientUuid = ApiUtil.getCreatedId(resp);
        }
        realm.cleanup().add(r -> r.clients().get(clientUuid).remove());

        ClientResource clientResource = realm.admin().clients().get(clientUuid);
        String secret = clientResource.generateNewSecret().getValue();
        assertThat(secret.length(), is(SecretGenerator.equivalentEntropySize(
                SecretGenerator.SECRET_LENGTH_512_BITS, SecretGenerator.ALPHANUM.length)));
    }

    private void processAuthenticateWithAlgorithm(String algorithm) {
        String cidConfidential = createClientByAdmin("jwt-client", "jwt-client", CLIENT_SECRET, algorithm);
        ClientResource clientResource = realm.admin().clients().get(cidConfidential);
        ClientSecretRotationUtils.configureDefaultProfileAndPolicy(realm);

        int requiredSecretLength = SecretGenerator.equivalentEntropySize(
                SecretGenerator.SECRET_LENGTH_512_BITS, SecretGenerator.ALPHANUM.length);

        String firstSecret = clientResource.generateNewSecret().getValue(); //clientResource.getSecret().getValue();
        assertThat(firstSecret.length(), is(requiredSecretLength));

        //generate new secret, rotate the secret
        String newSecret = clientResource.generateNewSecret().getValue();
        assertThat(firstSecret, not(equalTo(newSecret)));
        assertThat(newSecret.length(), is(requiredSecretLength));

        // test with first secret
        oauth.client("jwt-client");
        EventRepresentation loginEvent = doLogin();
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.accessTokenRequest(code)
                .signedJwt(getClientSignedJWT(firstSecret, algorithm))
                .send();
        assertThat(response.getStatusCode(), is(Response.Status.OK.getStatusCode()));
        EventAssertion.expectCodeToTokenSuccess(events.poll())
                .sessionId(loginEvent.getSessionId())
                .clientId(oauth.getClientId())
                .details(Details.CODE_ID, loginEvent.getDetails().get(Details.CODE_ID))
                .details(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_REFRESH)
                .details(Details.CLIENT_AUTH_METHOD, JWTClientSecretAuthenticator.PROVIDER_ID)
                .details(Details.CLIENT_AUTH_DETAIL, ClientSecretConstants.CLIENT_ROTATED_EVENT_DETAIL);

        // test with new secret
        oauth.openLoginForm();
        loginEvent = events.poll();
        EventAssertion.expectLoginSuccess(loginEvent).clientId(oauth.getClientId());
        code = oauth.parseLoginResponse().getCode();
        response = oauth.accessTokenRequest(code)
                .signedJwt(getClientSignedJWT(newSecret, algorithm))
                .send();
        assertThat(response.getStatusCode(), is(Response.Status.OK.getStatusCode()));
        EventAssertion.expectCodeToTokenSuccess(events.poll())
                .sessionId(loginEvent.getSessionId())
                .clientId(oauth.getClientId())
                .details(Details.CODE_ID, loginEvent.getDetails().get(Details.CODE_ID))
                .details(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_REFRESH)
                .details(Details.CLIENT_AUTH_METHOD, JWTClientSecretAuthenticator.PROVIDER_ID)
                .withoutDetails(Details.CLIENT_AUTH_DETAIL);
    }

    // TEST ERRORS

    @Test
    public void testAssertionInvalidSignature() {
        doLogin();

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.accessTokenRequest(code)
                .signedJwt(getClientSignedJWT("ppassswordd"))
                .send();

        // https://tools.ietf.org/html/rfc6749#section-5.2
        assertEquals(400, response.getStatusCode());
        assertEquals("unauthorized_client", response.getError());
    }

    @Test
    public void testAssertionWithNoneAlgorithm() throws JWSInputException {
        doLogin();

        String code = oauth.parseLoginResponse().getCode();

        String client1Jwt = getClientSignedJWT("ppassswordd");
        JsonWebToken client1JsonWebToken = new JWSInput(client1Jwt).readJsonContent(JsonWebToken.class);
        String request = new JWSBuilder().jsonContent(client1JsonWebToken).none();

        AccessTokenResponse response = oauth.accessTokenRequest(code).signedJwt(request).send();

        // https://tools.ietf.org/html/rfc6749#section-5.2
        assertEquals(400, response.getStatusCode());
        assertEquals("invalid_client", response.getError());
    }

    @Test
    public void testAssertionReuse() {
        EventRepresentation loginEvent = doLogin();

        String code = oauth.parseLoginResponse().getCode();
        String clientSignedJWT = getClientSignedJWT(CLIENT_SECRET);

        AccessTokenResponse response = oauth.accessTokenRequest(code).signedJwt(clientSignedJWT).send();
        assertEquals(200, response.getStatusCode());
        EventAssertion.expectCodeToTokenSuccess(events.poll())
                .sessionId(loginEvent.getSessionId())
                .clientId(oauth.getClientId())
                .details(Details.CODE_ID, loginEvent.getDetails().get(Details.CODE_ID))
                .details(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_REFRESH)
                .details(Details.CLIENT_AUTH_METHOD, JWTClientSecretAuthenticator.PROVIDER_ID);

        // 2nd attempt to use same clientSignedJWT should fail
        oauth.openLoginForm();
        loginEvent = events.poll();
        EventAssertion.expectLoginSuccess(loginEvent)
                .clientId("test-app");

        String code2 = oauth.parseLoginResponse().getCode();
        response = oauth.accessTokenRequest(code2).signedJwt(clientSignedJWT).send();
        EventAssertion.assertError(events.poll())
                .type(EventType.CODE_TO_TOKEN_ERROR)
                .error("invalid_client_credentials")
                .userId(null)
                .sessionId(null);

        assertEquals(OAuthErrorException.INVALID_CLIENT, response.getError());
        assertEquals(400, response.getStatusCode());
    }

    /**
     * After a secret rotation the client should not be able to authenticate after the rotated secret expires
     */
    @Test
    public void authenticateWithInvalidRotatedClientSecretPolicyIsEnable() {
        String cidConfidential = createClientByAdmin("jwt-client", "jwt-client", CLIENT_SECRET, Algorithm.HS256);
        ClientResource clientResource = realm.admin().clients().get(cidConfidential);
        ClientSecretRotationUtils.configureCustomProfileAndPolicy(realm, 60L, 30L, 20L);
        String firstSecret = clientResource.getSecret().getValue();

        //generate new secret, rotate the secret
        String newSecret = clientResource.generateNewSecret().getValue();
        assertThat(firstSecret, not(equalTo(newSecret)));

        //force rotated secret expiration
        timeOffSet.set(31);

        oauth.client("jwt-client");
        doLogin();
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.accessTokenRequest(code)
                .signedJwt(getClientSignedJWT(firstSecret, Algorithm.HS256))
                .send();
        assertThat(response.getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    private String getClientSignedJWT(String secret) {
        return getClientSignedJWT(secret, Algorithm.HS256);
    }

    private String getClientSignedJWT(String secret, String algorithm) {
        JWTClientSecretCredentialsProvider jwtProvider = new JWTClientSecretCredentialsProvider();
        jwtProvider.setClientSecret(secret, algorithm);
        return jwtProvider.createSignedRequestToken(oauth.getClientId(), realm.getBaseUrl(), algorithm);
    }

    protected String createClientByAdmin(String clientId, String clientName, String clientSecret, String signAlg) {
        ClientRepresentation clientRep = getClientRepresentation(clientId, clientName, clientSecret,signAlg);

        try (Response resp = realm.admin().clients().create(clientRep)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), resp.getStatus());
            // registered components will be removed automatically when a test method finishes regardless of its success or failure.
            String cId = ApiUtil.getCreatedId(resp);
            realm.cleanup().add(r -> {
                oauth.client(oauth.clientResource().toRepresentation().getClientId());
                r.clients().get(cId).remove();
            });
            return cId;
        }
    }

    private ClientRepresentation getClientRepresentation(String clientId, String clientName, String clientSecret, String signAlg) {
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId(clientId);
        clientRep.setName(clientName);
        clientRep.setSecret(clientSecret);
        clientRep.setAttributes(new HashMap<>());
        clientRep.getAttributes()
                .put(ClientSecretConstants.CLIENT_SECRET_CREATION_TIME,
                        String.valueOf(Time.currentTimeSeconds()));
        clientRep.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        clientRep.setBearerOnly(Boolean.FALSE);
        clientRep.setPublicClient(Boolean.FALSE);
        clientRep.setServiceAccountsEnabled(Boolean.TRUE);
        clientRep.setStandardFlowEnabled(Boolean.TRUE);
        clientRep.setImplicitFlowEnabled(Boolean.TRUE);
        clientRep.setClientAuthenticatorType(JWTClientSecretAuthenticator.PROVIDER_ID);
        clientRep.getAttributes().put(OIDCConfigAttributes.TOKEN_ENDPOINT_AUTH_SIGNING_ALG, signAlg);

        clientRep.setRedirectUris(Collections.singletonList("http://127.0.0.1:8500/callback/oauth"));
        return clientRep;
    }

    private void removeCertificateInformation(RealmResource realm) {
        // remove al certificate information
        ClientResource clientRes = AdminApiUtil.findClientByClientId(realm, oauth.getClientId());
        ClientRepresentation clientRep = clientRes.toRepresentation();
        for (String attr : clientRep.getAttributes().keySet()) {
            if (attr.startsWith("jwt.credential.")) {
                clientRep.getAttributes().put(attr, "");
            }
        }
        clientRep.setClientAuthenticatorType(JWTClientSecretAuthenticator.PROVIDER_ID);
        clientRes.update(clientRep);
    }

    private EventRepresentation doLogin() {
        oauth.doLogin("test-user@localhost", "password");
        EventRepresentation loginEvent = events.poll();
        EventAssertion.expectLoginSuccess(loginEvent).clientId(oauth.getClientId());
        realm.cleanup().add(r -> AdminApiUtil.findUserByUsernameId(r, "test-user@localhost").logout());
        return loginEvent;
    }

    private void setAlgorithmForClient(String algorithm) {
        ClientRepresentation clientRep = oauth.clientResource().toRepresentation();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setTokenEndpointAuthSigningAlg(algorithm);
        oauth.clientResource().update(clientRep);
        realm.cleanup().add(r -> {
            ClientResource res = r.clients().get(clientRep.getId());
            ClientRepresentation rep = res.toRepresentation();
            OIDCAdvancedConfigWrapper.fromClientRepresentation(rep).setTokenEndpointAuthSigningAlg("");
            res.update(rep);
        });
    }

    static class ClientAuthSecretSignedJWTRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.users(UserBuilder.create("test-user@localhost").password("password")
                    .name("Test", "User").email("test-user@localhost").emailVerified(true));
        }
    }

    static class OAuthClientConfig implements ClientConfig {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.clientId("test-app")
                    .secret(CLIENT_SECRET)
                    .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                    .publicClient(false)
                    .authenticatorType(JWTClientSecretAuthenticator.PROVIDER_ID)
                    .redirectUris("http://127.0.0.1:8500/callback/oauth");
        }
    }
}
