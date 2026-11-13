/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.oidc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientScopesResource;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.PemUtils;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.AesCbcHmacShaContentEncryptionProvider;
import org.keycloak.crypto.AesGcmContentEncryptionProvider;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.RsaCekManagementProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.jose.JOSEHeader;
import org.keycloak.jose.jwe.JWEConstants;
import org.keycloak.jose.jwe.JWEException;
import org.keycloak.jose.jwe.JWEHeader;
import org.keycloak.jose.jwe.alg.JWEAlgorithmProvider;
import org.keycloak.jose.jwe.enc.JWEEncryptionProvider;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.UserInfo;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.JwksProvider;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectJwksProvider;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RepresentationUtils;
import org.keycloak.testframework.realm.RoleBuilder;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.Assert;
import org.keycloak.tests.utils.KeyUtils;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.runonserver.RunHelpers;
import org.keycloak.util.BasicAuthHelper;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;
import org.keycloak.utils.MediaType;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static jakarta.ws.rs.client.ClientBuilder.newClient;

import static org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author pedroigor
 */
@KeycloakIntegrationTest
public class UserInfoTest {

    @InjectRealm(config = TestRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectOAuthClient(config = TestClientConfig.class, lifecycle = LifeCycle.METHOD)
    OAuthClient oauth;

    @InjectJwksProvider
    JwksProvider jwksProvider;

    @InjectEvents
    Events events;

    @InjectPage
    protected LoginPage loginPage;

    @BeforeEach
    public void configureUserRoles() {
        // Keep real client roles on the user even when asserting their absence from UserInfo.
        ClientResource client = oauth.clientResource();
        client.roles().create(RoleBuilder.create().name("customer-user").build());
        RoleRepresentation role = client.roles().get("customer-user").toRepresentation();
        AdminApiUtil.findUserByUsernameId(managedRealm.admin(), "test-user@localhost")
                .roles().clientLevel(client.toRepresentation().getId()).add(Collections.singletonList(role));
    }

    @AfterEach
    public void removeUserSessions() {
        runOnServer.run(RunHelpers.removeUserSessions());
    }

    public void testSuccessGet(String acceptHeader) throws Exception {
        try (Client client = newClient()) {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);
            Response response = executeUserInfoRequest(client, accessTokenResponse.getToken(), acceptHeader);

            UserInfo userInfo = testSuccessfulUserInfoResponse(response);
            testRolesAreNotInUserInfoResponse(userInfo);

        }
    }

    @Test
    public void testSuccess_getMethod_header() throws Exception {
        testSuccessGet(null);
    }

    @Test
    public void testSuccess_getMethod_header_accept_json() throws Exception {
        testSuccessGet(MediaType.APPLICATION_JSON);
    }

    @Test
    public void testSuccess_postMethod_header() throws Exception {
        try (Client client = newClient()) {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);

            WebTarget userInfoTarget = getUserInfoWebTarget(client);
            Response response = userInfoTarget.request()
                    .header(HttpHeaders.AUTHORIZATION, "bearer " + accessTokenResponse.getToken())
                    .post(Entity.form(new Form()));

            testSuccessfulUserInfoResponse(response);

        }
    }

    @Test
    public void testSuccess_postMethod_body() throws Exception {
        try (Client client = newClient()) {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);

            Form form = new Form();
            form.param("access_token", accessTokenResponse.getToken());

            WebTarget userInfoTarget = getUserInfoWebTarget(client);
            Response response = userInfoTarget.request()
                    .post(Entity.form(form));

            testSuccessfulUserInfoResponse(response);

        }
    }

    @Test
    public void testSuccess_postMethod_charset_body() throws Exception {
        try (Client client = newClient()) {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);

            Form form = new Form();
            form.param("access_token", accessTokenResponse.getToken());

            WebTarget userInfoTarget = getUserInfoWebTarget(client);
            Response response = userInfoTarget.request()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_TYPE.withCharset("utf-8"))
                    .post(Entity.form(form));

            testSuccessfulUserInfoResponse(response);

        }
    }


    // KEYCLOAK-8838
    @Test
    public void testSuccess_dotsInClientId() throws Exception {
        // Create client with dot in the name
        final String clientId = "my.foo.$\\client\\$";
        ClientRepresentation clientRep = ClientBuilder.create()
                .clientId(clientId)
                .redirectUris("http://foo.host")
                .secret("password")
                .directAccessGrantsEnabled()
                .build();

        RealmResource realm = managedRealm.admin();

        Response resp = realm.clients().create(clientRep);
        String clientUUID = ApiUtil.getCreatedId(resp);
        resp.close();
        managedRealm.cleanup().add(realmResource -> realmResource.clients().get(clientUUID).remove());

        //Create role with dot in the name
        realm.clients().get(clientUUID).roles().create(RoleBuilder.create().name("my.foo.role").build());

        // Assign role to the user
        RoleRepresentation fooRole = realm.clients().get(clientUUID).roles().get("my.foo.role").toRepresentation();
        UserResource userResource = AdminApiUtil.findUserByUsernameId(realm, "test-user@localhost");
        userResource.roles().clientLevel(clientUUID).add(Collections.singletonList(fooRole));

        // Login to the new client
        org.keycloak.testsuite.util.oauth.AccessTokenResponse accessTokenResponse = oauth.client(clientId, "password")
                .doPasswordGrantRequest("test-user@localhost", "password");

        AccessToken accessToken = oauth.verifyToken(accessTokenResponse.getAccessToken());
        Assert.assertNames(accessToken.getResourceAccess(clientId).getRoles(), "my.foo.role");

        events.clear();

        // Send UserInfo request and ensure it is correct
        try (Client client = newClient()) {
            Response response = executeUserInfoRequest(client, accessTokenResponse.getAccessToken());

            testSuccessfulUserInfoResponse(response, clientId);
        }
    }

    @Test
    public void testSuccess_postMethod_header_textEntity() throws Exception {
        try (Client client = newClient()) {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);

            WebTarget userInfoTarget = getUserInfoWebTarget(client);
            Response response = userInfoTarget.request()
                    .header(HttpHeaders.AUTHORIZATION, "bearer " + accessTokenResponse.getToken())
                    .post(Entity.text(""));

            testSuccessfulUserInfoResponse(response);

        }
    }

    @Test
    public void testSuccessEncryptedResponseSigAlgPS384AlgRSA_OAEPEncA256GCM() throws Exception {
        testUserInfoSignatureAndEncryption(Algorithm.PS384, JWEConstants.RSA_OAEP, JWEConstants.A256GCM);
    }

    @Test
    public void testSuccessEncryptedResponseSigAlgRS256AlgRSA_OAEP256EncA192CBC_HS384() throws Exception {
        testUserInfoSignatureAndEncryption(Algorithm.RS256, JWEConstants.RSA_OAEP_256, JWEConstants.A192CBC_HS384);
    }

    @Test
    public void testSuccessEncryptedResponseSigAlgES512AlgRSA1_5EncDefault() throws Exception {
        testUserInfoSignatureAndEncryption(Algorithm.ES512, JWEConstants.RSA1_5, null);
    }

    @Test
    public void testSuccessEncryptedResponseSigAlgES384AlgRSA_OAEPEncA128GCM() throws Exception {
        testUserInfoSignatureAndEncryption(Algorithm.ES384, JWEConstants.RSA_OAEP, JWEConstants.A128GCM);
    }

    @Test
    public void testSuccessEncryptedResponseSigAlgPS256AlgRSA_OAEP256EncA256CBC_HS512() throws Exception {
        testUserInfoSignatureAndEncryption(Algorithm.PS256, JWEConstants.RSA_OAEP_256, JWEConstants.A256CBC_HS512);
    }

    @Test
    public void testSuccessEncryptedResponseSigAlgNoneAlgRSA1_5EncDefault() throws Exception {
        testUserInfoSignatureAndEncryption(null, JWEConstants.RSA1_5, null);
    }

    @Test
    public void testSuccessEncryptedResponseSigAlgEd25519AlgRSA_OAEPEncA256GCM() throws Exception {
        testUserInfoSignatureAndEncryption(Algorithm.EdDSA, Algorithm.Ed25519, JWEConstants.RSA_OAEP, JWEConstants.A256GCM);
    }

    @Test
    public void testSuccessEncryptedResponseSigAlgEd448AlgRSA_OAEP256EncA256CBC_HS512() throws Exception {
        testUserInfoSignatureAndEncryption(Algorithm.EdDSA, Algorithm.Ed448, JWEConstants.RSA_OAEP_256, JWEConstants.A256CBC_HS512);
    }

    private void testUserInfoSignatureAndEncryption(String sigAlgorithm, String algAlgorithm, String encAlgorithm) {
        testUserInfoSignatureAndEncryption(sigAlgorithm, null, algAlgorithm, encAlgorithm);
    }

    private void testUserInfoSignatureAndEncryption(String sigAlgorithm, String curve, String algAlgorithm, String encAlgorithm) {
        try (Client client = newClient()) {
            // generate and register encryption key onto client
            Map<String, String> keyPair = jwksProvider.generateKeys(algAlgorithm, curve, true, false, null);

            ClientResource clientResource = oauth.clientResource();
            ClientRepresentation clientRep = clientResource.toRepresentation();
            // set UserInfo response signature algorithm and encryption algorithms
            if(sigAlgorithm != null) {
                OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUserInfoSignedResponseAlg(sigAlgorithm);
            }
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUserInfoEncryptedResponseAlg(algAlgorithm);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUserInfoEncryptedResponseEnc(encAlgorithm);
            // use and set jwks_url
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUseJwksUrl(true);
            String jwksUrl = jwksProvider.getUri();
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setJwksUrl(jwksUrl);
            clientResource.update(clientRep);

            // get User Info response
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);
            Response response = executeUserInfoRequest(client, accessTokenResponse.getToken());

            Assertions.assertEquals(200, response.getStatus());
            Assertions.assertEquals(response.getHeaderString(HttpHeaders.CONTENT_TYPE), MediaType.APPLICATION_JWT);
            String encryptedResponse = response.readEntity(String.class);
            response.close();

            // parse JWE and JOSE Header
            String[] parts = encryptedResponse.split("\\.");
            Assertions.assertEquals(parts.length, 5);

            // get decryption key
            // not publickey , use privateKey
            PrivateKey decryptionKEK = PemUtils.decodePrivateKey(keyPair.get("privateKey"));

            // a nested JWT (signed and encrypted JWT) needs to set "JWT" to its JOSE Header's "cty" field
            JWEHeader jweHeader = (JWEHeader) getHeader(parts[0]);
            Assertions.assertEquals(algAlgorithm, jweHeader.getAlgorithm());
            if(encAlgorithm != null) {
                Assertions.assertEquals(encAlgorithm, jweHeader.getEncryptionAlgorithm());
            } else {
                // if enc algorithm is not specified the default for this value is A128CBC-HS256
                Assertions.assertEquals(JWEConstants.A128CBC_HS256, jweHeader.getEncryptionAlgorithm());
            }
            if(sigAlgorithm != null) {
                Assertions.assertEquals("JWT", jweHeader.getContentType());
            }

            // verify and decrypt JWE
            JWEAlgorithmProvider algorithmProvider = getJweAlgorithmProvider(algAlgorithm);
            // if enc algorithm is not specified the default for this value is A128CBC-HS256
            JWEEncryptionProvider encryptionProvider = encAlgorithm != null ? getJweEncryptionProvider(encAlgorithm) :
                    getJweEncryptionProvider(JWEConstants.A128CBC_HS256);
            byte[] decodedString = TokenUtil.jweKeyEncryptionVerifyAndDecode(decryptionKEK, encryptedResponse, algorithmProvider, encryptionProvider);
            String jwePayload = new String(decodedString, StandardCharsets.UTF_8);

            UserInfo userInfo = null;
            // verify JWS
            if (sigAlgorithm != null) {
                // verify signature
                oauth.verifyToken(jwePayload, JsonWebToken.class);
                JWSInput jwsInput = new JWSInput(jwePayload);
                userInfo = JsonSerialization.readValue(jwsInput.getContent(), UserInfo.class);
            } else {
                userInfo = JsonSerialization.readValue(jwePayload, UserInfo.class);
            }
            Assertions.assertNotNull(userInfo);
            Assertions.assertNotNull(userInfo.getSubject());
            Assertions.assertEquals("test-user@localhost", userInfo.getEmail());
            Assertions.assertEquals("test-user@localhost", userInfo.getPreferredUsername());
        } catch (JWSInputException | JWEException | IOException e) {
            Assertions.fail(e);
        }
    }

    private JWEAlgorithmProvider getJweAlgorithmProvider(String algAlgorithm) {
        return new RsaCekManagementProvider(null, algAlgorithm).jweAlgorithmProvider();
    }
    private JWEEncryptionProvider getJweEncryptionProvider(String encAlgorithm) {
        JWEEncryptionProvider jweEncryptionProvider = null;
        switch(encAlgorithm) {
            case JWEConstants.A128GCM:
            case JWEConstants.A192GCM:
            case JWEConstants.A256GCM:
                jweEncryptionProvider = new AesGcmContentEncryptionProvider(null, encAlgorithm).jweEncryptionProvider();
                break;
            case JWEConstants.A128CBC_HS256:
            case JWEConstants.A192CBC_HS384:
            case JWEConstants.A256CBC_HS512:
                jweEncryptionProvider = new AesCbcHmacShaContentEncryptionProvider(null, encAlgorithm).jweEncryptionProvider();
                break;
        }
        return jweEncryptionProvider;
    }

    private JOSEHeader getHeader(String base64Header) {
        try {
            byte[] decodedHeader = Base64Url.decode(base64Header);
            return JsonSerialization.readValue(decodedHeader, JWEHeader.class);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @Test
    public void testSuccessSignedResponse() throws Exception {
        // Require signed userInfo request
        ClientResource clientResource = oauth.clientResource();
        ClientRepresentation clientRep = clientResource.toRepresentation();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUserInfoSignedResponseAlg(Algorithm.RS256);
        clientResource.update(clientRep);

        // test signed response
        try (Client client = newClient()) {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);

            Response response = executeUserInfoRequest(client, accessTokenResponse.getToken());

            EventAssertion.assertSuccess(events.poll()).type(EventType.USER_INFO_REQUEST)
                    .hasSessionId()
                    .details(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN)
                    .details(Details.USERNAME, "test-user@localhost")
                    .details(Details.SIGNATURE_REQUIRED, "true")
                    .details(Details.SIGNATURE_ALGORITHM, Algorithm.RS256);

            // Check signature and content
            PublicKey publicKey = PemUtils.decodePublicKey(KeyUtils.findActiveSigningKey(managedRealm.admin()).getPublicKey());

            Assertions.assertEquals(200, response.getStatus());
            Assertions.assertEquals(response.getHeaderString(HttpHeaders.CONTENT_TYPE), MediaType.APPLICATION_JWT);
            String signedResponse = response.readEntity(String.class);
            response.close();

            JWSInput jwsInput = new JWSInput(signedResponse);
            Assertions.assertTrue(RSAProvider.verify(jwsInput, publicKey));

            UserInfo userInfo = JsonSerialization.readValue(jwsInput.getContent(), UserInfo.class);

            Assertions.assertNotNull(userInfo);
            Assertions.assertNotNull(userInfo.getSubject());
            Assertions.assertEquals("test-user@localhost", userInfo.getEmail());
            Assertions.assertEquals("test-user@localhost", userInfo.getPreferredUsername());

            Assertions.assertTrue(userInfo.hasAudience("test-app"));
            String expectedIssuer = managedRealm.getBaseUrl();
            Assertions.assertEquals(expectedIssuer, userInfo.getIssuer());

        }

    }

    @Test
    public void testSuccessSignedResponseES256() throws Exception {
        testSuccessSignedResponse(Algorithm.ES256, null);
    }

    @Test
    public void testSuccessSignedResponsePS256() throws Exception {
        testSuccessSignedResponse(Algorithm.PS256, null);
    }

    @Test
    public void testSuccessSignedResponseRS256AcceptJWT() throws Exception {
        testSuccessSignedResponse(Algorithm.RS256, MediaType.APPLICATION_JWT);
    }

    @Test
    public void testSessionExpired() {
        try (Client client = newClient()) {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);

            String realmName = managedRealm.getName();
            runOnServer.run(RunHelpers.removeUserSessions());

            Response response = executeUserInfoRequest(client, accessTokenResponse.getToken());

            assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
            String wwwAuthHeader = response.getHeaderString(HttpHeaders.WWW_AUTHENTICATE);
            assertNotNull(wwwAuthHeader);
            assertThat(wwwAuthHeader, CoreMatchers.containsString("Bearer"));
            assertThat(wwwAuthHeader, CoreMatchers.containsString("realm=\"" + realmName + "\""));
            assertThat(wwwAuthHeader, CoreMatchers.containsString("error=\"" + OAuthErrorException.INVALID_TOKEN + "\""));

            response.close();

            EventAssertion.assertError(events.poll()).type(EventType.USER_INFO_REQUEST_ERROR)
                    .error(Errors.USER_SESSION_NOT_FOUND)
                    .userId(null)
                    .sessionId(accessTokenResponse.getSessionState())
                    .details(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN);
            Assertions.assertNull(events.poll());

        }
    }

    @Test
    public void testAccessTokenExpired() {
        try (Client client = newClient()) {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);

            timeOffSet.set(600);

            Response response = executeUserInfoRequest(client, accessTokenResponse.getToken());

            assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

            String wwwAuthHeader = response.getHeaderString(HttpHeaders.WWW_AUTHENTICATE);
            assertNotNull(wwwAuthHeader);
            assertThat(wwwAuthHeader, CoreMatchers.containsString("Bearer"));
            assertThat(wwwAuthHeader, CoreMatchers.containsString("error=\"" + OAuthErrorException.INVALID_TOKEN + "\""));

            response.close();

            EventAssertion.assertError(events.poll()).type(EventType.USER_INFO_REQUEST_ERROR)
                    .error(Errors.INVALID_TOKEN)
                    .userId(null)
                    .sessionId(null)
                    .details(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN)
                    .clientId(null);
        }
    }

    @Test
    public void testAccessTokenAfterUserSessionLogoutAndLoginAgain() {
        org.keycloak.testsuite.util.oauth.AccessTokenResponse accessTokenResponse = loginAndForceNewLoginPage();
        String refreshToken1 = accessTokenResponse.getRefreshToken();

        oauth.doLogout(refreshToken1);
        events.clear();

        timeOffSet.set(2);

        driver.navigate().refresh();
        oauth.fillLoginForm("test-user@localhost", "password");
        EventAssertion.expectLoginSuccess(events.poll());

        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());

        events.clear();

        try (Client client = newClient()) {
            Response response = executeUserInfoRequest(client, accessTokenResponse.getAccessToken());

            assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

            String wwwAuthHeader = response.getHeaderString(HttpHeaders.WWW_AUTHENTICATE);
            assertNotNull(wwwAuthHeader);
            assertThat(wwwAuthHeader, CoreMatchers.containsString("Bearer"));
            assertThat(wwwAuthHeader, CoreMatchers.containsString("error=\"" + OAuthErrorException.INVALID_TOKEN + "\""));

            response.close();

            EventAssertion.assertError(events.poll()).type(EventType.USER_INFO_REQUEST_ERROR)
                    .error(Errors.USER_SESSION_NOT_FOUND)
                    .userId(null)
                    .sessionId(accessTokenResponse.getSessionState())
                    .details(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN)
                    .clientId("test-app");
        }
    }

    // Issue 39037
    @Test
    public void testUserInfoWithOfflineAccessAndHybridFlow() throws Exception {
        ClientRepresentation clientRep = oauth.clientResource().toRepresentation();
        clientRep.setImplicitFlowEnabled(true);
        oauth.clientResource().update(clientRep);
        try (Client client = newClient()) {
            oauth.scope(OAuth2Constants.SCOPE_OPENID + " " + OAuth2Constants.OFFLINE_ACCESS)
                    .responseType(OIDCResponseType.CODE + " " + OIDCResponseType.TOKEN)
                    .doLogin("test-user@localhost", "password");
            AuthorizationEndpointResponse authzEndpointResponse = oauth.parseLoginResponse();

            // UserInfo request with the accessToken returned from authz endpoint
            Response response1 = executeUserInfoRequest(client, authzEndpointResponse.getAccessToken());
            assertEquals(Response.Status.Family.SUCCESSFUL, response1.getStatusInfo().getFamily());
            response1.close();

            org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(authzEndpointResponse.getCode());

            // Another userInfo request with the accessToken (but after tokens are exchanged)
            Response response2 = executeUserInfoRequest(client, authzEndpointResponse.getAccessToken());
            assertEquals(Response.Status.Family.SUCCESSFUL, response2.getStatusInfo().getFamily());
            response2.close();

            // UserInfo request with the token returned from token response
            Response response3 = executeUserInfoRequest(client, tokenResponse.getAccessToken());
            assertEquals(Response.Status.Family.SUCCESSFUL, response3.getStatusInfo().getFamily());
            response3.close();
        }
    }

    @Test
    public void testNotBeforeTokens() {
        try (Client client = newClient()) {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);

            int time = Time.currentTime() + 60;

            RealmResource realm = managedRealm.admin();
            RealmRepresentation rep = realm.toRepresentation();
            RealmRepresentation originalRealm = RepresentationUtils.clone(rep);
            managedRealm.cleanup().add(r -> r.update(originalRealm));
            rep.setNotBefore(time);
            realm.update(rep);

            Response response = executeUserInfoRequest(client, accessTokenResponse.getToken());

            assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

            response.close();

            EventAssertion.assertError(events.poll()).type(EventType.USER_INFO_REQUEST_ERROR)
                    .error(Errors.INVALID_TOKEN)
                    .userId(null)
                    .sessionId(null)
                    .details(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN)
                    .clientId(null);

            events.clear();
            rep.setNotBefore(0);
            realm.update(rep);

            // do the same with client's notBefore
            ClientResource clientResource = oauth.clientResource();
            ClientRepresentation clientRep = clientResource.toRepresentation();
            clientRep.setNotBefore(time);
            clientResource.update(clientRep);

            response.close();
            response = executeUserInfoRequest(client, accessTokenResponse.getToken());

            assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

            response.close();

            EventAssertion.assertError(events.poll()).type(EventType.USER_INFO_REQUEST_ERROR)
                    .error(Errors.INVALID_TOKEN)
                    .userId(null)
                    .sessionId(null)
                    .details(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN)
                    .clientId(null);

            clientRep.setNotBefore(0);
            clientResource.update(clientRep);

            //Test realm notBefore with client notBefore both set
            // Set client notBefore to past
            clientRep.setNotBefore(time - 200);
            clientResource.update(clientRep);

            // Set realm notBefore to future
            rep.setNotBefore(time);
            realm.update(rep);

            response.close();
            response = executeUserInfoRequest(client, accessTokenResponse.getToken());

            assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

            EventAssertion.assertError(events.poll()).type(EventType.USER_INFO_REQUEST_ERROR)
                    .error(Errors.INVALID_TOKEN)
                    .sessionId(null)
                    .details(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN)
                    .clientId(null);

            rep.setNotBefore(0);
            realm.update(rep);
            clientRep.setNotBefore(0);
            clientResource.update(clientRep);

            managedRealm.cleanup().add(r -> runOnServer.run(session -> {
                RealmModel realmModel = session.getContext().getRealm();
                UserModel userModel = session.users().getUserByUsername(realmModel, "test-user@localhost");
                session.users().setNotBeforeForUser(realmModel, userModel, 0);
            }));
            runOnServer.run(session -> {
                RealmModel realmModel = session.getContext().getRealm();
                UserModel userModel = session.users().getUserByUsername(realmModel, "test-user@localhost");
                session.users().setNotBeforeForUser(realmModel, userModel, time);
            });

            response.close();
            response = executeUserInfoRequest(client, accessTokenResponse.getToken());
            assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

            runOnServer.run(session -> {
                RealmModel realmModel = session.getContext().getRealm();
                UserModel userModel = session.users().getUserByUsername(realmModel, "test-user@localhost");
                session.users().setNotBeforeForUser(realmModel, userModel, 0);
            });

            response.close();
            response = executeUserInfoRequest(client, accessTokenResponse.getToken());
            assertEquals(Status.OK.getStatusCode(), response.getStatus());
            response.close();

        }
    }

    @Test
    public void testSessionExpiredOfflineAccess() throws Exception {
        try (Client client = newClient()) {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client, true, true);

            runOnServer.run(RunHelpers.removeExpired());

            Response response = executeUserInfoRequest(client, accessTokenResponse.getToken());

            testSuccessfulUserInfoResponse(response);
            response.close();
        }
    }

    @Test
    public void testUnsuccessfulUserInfoRequest() throws Exception {
        try (Client client = newClient()) {
            try (Response response = executeUserInfoRequest(client, "bad")) {
                assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
                assertNoCacheHeaders(response);

                String wwwAuthHeader = response.getHeaderString(HttpHeaders.WWW_AUTHENTICATE);
                assertNotNull(wwwAuthHeader);
                assertThat(wwwAuthHeader, CoreMatchers.containsString("Bearer"));
                assertThat(wwwAuthHeader, CoreMatchers.containsString("error=\"" + OAuthErrorException.INVALID_TOKEN + "\""));

                EventAssertion.assertError(events.poll()).type(EventType.USER_INFO_REQUEST_ERROR)
                        .error(Errors.INVALID_TOKEN)
                        .clientId(null)
                        .userId(null)
                        .sessionId(null)
                        .details(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN);
            }

        }
    }

    @Test
    public void testUnsuccessfulUserInfoRequestWithEmptyAccessToken() {
        try (Client client = newClient()) {
            try (Response response = executeUserInfoRequest(client, "")) {
                String wwwAuthHeader = response.getHeaderString(HttpHeaders.WWW_AUTHENTICATE);
                assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
                assertNoCacheHeaders(response);
                assertEquals(wwwAuthHeader, "Bearer realm=\"" + managedRealm.getName() + "\"");
            }
        }
    }

    @Test
    public void testUnsuccessfulUserInfoRequestwithDuplicatedParams() {
        try (Client client = newClient()) {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);

            Form form = new Form();
            form.param("access_token", accessTokenResponse.getToken());
            form.param("access_token", accessTokenResponse.getToken());

            WebTarget userInfoTarget = getUserInfoWebTarget(client);
            Response response = userInfoTarget.request().post(Entity.form(form));
            response.close();
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testUnsuccessfulUserInfoRequestWithMultipleTokens() {
        try (Client client = newClient()) {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);
            String accessToken = accessTokenResponse.getToken();

            Form form = new Form();
            form.param("access_token", accessToken);

            WebTarget userInfoTarget = getUserInfoWebTarget(client);
            Response response = userInfoTarget.request()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .post(Entity.form(form));
            response.close();
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testUnsuccessfulUserInfoRequestWithoutOpenIDScope() {
        try (Client client = newClient()) {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client, false, false);
            try (Response response = executeUserInfoRequest(client, accessTokenResponse.getToken())) {
                assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
                assertNoCacheHeaders(response);

                String wwwAuthHeader = response.getHeaderString(HttpHeaders.WWW_AUTHENTICATE);
                assertNotNull(wwwAuthHeader);
                assertThat(wwwAuthHeader, CoreMatchers.containsString("Bearer"));
                assertThat(wwwAuthHeader, CoreMatchers.containsString("error=\"" + OAuthErrorException.INSUFFICIENT_SCOPE + "\""));

                EventAssertion.assertError(events.poll()).type(EventType.USER_INFO_REQUEST_ERROR)
                        .error(Errors.ACCESS_DENIED)
                        .clientId(null)
                        .userId(null)
                        .sessionId(null)
                        .details(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN);
            }
        }
    }

    @Test
    public void testUnsuccessfulUserInfoRequestWithDisabledUser() {
        RealmResource realm = managedRealm.admin();
        UserResource userResource = AdminApiUtil.findUserByUsernameId(realm, "test-user@localhost");
        UserRepresentation user = userResource.toRepresentation();

        try (Client client = newClient()) {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);
            managedRealm.updateUserWithCleanup("test-user@localhost", u -> u.enabled(false));
            Response response = executeUserInfoRequest(client, accessTokenResponse.getToken());
            response.close();

            assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

            String wwwAuthHeader = response.getHeaderString(HttpHeaders.WWW_AUTHENTICATE);
            assertNotNull(wwwAuthHeader);
            assertThat(wwwAuthHeader, CoreMatchers.containsString("Bearer"));
            assertThat(wwwAuthHeader, CoreMatchers.containsString("error=\"" + OAuthErrorException.INVALID_TOKEN + "\""));

            EventAssertion.assertError(events.poll()).type(EventType.USER_INFO_REQUEST_ERROR)
                    .error(Errors.USER_DISABLED)
                    .clientId("test-app")
                    .userId(user.getId())
                    .hasSessionId()
                    .details(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN);
        }

    }

    @Test
    public void testUserInfoRequestWithSamlClient() throws Exception {
        // obtain an access token
        String accessToken = oauth.client("saml-client", "secret").doPasswordGrantRequest( "test-user@localhost", "password").getAccessToken();

        // change client's protocol
        managedRealm.updateClientWithCleanup("saml-client", c -> c.protocol("saml"));

        try (Client client = newClient()) {
            events.clear();
            Response response = executeUserInfoRequest(client, accessToken);
            response.close();

            assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
            EventAssertion.assertError(events.poll()).type(EventType.USER_INFO_REQUEST_ERROR)
                    .error(Errors.INVALID_CLIENT)
                    .clientId(null)
                    .userId(null)
                    .sessionId(null)
                    .details(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN);
        }
    }

    @Test
    public void testRolesAreAvailable_getMethod_header() throws Exception {

        switchIncludeRolesInUserInfoEndpoint(true);

        try (Client client = newClient()) {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);
            Response response = executeUserInfoRequest(client, accessTokenResponse.getToken());

            UserInfo userInfo = testSuccessfulUserInfoResponse(response);
            testRolesInUserInfoResponse(userInfo);
        } finally {
            switchIncludeRolesInUserInfoEndpoint(false);
        }
    }

    private AccessTokenResponse executeGrantAccessTokenRequest(Client client) {
        return executeGrantAccessTokenRequest(client, false, true);
    }

    private AccessTokenResponse executeGrantAccessTokenRequest(Client client, boolean requestOfflineToken, boolean openid) {
        WebTarget grantTarget = client.target(oauth.getEndpoints().getToken());

        String header = BasicAuthHelper.createHeader("test-app", "password");
        Form form = new Form();
        String scope = null;
        form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD)
                .param("username", "test-user@localhost")
                .param("password", "password");
        if( requestOfflineToken) {
            scope = OAuth2Constants.OFFLINE_ACCESS;
        }
        if (openid) {
            scope = TokenUtil.attachOIDCScope(scope);
        }
        if (scope != null) {
            form.param(OAuth2Constants.SCOPE, scope);
        }

        Response response = grantTarget.request()
                .header(HttpHeaders.AUTHORIZATION, header)
                .post(Entity.form(form));

        assertEquals(200, response.getStatus());

        AccessTokenResponse accessTokenResponse = response.readEntity(AccessTokenResponse.class);

        response.close();

        events.clear();

        return accessTokenResponse;
    }

    private UserInfo testSuccessfulUserInfoResponse(Response response) {
        return testSuccessfulUserInfoResponse(response, "test-app");
    }

    private UserInfo testSuccessfulUserInfoResponse(Response response, String expectedClientId) {
        EventAssertion.assertSuccess(events.poll()).type(EventType.USER_INFO_REQUEST)
                .hasSessionId()
                .details(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN)
                .details(Details.USERNAME, "test-user@localhost")
                .details(Details.SIGNATURE_REQUIRED, "false")
                .clientId(expectedClientId);
        try (response) {
            assertEquals(Status.OK.getStatusCode(), response.getStatus());
            assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));
            UserInfo userInfo = response.readEntity(UserInfo.class);
            assertNotNull(userInfo);
            assertNotNull(userInfo.getSubject());
            assertEquals("test-user@localhost", userInfo.getEmail());
            assertEquals("test-user@localhost", userInfo.getPreferredUsername());
            return userInfo;
        }
    }

    private void assertNoCacheHeaders(Response response) {
        assertEquals("no-store", response.getHeaderString(HttpHeaders.CACHE_CONTROL));
        assertEquals("no-cache", response.getHeaderString("Pragma"));
    }

    private void testSuccessSignedResponse(String sigAlg, String acceptHeader) throws Exception {

        // Require signed userInfo request
        ClientResource clientResource = oauth.clientResource();
        ClientRepresentation clientRep = clientResource.toRepresentation();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUserInfoSignedResponseAlg(sigAlg);
        clientResource.update(clientRep);

        // test signed response
        try (Client client = newClient()) {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);

            Response response = executeUserInfoRequest(client, accessTokenResponse.getToken(), acceptHeader);

            EventAssertion.assertSuccess(events.poll()).type(EventType.USER_INFO_REQUEST)
                    .hasSessionId()
                    .details(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN)
                    .details(Details.USERNAME, "test-user@localhost")
                    .details(Details.SIGNATURE_REQUIRED, "true")
                    .details(Details.SIGNATURE_ALGORITHM, sigAlg);

            Assertions.assertEquals(200, response.getStatus());
            Assertions.assertEquals(response.getHeaderString(HttpHeaders.CONTENT_TYPE), MediaType.APPLICATION_JWT);
            String signedResponse = response.readEntity(String.class);
            response.close();

            JWSInput jwsInput = new JWSInput(signedResponse);

            assertEquals(sigAlg, jwsInput.getHeader().getAlgorithm().name());

            UserInfo userInfo = JsonSerialization.readValue(jwsInput.getContent(), UserInfo.class);

            Assertions.assertNotNull(userInfo);
            Assertions.assertNotNull(userInfo.getSubject());
            Assertions.assertEquals("test-user@localhost", userInfo.getEmail());
            Assertions.assertEquals("test-user@localhost", userInfo.getPreferredUsername());

            Assertions.assertTrue(userInfo.hasAudience("test-app"));
            String expectedIssuer = managedRealm.getBaseUrl();
            Assertions.assertEquals(expectedIssuer, userInfo.getIssuer());

        }

    }

    private org.keycloak.testsuite.util.oauth.AccessTokenResponse loginAndForceNewLoginPage() {
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.parseLoginResponse().getCode();

        org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);

        timeOffSet.set(1);

        oauth.loginForm().prompt(OIDCLoginProtocol.PROMPT_VALUE_LOGIN).open();

        loginPage.assertCurrent();

        return tokenResponse;
    }

    private void switchIncludeRolesInUserInfoEndpoint(boolean includeRoles) {
        ClientScopesResource clientScopesResource = managedRealm.admin().clientScopes();
        ClientScopeRepresentation rolesClientScope = clientScopesResource.findAll().stream()
                .filter(clientScope -> "roles".equals(clientScope.getName()))
                .findAny()
                .get();

        ProtocolMappersResource protocolMappersResource =
                clientScopesResource.get(rolesClientScope.getId()).getProtocolMappers();

        ProtocolMapperRepresentation realmRolesMapper = protocolMappersResource.getMappers().stream()
                .filter(mapper -> "realm roles".equals(mapper.getName()))
                .findAny()
                .get();

        if (includeRoles) {
            ProtocolMapperRepresentation original = RepresentationUtils.clone(realmRolesMapper);
            managedRealm.cleanup().add(r -> r.clientScopes().get(rolesClientScope.getId())
                    .getProtocolMappers().update(original.getId(), original));
        }
        realmRolesMapper.getConfig().put(INCLUDE_IN_USERINFO, String.valueOf(includeRoles));

        ProtocolMapperRepresentation clientRolesMapper = protocolMappersResource.getMappers().stream()
                .filter(mapper -> "client roles".equals(mapper.getName()))
                .findAny()
                .get();

        if (includeRoles) {
            ProtocolMapperRepresentation original = RepresentationUtils.clone(clientRolesMapper);
            managedRealm.cleanup().add(r -> r.clientScopes().get(rolesClientScope.getId())
                    .getProtocolMappers().update(original.getId(), original));
        }
        clientRolesMapper.getConfig().put(INCLUDE_IN_USERINFO, String.valueOf(includeRoles));

        protocolMappersResource.update(realmRolesMapper.getId(), realmRolesMapper);
        protocolMappersResource.update(clientRolesMapper.getId(), clientRolesMapper);
    }

    private void testRolesInUserInfoResponse(UserInfo userInfo) {
        Map<String, Collection<String>> realmAccess = (Map<String, Collection<String>>) userInfo.getOtherClaims().get("realm_access");
        Map<String, Map<String, Collection<String>>> resourceAccess = (Map<String, Map<String, Collection<String>>>) userInfo.getOtherClaims().get("resource_access");

        org.hamcrest.MatcherAssert.assertThat(realmAccess.get("roles"), CoreMatchers.hasItems("offline_access", "user"));
        org.hamcrest.MatcherAssert.assertThat(resourceAccess.get("test-app").get("roles"), CoreMatchers.hasItems("customer-user"));
    }

    private void testRolesAreNotInUserInfoResponse(UserInfo userInfo) {
        assertNull(userInfo.getOtherClaims().get("realm_access"));
        assertNull(userInfo.getOtherClaims().get("resource_access"));
    }

    @Test
    public void test_noContentType() throws Exception {
        try (Client client = newClient()) {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);

            WebTarget userInfoTarget = getUserInfoWebTarget(client);
            Response response = userInfoTarget.request()
                    .header(HttpHeaders.AUTHORIZATION, "bearer " + accessTokenResponse.getToken())
                    .build("POST")
                    .invoke();

            Assertions.assertEquals(200, response.getStatus());
            Assertions.assertEquals("OK", response.getStatusInfo().toString());

        }
    }

    private WebTarget getUserInfoWebTarget(Client client) {
        return client.target(oauth.getEndpoints().getUserInfo());
    }

    private Response executeUserInfoRequest(Client client, String accessToken) {
        return executeUserInfoRequest(client, accessToken, null);
    }

    private Response executeUserInfoRequest(Client client, String accessToken, String acceptHeader) {
        Invocation.Builder request = getUserInfoWebTarget(client).request()
                .header(HttpHeaders.AUTHORIZATION, "bearer " + accessToken);
        if (acceptHeader != null) {
            request.header(HttpHeaders.ACCEPT, acceptHeader);
        }
        return request.get();
    }

    public static class TestRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.roles("user")
                    // Match the legacy event queue, including events not stored by default.
                    .enabledEventTypes(Arrays.stream(EventType.values()).map(EventType::name).toArray(String[]::new))
                    .users(UserBuilder.create("test-user@localhost").email("test-user@localhost")
                            .name("Tom", "Brady").password("password").realmRoles("user", "offline_access"))
                    .clients(ClientBuilder.create("saml-client").secret("secret")
                            .serviceAccountsEnabled(true).directAccessGrantsEnabled(true));
        }
    }

    public static class TestClientConfig implements ClientConfig {
        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.clientId("test-app").secret("password").directAccessGrantsEnabled(true);
        }
    }
}
