/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.adapters.AdapterUtils;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistration;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.common.util.Base64;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.Time;
import org.keycloak.common.util.UriUtils;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.Algorithm;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.representations.oidc.TokenMetadataRepresentation;
import org.keycloak.services.Urls;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPolicyProvider;
import org.keycloak.services.clientpolicy.DefaultClientPolicyProviderFactory;
import org.keycloak.services.clientpolicy.condition.ClientAccessTypeConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientPolicyConditionProvider;
import org.keycloak.services.clientpolicy.condition.ClientRolesConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientScopesConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdateContextConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdateSourceGroupsConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdateSourceHostsConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdateSourceRolesConditionFactory;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProvider;
import org.keycloak.services.clientpolicy.executor.SecureClientAuthEnforceExecutorFactory;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.client.resources.TestApplicationResourceUrls;
import org.keycloak.testsuite.client.resources.TestOIDCEndpointsApplicationResource;
import org.keycloak.testsuite.rest.resource.TestingOIDCEndpointsApplicationResource;
import org.keycloak.testsuite.rest.resource.TestingOIDCEndpointsApplicationResource.AuthorizationEndpointRequestObject;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.ServerURLs;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class AbstractClientPoliciesTest extends AbstractKeycloakTest {

    protected static final String REALM_NAME = "test";
    protected static final String TEST_CLIENT = "test-app";
    protected static final String TEST_CLIENT_SECRET = "password";

    protected static final String ERR_MSG_MISSING_NONCE = "Missing parameter: nonce";
    protected static final String ERR_MSG_MISSING_STATE = "Missing parameter: state";
    protected static final String ERR_MSG_CLIENT_REG_FAIL = "Failed to send request";

    protected static final String AnyClientCondition_NAME = "AnyClientCondition";
    protected static final String ClientAccessTypeCondition_NAME = "ClientAccessTypeCondition";
    protected static final String ClientRolesCondition_NAME = "ClientRolesCondition";
    protected static final String ClientScopesCondition_NAME = "ClientScopesCondition";
    protected static final String ClientUpdateContextCondition_NAME = "ClientUpdateContextCondition";
    protected static final String ClientUpdateSourceGroupsCondition_NAME = "ClientUpdateSourceGroupsCondition";
    protected static final String ClientUpdateSourceRolesCondition_NAME = "ClientUpdateSourceRolesCondition";
    protected static final String ClientUpdateSourceHostsCondition_NAME = "ClientUpdateSourceHostsCondition";
    protected static final String TestRaiseExeptionCondition_NAME = "TestRaiseExeptionCondition";

    protected static final String HolderOfKeyEnforceExecutor_NAME = "HolderOfKeyEnforceExecutor";
    protected static final String PKCEEnforceExecutor_NAME = "PKCEEnforceExecutor";
    protected static final String SecureClientAuthEnforceExecutor_NAME = "SecureClientAuthEnforceExecutor";
    protected static final String SecureRedirectUriEnforceExecutor_NAME = "SecureRedirectUriEnforceExecutor";
    protected static final String SecureResponseTypeExecutor_NAME = "SecureResponseTypeExecutor";
    protected static final String SecureRequestObjectExecutor_NAME = "SecureRequestObjectExecutor";
    protected static final String SecureSessionEnforceExecutor_NAME = "SecureSessionEnforceExecutor";
    protected static final String SecureSigningAlgorithmEnforceExecutor_NAME = "SecureSigningAlgorithmEnforceExecutor";
    protected static final String SecureSigningAlgorithmForSignedJwtEnforceExecutor_NAME = "SecureSigningAlgorithmForSignedJwtEnforceExecutor";

    protected ClientRegistration reg;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Before
    public void before() throws Exception {
        // get initial access token for Dynamic Client Registration with authentication
        reg = ClientRegistration.create().url(suiteContext.getAuthServerInfo().getContextRoot() + "/auth", REALM_NAME).build();
        ClientInitialAccessPresentation token = adminClient.realm(REALM_NAME).clientInitialAccess().create(new ClientInitialAccessCreatePresentation(0, 10));
        reg.auth(Auth.token(token));
    }

    @After
    public void after() throws Exception {
        reg.close();
    }

    protected String generateSuffixedName(String name) {
        return name + "-" + UUID.randomUUID().toString().subSequence(0, 7);
    }

    // Utilities for Request Object retrieved by reference from jwks_uri

    protected KeyPair setupJwks(String algorithm, ClientRepresentation clientRepresentation, ClientResource clientResource) throws Exception {
        // generate and register client keypair
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        oidcClientEndpointsResource.generateKeys(algorithm);
        Map<String, String> generatedKeys = oidcClientEndpointsResource.getKeysAsBase64();
        KeyPair keyPair = getKeyPairFromGeneratedBase64(generatedKeys, algorithm);

        // use and set jwks_url
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRepresentation).setUseJwksUrl(true);
        String jwksUrl = TestApplicationResourceUrls.clientJwksUri();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRepresentation).setJwksUrl(jwksUrl);
        clientResource.update(clientRepresentation);

        // set time offset, so that new keys are downloaded
        setTimeOffset(20);

        return keyPair;
    }

    private KeyPair getKeyPairFromGeneratedBase64(Map<String, String> generatedKeys, String algorithm) throws Exception {
        // It seems that PemUtils.decodePrivateKey, decodePublicKey can only treat RSA type keys, not EC type keys. Therefore, these are not used.
        String privateKeyBase64 = generatedKeys.get(TestingOIDCEndpointsApplicationResource.PRIVATE_KEY);
        String publicKeyBase64 =  generatedKeys.get(TestingOIDCEndpointsApplicationResource.PUBLIC_KEY);
        PrivateKey privateKey = decodePrivateKey(Base64.decode(privateKeyBase64), algorithm);
        PublicKey publicKey = decodePublicKey(Base64.decode(publicKeyBase64), algorithm);
        return new KeyPair(publicKey, privateKey);
    }

    private PrivateKey decodePrivateKey(byte[] der, String algorithm) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
        String keyAlg = getKeyAlgorithmFromJwaAlgorithm(algorithm);
        KeyFactory kf = KeyFactory.getInstance(keyAlg, "BC");
        return kf.generatePrivate(spec);
    }

    private PublicKey decodePublicKey(byte[] der, String algorithm) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
        String keyAlg = getKeyAlgorithmFromJwaAlgorithm(algorithm);
        KeyFactory kf = KeyFactory.getInstance(keyAlg, "BC");
        return kf.generatePublic(spec);
    }

    private String getKeyAlgorithmFromJwaAlgorithm(String jwaAlgorithm) {
        String keyAlg = null;
        switch (jwaAlgorithm) {
            case org.keycloak.crypto.Algorithm.RS256:
            case org.keycloak.crypto.Algorithm.RS384:
            case org.keycloak.crypto.Algorithm.RS512:
            case org.keycloak.crypto.Algorithm.PS256:
            case org.keycloak.crypto.Algorithm.PS384:
            case org.keycloak.crypto.Algorithm.PS512:
                keyAlg = KeyType.RSA;
                break;
            case org.keycloak.crypto.Algorithm.ES256:
            case org.keycloak.crypto.Algorithm.ES384:
            case org.keycloak.crypto.Algorithm.ES512:
                keyAlg = KeyType.EC;
                break;
            default :
                throw new RuntimeException("Unsupported signature algorithm");
        }
        return keyAlg;
    }

   // Signed JWT for client authentication utility

    protected String createSignedRequestToken(String clientId, PrivateKey privateKey, PublicKey publicKey, String algorithm) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        JsonWebToken jwt = createRequestToken(clientId, getRealmInfoUrl());
        String kid = KeyUtils.createKeyId(publicKey);
        SignatureSignerContext signer = oauth.createSigner(privateKey, kid, algorithm);
        return new JWSBuilder().kid(kid).jsonContent(jwt).sign(signer);
    }

    private String getRealmInfoUrl() {
        String authServerBaseUrl = UriUtils.getOrigin(oauth.getRedirectUri()) + "/auth";
        return KeycloakUriBuilder.fromUri(authServerBaseUrl).path(ServiceUrlConstants.REALM_INFO_PATH).build(REALM_NAME).toString();
    }

    private JsonWebToken createRequestToken(String clientId, String realmInfoUrl) {
        JsonWebToken reqToken = new JsonWebToken();
        reqToken.id(AdapterUtils.generateId());
        reqToken.issuer(clientId);
        reqToken.subject(clientId);
        reqToken.audience(realmInfoUrl);

        int now = Time.currentTime();
        reqToken.iat(Long.valueOf(now));
        reqToken.exp(Long.valueOf(now + 10));
        reqToken.nbf(Long.valueOf(now));

        return reqToken;
    }

    // OAuth2 protocol operation with signed JWT for client authentication

    protected OAuthClient.AccessTokenResponse doAccessTokenRequestWithSignedJWT(String code, String signedJwt) throws Exception {
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.AUTHORIZATION_CODE));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CODE, code));
        parameters.add(new BasicNameValuePair(OAuth2Constants.REDIRECT_URI, oauth.getRedirectUri()));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, signedJwt));

        CloseableHttpResponse response = sendRequest(oauth.getAccessTokenUrl(), parameters);
        return new OAuthClient.AccessTokenResponse(response);
    }

    protected OAuthClient.AccessTokenResponse doRefreshTokenRequestWithSignedJWT(String refreshToken, String signedJwt) throws Exception {
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.REFRESH_TOKEN));
        parameters.add(new BasicNameValuePair(OAuth2Constants.REFRESH_TOKEN, refreshToken));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, signedJwt));

        CloseableHttpResponse response = sendRequest(oauth.getRefreshTokenUrl(), parameters);
        return new OAuthClient.AccessTokenResponse(response);
    }

    protected HttpResponse doTokenIntrospectionWithSignedJWT(String tokenType, String tokenToIntrospect, String signedJwt) throws Exception {
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair("token", tokenToIntrospect));
        parameters.add(new BasicNameValuePair("token_type_hint", tokenType));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, signedJwt));

        return sendRequest(oauth.getTokenIntrospectionUrl(), parameters);
    }

    protected HttpResponse doTokenRevokeWithSignedJWT(String tokenType, String tokenToIntrospect, String signedJwt) throws Exception {
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair("token", tokenToIntrospect));
        parameters.add(new BasicNameValuePair("token_type_hint", tokenType));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, signedJwt));

        return sendRequest(oauth.getTokenRevocationUrl(), parameters);
    }

    protected HttpResponse doLogoutWithSignedJWT(String refreshToken, String signedJwt) throws Exception {
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.REFRESH_TOKEN));
        parameters.add(new BasicNameValuePair(OAuth2Constants.REFRESH_TOKEN, refreshToken));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, signedJwt));

        return sendRequest(oauth.getLogoutUrl().build(), parameters);
    }

    private CloseableHttpResponse sendRequest(String requestUrl, List<NameValuePair> parameters) throws Exception {
        CloseableHttpClient client = new DefaultHttpClient();
        try {
            HttpPost post = new HttpPost(requestUrl);
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, "UTF-8");
            post.setEntity(formEntity);
            return client.execute(post);
        } finally {
            oauth.closeClient(client);
        }
    }

    // Request Object utility

    protected AuthorizationEndpointRequestObject createValidRequestObjectForSecureRequestObjectExecutor(String clientId) throws URISyntaxException {
        AuthorizationEndpointRequestObject requestObject = new AuthorizationEndpointRequestObject();
        requestObject.id(KeycloakModelUtils.generateId());
        requestObject.iat(Long.valueOf(Time.currentTime()));
        requestObject.exp(requestObject.getIat() + Long.valueOf(300));
        requestObject.nbf(Long.valueOf(0));
        requestObject.setClientId(clientId);
        requestObject.setResponseType("code");
        requestObject.setRedirectUriParam(oauth.getRedirectUri());
        requestObject.setScope("openid");
        String scope = KeycloakModelUtils.generateId();
        oauth.stateParamHardcoded(scope);
        requestObject.setState(scope);
        requestObject.setMax_age(Integer.valueOf(600));
        requestObject.setOtherClaims("custom_claim_ein", "rot");
        requestObject.audience(Urls.realmIssuer(new URI(suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth"), REALM_NAME), "https://example.com");
        return requestObject;
    }

    protected void registerRequestObject(AuthorizationEndpointRequestObject requestObject, String clientId, Algorithm sigAlg, boolean isUseRequestUri) throws URISyntaxException, IOException {
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();

        // Set required signature for request_uri
        // use and set jwks_url
        ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm(REALM_NAME), clientId);
        ClientRepresentation clientRep = clientResource.toRepresentation();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestObjectSignatureAlg(sigAlg);
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUseJwksUrl(true);
        String jwksUrl = TestApplicationResourceUrls.clientJwksUri();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setJwksUrl(jwksUrl);
        clientResource.update(clientRep);

        oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();

        // generate and register client keypair
        oidcClientEndpointsResource.generateKeys(sigAlg.name());

        // register request object
        byte[] contentBytes = JsonSerialization.writeValueAsBytes(requestObject);
        String encodedRequestObject = Base64Url.encode(contentBytes);
        oidcClientEndpointsResource.registerOIDCRequest(encodedRequestObject, sigAlg.name());

        if (isUseRequestUri) {
            oauth.request(null);
            oauth.requestUri(TestApplicationResourceUrls.clientRequestUri());
        } else {
            oauth.requestUri(null);
            oauth.request(oidcClientEndpointsResource.getOIDCRequest());
        }
    }

    // PKCE utility

    protected String generateS256CodeChallenge(String codeVerifier) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(codeVerifier.getBytes("ISO_8859_1"));
        byte[] digestBytes = md.digest();
        String codeChallenge = Base64Url.encode(digestBytes);
        return codeChallenge;
    }

    // OAuth2 protocol operation

    protected void doIntrospectAccessToken(OAuthClient.AccessTokenResponse tokenRes, String username, String clientId, String clientSecret) throws IOException {
        String tokenResponse = oauth.introspectAccessTokenWithClientCredential(clientId, clientSecret, tokenRes.getAccessToken());
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(tokenResponse);
        assertEquals(true, jsonNode.get("active").asBoolean());
        assertEquals(username, jsonNode.get("username").asText());
        assertEquals(clientId, jsonNode.get("client_id").asText());
        TokenMetadataRepresentation rep = objectMapper.readValue(tokenResponse, TokenMetadataRepresentation.class);
        assertEquals(true, rep.isActive());
        assertEquals(clientId, rep.getClientId());
        assertEquals(clientId, rep.getIssuedFor());
        events.expect(EventType.INTROSPECT_TOKEN).client(clientId).user((String)null).clearDetails().assertEvent();
    }

    protected void doTokenRevoke(String refreshToken, String clientId, String clientSecret, String userId, boolean isOfflineAccess) throws IOException {
        oauth.clientId(clientId);
        oauth.doTokenRevoke(refreshToken, "refresh_token", clientSecret);

        // confirm revocation
        OAuthClient.AccessTokenResponse tokenRes = oauth.doRefreshTokenRequest(refreshToken, clientSecret);
        assertEquals(400, tokenRes.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_GRANT, tokenRes.getError());
        if (isOfflineAccess) assertEquals("Offline user session not found", tokenRes.getErrorDescription());
        else assertEquals("Session not active", tokenRes.getErrorDescription());

        events.expect(EventType.REVOKE_GRANT).clearDetails().client(clientId).user(userId).assertEvent();
    }

    // Client CRUD operation by Admin REST API primitives

    protected String createClientByAdmin(String clientName, Consumer<ClientRepresentation> op) throws ClientPolicyException {
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId(clientName);
        clientRep.setName(clientName);
        clientRep.setProtocol("openid-connect");
        clientRep.setBearerOnly(Boolean.FALSE);
        clientRep.setPublicClient(Boolean.FALSE);
        clientRep.setServiceAccountsEnabled(Boolean.TRUE);
        clientRep.setRedirectUris(Collections.singletonList(ServerURLs.getAuthServerContextRoot() + "/auth/realms/master/app/auth"));
        op.accept(clientRep);
        Response resp = adminClient.realm(REALM_NAME).clients().create(clientRep);
        if (resp.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()) {
            String respBody = resp.readEntity(String.class);
            Map<String, String> responseJson = null;
            try {
                responseJson = JsonSerialization.readValue(respBody, Map.class);
            } catch (IOException e) {
                fail();
            }
            throw new ClientPolicyException(responseJson.get(OAuth2Constants.ERROR), responseJson.get(OAuth2Constants.ERROR_DESCRIPTION));
        }
        resp.close();
        assertEquals(Response.Status.CREATED.getStatusCode(), resp.getStatus());
        // registered components will be removed automatically when a test method finishes regardless of its success or failure.
        String cId = ApiUtil.getCreatedId(resp);
        testContext.getOrCreateCleanup(REALM_NAME).addClientUuid(cId);
        return cId;
    }

    protected ClientRepresentation getClientByAdmin(String cId) throws ClientPolicyException {
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(cId);
        try {
            return clientResource.toRepresentation();
        } catch (BadRequestException bre) {
            processClientPolicyExceptionByAdmin(bre);
        }
        return null;
    }

    protected ClientRepresentation getClientByAdminWithName(String clientName) {
        return adminClient.realm(REALM_NAME).clients().findByClientId(clientName).get(0);
    }

    protected void updateClientByAdmin(String cId, Consumer<ClientRepresentation> op) throws ClientPolicyException {
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(cId);
        ClientRepresentation clientRep = clientResource.toRepresentation();
        op.accept(clientRep);
        try {
            clientResource.update(clientRep);
        } catch (BadRequestException bre) {
            processClientPolicyExceptionByAdmin(bre);
        }
    }

    protected void deleteClientByAdmin(String cId) throws ClientPolicyException {
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(cId);
        try {
            clientResource.remove();
        } catch (BadRequestException bre) {
            processClientPolicyExceptionByAdmin(bre);
        }
    }

    private void processClientPolicyExceptionByAdmin(BadRequestException bre) throws ClientPolicyException {
        Response resp = bre.getResponse();
        if (resp.getStatus() != Response.Status.BAD_REQUEST.getStatusCode()) {
            resp.close();
            return;
        }

        String respBody = resp.readEntity(String.class);
        Map<String, String> responseJson = null;
        try {
            responseJson = JsonSerialization.readValue(respBody, Map.class);
        } catch (IOException e) {
            fail();
        }
        throw new ClientPolicyException(responseJson.get(OAuth2Constants.ERROR), responseJson.get(OAuth2Constants.ERROR_DESCRIPTION));
    }

    // Registration/Initial Access Token acquisition for Dynamic Client Registration

    protected void authCreateClients() {
        reg.auth(Auth.token(getToken("create-clients", "password")));
    }

    protected void authManageClients() {
        reg.auth(Auth.token(getToken("manage-clients", "password")));
    }

    protected void authNoAccess() {
        reg.auth(Auth.token(getToken("no-access", "password")));
    }

    private String getToken(String username, String password) {
        try {
            return oauth.doGrantAccessTokenRequest(REALM_NAME, username, password, null, Constants.ADMIN_CLI_CLIENT_ID, null).getAccessToken();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Client CRUD operation by Dynamic Client Registration primitives

    protected String createClientDynamically(String clientName, Consumer<OIDCClientRepresentation> op) throws ClientRegistrationException {
        OIDCClientRepresentation clientRep = new OIDCClientRepresentation();
        clientRep.setClientName(clientName);
        clientRep.setClientUri(ServerURLs.getAuthServerContextRoot());
        clientRep.setRedirectUris(Collections.singletonList(ServerURLs.getAuthServerContextRoot() + "/auth/realms/master/app/auth"));
        op.accept(clientRep);
        OIDCClientRepresentation response = reg.oidc().create(clientRep);
        reg.auth(Auth.token(response));
        // registered components will be removed automatically when a test method finishes regardless of its success or failure.
        String clientId = response.getClientId();
        testContext.getOrCreateCleanup(REALM_NAME).addClientUuid(clientId);
        return clientId;
    }

    protected OIDCClientRepresentation getClientDynamically(String clientId) throws ClientRegistrationException {
        return reg.oidc().get(clientId);
    }

    protected void updateClientDynamically(String clientId, Consumer<OIDCClientRepresentation> op) throws ClientRegistrationException {
        OIDCClientRepresentation clientRep = reg.oidc().get(clientId);
        op.accept(clientRep);
        OIDCClientRepresentation response = reg.oidc().update(clientRep);
        reg.auth(Auth.token(response));
    }

    protected void deleteClientDynamically(String clientId) throws ClientRegistrationException {
        reg.oidc().delete(clientId);
    }

    // Policy CRUD operation primitives

    protected String createPolicy(String name, String providerId, String subType, List<String> conditions, List<String> executors) {
        ComponentRepresentation component = createComponentInstance(name, providerId, ClientPolicyProvider.class.getName(), subType);
        component.getConfig().put(DefaultClientPolicyProviderFactory.CONDITION_IDS, conditions);
        component.getConfig().put(DefaultClientPolicyProviderFactory.EXECUTOR_IDS, executors);
        return createComponent(component);
    }

    protected ComponentRepresentation getPolicy(String name) {
        return getComponent(name, ClientPolicyProvider.class.getName());
    }

    protected void updatePolicy(ComponentRepresentation policy) {
        updateComponent(policy);
    }

    protected void deletePolicy(String policyName) {
        ComponentRepresentation policy = getPolicy(policyName);
        List<String> conditionIds = policy.getConfig().get(DefaultClientPolicyProviderFactory.CONDITION_IDS);
        List<String> executorIds = policy.getConfig().get(DefaultClientPolicyProviderFactory.EXECUTOR_IDS);
        conditionIds.stream().forEach(i->adminClient.realm(REALM_NAME).components().component(i).remove());
        executorIds.stream().forEach(i->adminClient.realm(REALM_NAME).components().component(i).remove());
        adminClient.realm(REALM_NAME).components().component(policy.getId()).remove();
    }

    // Condition CRUD operation primitives

    // only create the component
    protected String createCondition(String name, String providerId, String subType, Consumer<ComponentRepresentation> op) {
        ComponentRepresentation component = createComponentInstance(name, providerId, ClientPolicyConditionProvider.class.getName(), subType);
        op.accept(component);
        return createComponent(component);
    }

    // register created component to the policy
    protected void registerCondition(String conditionName, String policyName) {
        ComponentRepresentation policy = getPolicy(policyName);
        List<String> conditionIds = policy.getConfig().get(DefaultClientPolicyProviderFactory.CONDITION_IDS);
        if (conditionIds == null) conditionIds = new ArrayList<String>();
        ComponentRepresentation condition = getCondition(conditionName);
        conditionIds.add(condition.getId());
        policy.getConfig().put(DefaultClientPolicyProviderFactory.CONDITION_IDS, conditionIds);
        updatePolicy(policy);
    }

    protected ComponentRepresentation getCondition(String name) {
        return getComponent(name, ClientPolicyConditionProvider.class.getName());
    }

    protected void updateCondition(String name, Consumer<ComponentRepresentation> op) {
        ComponentRepresentation condition = getCondition(name);
        op.accept(condition);
        updateComponent(condition);
    }

    protected void deleteCondition(String conditionName, String policyName) {
        ComponentRepresentation policy = getPolicy(policyName);
        List<String> conditionIds = policy.getConfig().get(DefaultClientPolicyProviderFactory.CONDITION_IDS);
        ComponentRepresentation condition = getCondition(conditionName);
        String conditionId = condition.getId();
        adminClient.realm(REALM_NAME).components().component(conditionId).remove();
        conditionIds.remove(conditionId);
        policy.getConfig().put(DefaultClientPolicyProviderFactory.CONDITION_IDS, conditionIds);
        updatePolicy(policy);
    }

    // Condition configuration primitives

    protected void setConditionRegistrationMethods(ComponentRepresentation provider, List<String> registrationMethods) {
        provider.getConfig().put(ClientUpdateContextConditionFactory.UPDATE_CLIENT_SOURCE, registrationMethods);
    }

    protected void setConditionClientRoles(ComponentRepresentation provider, List<String> clientRoles) {
        provider.getConfig().put(ClientRolesConditionFactory.ROLES, clientRoles);
    }

    protected void setConditionClientScopes(ComponentRepresentation provider, List<String> clientScopes) {
        provider.getConfig().put(ClientScopesConditionFactory.SCOPES, clientScopes);
    }

    protected void setConditionClientAccessType(ComponentRepresentation provider, List<String> clientAccessTypes) {
        provider.getConfig().put(ClientAccessTypeConditionFactory.TYPE, clientAccessTypes);
    }

    protected void setConditionClientUpdateSourceHosts(ComponentRepresentation provider, List<String> hosts) {
        provider.getConfig().putSingle(ClientUpdateSourceHostsConditionFactory.HOST_SENDING_REQUEST_MUST_MATCH, "true");
        provider.getConfig().put(ClientUpdateSourceHostsConditionFactory.TRUSTED_HOSTS, hosts);
    }

    protected void setConditionClientUpdateSourceGroups(ComponentRepresentation provider, List<String> groups) {
        provider.getConfig().put(ClientUpdateSourceGroupsConditionFactory.GROUPS, groups);
    }

    protected void setConditionUpdatingClientSourceRoles(ComponentRepresentation provider, List<String> groups) {
        provider.getConfig().put(ClientUpdateSourceRolesConditionFactory.ROLES, groups);
    }

    // Executor CRUD operation primitives

    // only create the component
    protected String createExecutor(String name, String providerId, String subType, Consumer<ComponentRepresentation> op) {
        ComponentRepresentation component = createComponentInstance(name, providerId, ClientPolicyExecutorProvider.class.getName(), subType);
        op.accept(component);
        return createComponent(component);
    }

    // register created component to the policy
    protected void registerExecutor(String executorName, String policyName) {
        ComponentRepresentation policy = getPolicy(policyName);
        List<String> executorIds = policy.getConfig().get(DefaultClientPolicyProviderFactory.EXECUTOR_IDS);
        if (executorIds == null) executorIds = new ArrayList<String>();
        ComponentRepresentation executor = getExecutor(executorName);
        executorIds.add(executor.getId());
        policy.getConfig().put(DefaultClientPolicyProviderFactory.EXECUTOR_IDS, executorIds);
        updatePolicy(policy);
    }

    protected ComponentRepresentation getExecutor(String name) {
        return getComponent(name, ClientPolicyExecutorProvider.class.getName());
    }

    protected void updateExecutor(String name, Consumer<ComponentRepresentation> op) {
        ComponentRepresentation executor = getExecutor(name);
        op.accept(executor);
        updateComponent(executor);
    }

    protected void deleteExecutor(String executorName, String policyName) {
        ComponentRepresentation policy = getPolicy(policyName);
        List<String> executorIds = policy.getConfig().get(DefaultClientPolicyProviderFactory.EXECUTOR_IDS);
        ComponentRepresentation executor = getExecutor(executorName);
        String executorId = executor.getId();
        adminClient.realm(REALM_NAME).components().component(executorId).remove();
        executorIds.remove(executorId);
        policy.getConfig().put(DefaultClientPolicyProviderFactory.EXECUTOR_IDS, executorIds);
        updatePolicy(policy);
    }

    // Executor configuration primitives

    protected void setExecutorAugmentActivate(ComponentRepresentation provider) {
        provider.getConfig().putSingle("is-augment", Boolean.TRUE.toString());
    }

    protected void setExecutorAugmentDeactivate(ComponentRepresentation provider) {
        provider.getConfig().putSingle("is-augment", Boolean.FALSE.toString());
    }

    protected void setExecutorAcceptedClientAuthMethods(ComponentRepresentation provider, List<String> acceptedClientAuthMethods) {
        provider.getConfig().put(SecureClientAuthEnforceExecutorFactory.CLIENT_AUTHNS, acceptedClientAuthMethods);
    }

    protected void setExecutorAugmentedClientAuthMethod(ComponentRepresentation provider, String augmentedClientAuthMethod) {
        provider.getConfig().putSingle(SecureClientAuthEnforceExecutorFactory.CLIENT_AUTHNS_AUGMENT, augmentedClientAuthMethod);
    }

    // Component on which condition/executor/policy based CRUD operation primitives

    private ComponentRepresentation createComponentInstance(String name, String providerId, String providerType, String subType) {
        ComponentRepresentation rep = new ComponentRepresentation();
        rep.setId(org.keycloak.models.utils.KeycloakModelUtils.generateId());
        rep.setName(name);
        rep.setParentId(REALM_NAME);
        rep.setProviderId(providerId);
        rep.setProviderType(providerType);
        rep.setSubType(subType);
        rep.setConfig(new MultivaluedHashMap<>());
        return rep;
    }

    private String createComponent(ComponentRepresentation cr) {
        Response resp = adminClient.realm(REALM_NAME).components().add(cr);
        String id = ApiUtil.getCreatedId(resp);
        resp.close();
        // registered components will be removed automatically when a test method finishes regardless of its success or failure.
        testContext.getOrCreateCleanup(REALM_NAME).addComponentId(id);
        return id;
    }

    private ComponentRepresentation getComponent(String name, String providerType) {
        return adminClient.realm(REALM_NAME).components().query(null, providerType, name).get(0);
    }

    private void updateComponent(ComponentRepresentation cr) {
        adminClient.realm(REALM_NAME).components().component(cr.getId()).update(cr);
    }

    private void deleteComponent(String id) {
        adminClient.realm(REALM_NAME).components().component(id).remove();
    }
}
