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
package org.keycloak.testsuite.oidc;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriBuilder;

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
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
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
import org.keycloak.services.Urls;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.client.resources.TestApplicationResourceUrls;
import org.keycloak.testsuite.client.resources.TestOIDCEndpointsApplicationResource;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.KeyUtils;
import org.keycloak.testsuite.util.KeycloakModelUtils;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.RoleBuilder;
import org.keycloak.testsuite.util.TokenSignatureUtil;
import org.keycloak.testsuite.util.UserInfoClientUtil;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.util.BasicAuthHelper;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;
import org.keycloak.utils.MediaType;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO;
import static org.keycloak.testsuite.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.AbstractTestRealmKeycloakTest.TEST_REALM_NAME;
import static org.keycloak.testsuite.util.oauth.OAuthClient.AUTH_SERVER_ROOT;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author pedroigor
 */
public class UserInfoTest extends AbstractKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected LoginPage loginPage;

    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();
    }

    @Before
    public void clientConfiguration() {
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").directAccessGrant(true);
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realmRepresentation = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        RealmBuilder realm = RealmBuilder.edit(realmRepresentation).testEventListener();
        RealmRepresentation testRealm = realm.build();
        testRealms.add(testRealm);

        ClientRepresentation samlApp = KeycloakModelUtils.createClient(testRealm, "saml-client");
        samlApp.setSecret("secret");
        samlApp.setServiceAccountsEnabled(true);
        samlApp.setDirectAccessGrantsEnabled(true);
    }

    public void testSuccessGet(String acceptHeader) throws Exception {
        Client client = AdminClientUtil.createResteasyClient();

        try {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);
            Response response = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, accessTokenResponse.getToken(), acceptHeader);

            UserInfo userInfo = testSuccessfulUserInfoResponse(response);
            testRolesAreNotInUserInfoResponse(userInfo);

        } finally {
            client.close();
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
        Client client = AdminClientUtil.createResteasyClient();

        try {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);

            WebTarget userInfoTarget = UserInfoClientUtil.getUserInfoWebTarget(client);
            Response response = userInfoTarget.request()
                    .header(HttpHeaders.AUTHORIZATION, "bearer " + accessTokenResponse.getToken())
                    .post(Entity.form(new Form()));

            testSuccessfulUserInfoResponse(response);

        } finally {
            client.close();
        }
    }

    @Test
    public void testSuccess_postMethod_body() throws Exception {
        Client client = AdminClientUtil.createResteasyClient();

        try {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);

            Form form = new Form();
            form.param("access_token", accessTokenResponse.getToken());

            WebTarget userInfoTarget = UserInfoClientUtil.getUserInfoWebTarget(client);
            Response response = userInfoTarget.request()
                    .post(Entity.form(form));

            testSuccessfulUserInfoResponse(response);

        } finally {
            client.close();
        }
    }

    @Test
    public void testSuccess_postMethod_charset_body() throws Exception {
        Client client = AdminClientUtil.createResteasyClient();

        try {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);

            Form form = new Form();
            form.param("access_token", accessTokenResponse.getToken());

            WebTarget userInfoTarget = UserInfoClientUtil.getUserInfoWebTarget(client);
            Response response = userInfoTarget.request()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_TYPE.withCharset("utf-8"))
                    .post(Entity.form(form));

            testSuccessfulUserInfoResponse(response);

        } finally {
            client.close();
        }
    }


    // KEYCLOAK-8838
    @Test
    public void testSuccess_dotsInClientId() throws Exception {
        // Create client with dot in the name
        final String clientId = "my.foo.$\\client\\$";
        ClientRepresentation clientRep = org.keycloak.testsuite.util.ClientBuilder.create()
                .clientId(clientId)
                .addRedirectUri("http://foo.host")
                .secret("password")
                .directAccessGrants()
                .build();

        RealmResource realm = adminClient.realm("test");

        Response resp = realm.clients().create(clientRep);
        String clientUUID = ApiUtil.getCreatedId(resp);
        resp.close();
        getCleanup().addClientUuid(clientUUID);

        //Create role with dot in the name
        realm.clients().get(clientUUID).roles().create(RoleBuilder.create().name("my.foo.role").build());

        // Assign role to the user
        RoleRepresentation fooRole = realm.clients().get(clientUUID).roles().get("my.foo.role").toRepresentation();
        UserResource userResource = ApiUtil.findUserByUsernameId(realm, "test-user@localhost");
        userResource.roles().clientLevel(clientUUID).add(Collections.singletonList(fooRole));

        // Login to the new client
        org.keycloak.testsuite.util.oauth.AccessTokenResponse accessTokenResponse = oauth.client(clientId, "password")
                .doPasswordGrantRequest("test-user@localhost", "password");

        AccessToken accessToken = oauth.verifyToken(accessTokenResponse.getAccessToken());
        Assert.assertNames(accessToken.getResourceAccess(clientId).getRoles(), "my.foo.role");

        events.clear();

        // Send UserInfo request and ensure it is correct
        Client client = AdminClientUtil.createResteasyClient();
        try {
            Response response = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, accessTokenResponse.getAccessToken());

            testSuccessfulUserInfoResponse(response, clientId);
        } finally {
            client.close();
        }
    }

    @Test
    public void testSuccess_postMethod_header_textEntity() throws Exception {
        Client client = AdminClientUtil.createResteasyClient();

        try {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);

            WebTarget userInfoTarget = UserInfoClientUtil.getUserInfoWebTarget(client);
            Response response = userInfoTarget.request()
                    .header(HttpHeaders.AUTHORIZATION, "bearer " + accessTokenResponse.getToken())
                    .post(Entity.text(""));

            testSuccessfulUserInfoResponse(response);

        } finally {
            client.close();
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
        ClientResource clientResource = null;
        ClientRepresentation clientRep = null;
        try {
            // generate and register encryption key onto client
            TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
            oidcClientEndpointsResource.generateKeys(algAlgorithm, curve);

            clientResource = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
            clientRep = clientResource.toRepresentation();
            // set UserInfo response signature algorithm and encryption algorithms
            if(sigAlgorithm != null) {
                OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUserInfoSignedResponseAlg(sigAlgorithm);
            }
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUserInfoEncryptedResponseAlg(algAlgorithm);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUserInfoEncryptedResponseEnc(encAlgorithm);
            // use and set jwks_url
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUseJwksUrl(true);
            String jwksUrl = TestApplicationResourceUrls.clientJwksUri();
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setJwksUrl(jwksUrl);
            clientResource.update(clientRep);

            // get User Info response
            Client client = AdminClientUtil.createResteasyClient();
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);
            Response response = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, accessTokenResponse.getToken());

            Assert.assertEquals(200, response.getStatus());
            Assert.assertEquals(response.getHeaderString(HttpHeaders.CONTENT_TYPE), MediaType.APPLICATION_JWT);
            String encryptedResponse = response.readEntity(String.class);
            response.close();

            // parse JWE and JOSE Header
            String[] parts = encryptedResponse.split("\\.");
            Assert.assertEquals(parts.length, 5);

            // get decryption key
            // not publickey , use privateKey
            Map<String, String> keyPair = oidcClientEndpointsResource.getKeysAsPem();
            PrivateKey decryptionKEK = PemUtils.decodePrivateKey(keyPair.get("privateKey"));

            // a nested JWT (signed and encrypted JWT) needs to set "JWT" to its JOSE Header's "cty" field
            JWEHeader jweHeader = (JWEHeader) getHeader(parts[0]);
            Assert.assertEquals(algAlgorithm, jweHeader.getAlgorithm());
            if(encAlgorithm != null) {
                Assert.assertEquals(encAlgorithm, jweHeader.getEncryptionAlgorithm());
            } else {
                // if enc algorithm is not specified the default for this value is A128CBC-HS256
                Assert.assertEquals(JWEConstants.A128CBC_HS256, jweHeader.getEncryptionAlgorithm());
            }
            if(sigAlgorithm != null) {
                Assert.assertEquals("JWT", jweHeader.getContentType());
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
                JsonWebToken jsonWebToken = oauth.verifyToken(jwePayload, JsonWebToken.class);
                JWSInput jwsInput = new JWSInput(jwePayload);
                userInfo = JsonSerialization.readValue(jwsInput.getContent(), UserInfo.class);
            } else {
                userInfo = JsonSerialization.readValue(jwePayload, UserInfo.class);
            }
            Assert.assertNotNull(userInfo);
            Assert.assertNotNull(userInfo.getSubject());
            Assert.assertEquals("test-user@localhost", userInfo.getEmail());
            Assert.assertEquals("test-user@localhost", userInfo.getPreferredUsername());
        } catch (JWSInputException | JWEException | IOException e) {
            Assert.fail();
        } finally {
            clientResource = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
            clientRep = clientResource.toRepresentation();
            // revert User Info response signature algorithm and encryption algorithms
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUserInfoSignedResponseAlg(null);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUserInfoEncryptedResponseAlg(null);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUserInfoEncryptedResponseEnc(null);
            // revert jwks_url settings
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUseJwksUrl(false);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setJwksUrl(null);
            clientResource.update(clientRep);
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
        ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
        ClientRepresentation clientRep = clientResource.toRepresentation();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUserInfoSignedResponseAlg(Algorithm.RS256);
        clientResource.update(clientRep);

        // test signed response
        Client client = AdminClientUtil.createResteasyClient();

        try {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);

            Response response = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, accessTokenResponse.getToken());

            events.expect(EventType.USER_INFO_REQUEST)
                    .session(Matchers.notNullValue(String.class))
                    .detail(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN)
                    .detail(Details.USERNAME, "test-user@localhost")
                    .detail(Details.SIGNATURE_REQUIRED, "true")
                    .detail(Details.SIGNATURE_ALGORITHM, Algorithm.RS256)
                    .assertEvent();

            // Check signature and content
            PublicKey publicKey = PemUtils.decodePublicKey(KeyUtils.findActiveSigningKey(adminClient.realm("test")).getPublicKey());

            Assert.assertEquals(200, response.getStatus());
            Assert.assertEquals(response.getHeaderString(HttpHeaders.CONTENT_TYPE), MediaType.APPLICATION_JWT);
            String signedResponse = response.readEntity(String.class);
            response.close();

            JWSInput jwsInput = new JWSInput(signedResponse);
            Assert.assertTrue(RSAProvider.verify(jwsInput, publicKey));

            UserInfo userInfo = JsonSerialization.readValue(jwsInput.getContent(), UserInfo.class);

            Assert.assertNotNull(userInfo);
            Assert.assertNotNull(userInfo.getSubject());
            Assert.assertEquals("test-user@localhost", userInfo.getEmail());
            Assert.assertEquals("test-user@localhost", userInfo.getPreferredUsername());

            Assert.assertTrue(userInfo.hasAudience("test-app"));
            String expectedIssuer = Urls.realmIssuer(new URI(AUTH_SERVER_ROOT), "test");
            Assert.assertEquals(expectedIssuer, userInfo.getIssuer());

        } finally {
            client.close();
        }

        // Revert signed userInfo request
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUserInfoSignedResponseAlg(null);
        clientResource.update(clientRep);
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
        Client client = AdminClientUtil.createResteasyClient();

        try {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);

            String realmName = "test";
            testingClient.testing().removeUserSessions(realmName);

            Response response = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, accessTokenResponse.getToken());

            assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
            String wwwAuthHeader = response.getHeaderString(HttpHeaders.WWW_AUTHENTICATE);
            assertNotNull(wwwAuthHeader);
            assertThat(wwwAuthHeader, CoreMatchers.containsString("Bearer"));
            assertThat(wwwAuthHeader, CoreMatchers.containsString("realm=\"" + realmName + "\""));
            assertThat(wwwAuthHeader, CoreMatchers.containsString("error=\"" + OAuthErrorException.INVALID_TOKEN + "\""));

            response.close();

            events.expect(EventType.USER_INFO_REQUEST_ERROR)
                    .error(Errors.USER_SESSION_NOT_FOUND)
                    .user(Matchers.nullValue(String.class))
                    .session(accessTokenResponse.getSessionState())
                    .detail(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN)
                    .assertEvent();
            events.assertEmpty();

        } finally {
            client.close();
        }
    }

    @Test
    public void testAccessTokenExpired() {
        Client client = AdminClientUtil.createResteasyClient();

        try {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);

            setTimeOffset(600);

            Response response = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, accessTokenResponse.getToken());

            assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

            String wwwAuthHeader = response.getHeaderString(HttpHeaders.WWW_AUTHENTICATE);
            assertNotNull(wwwAuthHeader);
            assertThat(wwwAuthHeader, CoreMatchers.containsString("Bearer"));
            assertThat(wwwAuthHeader, CoreMatchers.containsString("error=\"" + OAuthErrorException.INVALID_TOKEN + "\""));

            response.close();

            events.expect(EventType.USER_INFO_REQUEST_ERROR)
                    .error(Errors.INVALID_TOKEN)
                    .user(Matchers.nullValue(String.class))
                    .session(Matchers.nullValue(String.class))
                    .detail(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN)
                    .client((String) null)
                    .assertEvent();
        } finally {
            client.close();
        }
    }

    @Test
    public void testAccessTokenAfterUserSessionLogoutAndLoginAgain() {
        org.keycloak.testsuite.util.oauth.AccessTokenResponse accessTokenResponse = loginAndForceNewLoginPage();
        String refreshToken1 = accessTokenResponse.getRefreshToken();

        oauth.doLogout(refreshToken1);
        events.clear();

        setTimeOffset(2);

        driver.navigate().refresh();
        oauth.fillLoginForm("test-user@localhost", "password");
        events.expectLogin().assertEvent();

        Assert.assertFalse(loginPage.isCurrent());

        events.clear();

        Client client = AdminClientUtil.createResteasyClient();

        try {
            Response response = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, accessTokenResponse.getAccessToken());

            assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

            String wwwAuthHeader = response.getHeaderString(HttpHeaders.WWW_AUTHENTICATE);
            assertNotNull(wwwAuthHeader);
            assertThat(wwwAuthHeader, CoreMatchers.containsString("Bearer"));
            assertThat(wwwAuthHeader, CoreMatchers.containsString("error=\"" + OAuthErrorException.INVALID_TOKEN + "\""));

            response.close();

            events.expect(EventType.USER_INFO_REQUEST_ERROR)
                    .error(Errors.USER_SESSION_NOT_FOUND)
                    .user(Matchers.nullValue(String.class))
                    .session(accessTokenResponse.getSessionState())
                    .detail(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN)
                    .client("test-app")
                    .assertEvent();
        } finally {
            client.close();
        }
    }

    // Issue 39037
    @Test
    public void testUserInfoWithOfflineAccessAndHybridFlow() throws Exception {
        try (Client client = AdminClientUtil.createResteasyClient();
             ClientAttributeUpdater oidcClient = ClientAttributeUpdater.forClient(adminClient, TEST_REALM_NAME, "test-app")
                .setImplicitFlowEnabled(true)
                .update()) {
            oauth.scope(OAuth2Constants.SCOPE_OPENID + " " + OAuth2Constants.OFFLINE_ACCESS)
                    .responseType(OIDCResponseType.CODE + " " + OIDCResponseType.TOKEN)
                    .doLogin("test-user@localhost", "password");
            AuthorizationEndpointResponse authzEndpointResponse = oauth.parseLoginResponse();

            // UserInfo request with the accessToken returned from authz endpoint
            Response response1 = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, authzEndpointResponse.getAccessToken());
            assertResponseSuccessful(response1);

            org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(authzEndpointResponse.getCode());

            // Another userInfo request with the accessToken (but after tokens are exchanged)
            Response response2 = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, authzEndpointResponse.getAccessToken());
            assertResponseSuccessful(response2);

            // UserInfo request with the token returned from token response
            Response response3 = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, tokenResponse.getAccessToken());
            assertResponseSuccessful(response3);
        }
    }

    @Test
    public void testNotBeforeTokens() {
        Client client = AdminClientUtil.createResteasyClient();

        try {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);

            int time = Time.currentTime() + 60;

            RealmResource realm = adminClient.realm("test");
            RealmRepresentation rep = realm.toRepresentation();
            rep.setNotBefore(time);
            realm.update(rep);

            Response response = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, accessTokenResponse.getToken());

            assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

            response.close();

            events.expect(EventType.USER_INFO_REQUEST_ERROR)
                    .error(Errors.INVALID_TOKEN)
                    .user(Matchers.nullValue(String.class))
                    .session(Matchers.nullValue(String.class))
                    .detail(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN)
                    .client((String) null)
                    .assertEvent();

            events.clear();
            rep.setNotBefore(0);
            realm.update(rep);

            // do the same with client's notBefore
            ClientResource clientResource = realm.clients().get(realm.clients().findByClientId("test-app").get(0).getId());
            ClientRepresentation clientRep = clientResource.toRepresentation();
            clientRep.setNotBefore(time);
            clientResource.update(clientRep);

            response = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, accessTokenResponse.getToken());

            assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

            response.close();

            events.expect(EventType.USER_INFO_REQUEST_ERROR)
                    .error(Errors.INVALID_TOKEN)
                    .user(Matchers.nullValue(String.class))
                    .session(Matchers.nullValue(String.class))
                    .detail(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN)
                    .client((String) null)
                    .assertEvent();

            clientRep.setNotBefore(0);
            clientResource.update(clientRep);
        } finally {
            client.close();
        }
    }

    @Test
    public void testSessionExpiredOfflineAccess() throws Exception {
        Client client = AdminClientUtil.createResteasyClient();

        try {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client, true, true);

            testingClient.testing().removeExpired("test");

            Response response = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, accessTokenResponse.getToken());

            testSuccessfulUserInfoResponse(response);
            response.close();
        } finally {
            client.close();
        }
    }

    @Test
    public void testUnsuccessfulUserInfoRequest() throws Exception {
        Client client = AdminClientUtil.createResteasyClient();

        try {
            Response response = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, "bad");

            response.close();

            assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

            String wwwAuthHeader = response.getHeaderString(HttpHeaders.WWW_AUTHENTICATE);
            assertNotNull(wwwAuthHeader);
            assertThat(wwwAuthHeader, CoreMatchers.containsString("Bearer"));
            assertThat(wwwAuthHeader, CoreMatchers.containsString("error=\"" + OAuthErrorException.INVALID_TOKEN + "\""));

            events.expect(EventType.USER_INFO_REQUEST_ERROR)
                    .error(Errors.INVALID_TOKEN)
                    .client((String) null)
                    .user(Matchers.nullValue(String.class))
                    .session(Matchers.nullValue(String.class))
                    .detail(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN)
                    .assertEvent();

        } finally {
            client.close();
        }
    }

    @Test
    public void testUnsuccessfulUserInfoRequestWithEmptyAccessToken() {
        Client client = AdminClientUtil.createResteasyClient();

        try {
            Response response = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, "");
            String wwwAuthHeader = response.getHeaderString(HttpHeaders.WWW_AUTHENTICATE);
            assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
            assertEquals(wwwAuthHeader, "Bearer realm=\"test\"");
            response.close();
        } finally {
            client.close();
        }
    }

    @Test
    public void testUnsuccessfulUserInfoRequestwithDuplicatedParams() {
        Client client = AdminClientUtil.createResteasyClient();

        try {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);

            Form form = new Form();
            form.param("access_token", accessTokenResponse.getToken());
            form.param("access_token", accessTokenResponse.getToken());

            WebTarget userInfoTarget = UserInfoClientUtil.getUserInfoWebTarget(client);
            Response response = userInfoTarget.request().post(Entity.form(form));
            response.close();
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        } finally {
            client.close();
        }
    }

    @Test
    public void testUnsuccessfulUserInfoRequestWithMultipleTokens() {
        Client client = AdminClientUtil.createResteasyClient();

        try {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);
            String accessToken = accessTokenResponse.getToken();

            Form form = new Form();
            form.param("access_token", accessToken);

            WebTarget userInfoTarget = UserInfoClientUtil.getUserInfoWebTarget(client);
            Response response = userInfoTarget.request()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .post(Entity.form(form));
            response.close();
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        } finally {
            client.close();
        }
    }

    @Test
    public void testUnsuccessfulUserInfoRequestWithoutOpenIDScope() {
        Client client = AdminClientUtil.createResteasyClient();

        try {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client, false, false);
            Response response = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, accessTokenResponse.getToken());
            response.close();

            assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());

            String wwwAuthHeader = response.getHeaderString(HttpHeaders.WWW_AUTHENTICATE);
            assertNotNull(wwwAuthHeader);
            assertThat(wwwAuthHeader, CoreMatchers.containsString("Bearer"));
            assertThat(wwwAuthHeader, CoreMatchers.containsString("error=\"" + OAuthErrorException.INSUFFICIENT_SCOPE + "\""));

            events.expect(EventType.USER_INFO_REQUEST_ERROR)
                    .error(Errors.ACCESS_DENIED)
                    .client((String) null)
                    .user(Matchers.nullValue(String.class))
                    .session(Matchers.nullValue(String.class))
                    .detail(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN)
                    .assertEvent();
        } finally {
            client.close();
        }
    }

    @Test
    public void testUnsuccessfulUserInfoRequestWithDisabledUser() {
        Client client = AdminClientUtil.createResteasyClient();
        RealmResource realm = adminClient.realm("test");
        UserResource userResource = ApiUtil.findUserByUsernameId(realm, "test-user@localhost");
        UserRepresentation user = userResource.toRepresentation();

        try {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);
            user.setEnabled(false);
            userResource.update(user);
            Response response = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, accessTokenResponse.getToken());
            response.close();

            assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

            String wwwAuthHeader = response.getHeaderString(HttpHeaders.WWW_AUTHENTICATE);
            assertNotNull(wwwAuthHeader);
            assertThat(wwwAuthHeader, CoreMatchers.containsString("Bearer"));
            assertThat(wwwAuthHeader, CoreMatchers.containsString("error=\"" + OAuthErrorException.INVALID_TOKEN + "\""));

            events.expect(EventType.USER_INFO_REQUEST_ERROR)
                    .error(Errors.USER_DISABLED)
                    .client("test-app")
                    .user(user.getId())
                    .session(Matchers.notNullValue(String.class))
                    .detail(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN)
                    .assertEvent();
        } finally {
            client.close();
        }

        user.setEnabled(true);
        userResource.update(user);
    }

    @Test
    public void testUserInfoRequestWithSamlClient() throws Exception {
        // obtain an access token
        String accessToken = oauth.client("saml-client", "secret").doPasswordGrantRequest( "test-user@localhost", "password").getAccessToken();

        // change client's protocol
        ClientRepresentation samlClient = adminClient.realm("test").clients().findByClientId("saml-client").get(0);
        samlClient.setProtocol("saml");
        adminClient.realm("test").clients().get(samlClient.getId()).update(samlClient);

        Client client = AdminClientUtil.createResteasyClient();
        try {
            events.clear();
            Response response = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, accessToken);
            response.close();

            assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
            events.expect(EventType.USER_INFO_REQUEST)
                    .error(Errors.INVALID_CLIENT)
                    .client((String) null)
                    .user(Matchers.nullValue(String.class))
                    .session(Matchers.nullValue(String.class))
                    .detail(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN)
                    .assertEvent();
        } finally {
            client.close();
        }
    }

    @Test
    public void testRolesAreAvailable_getMethod_header() throws Exception {

        switchIncludeRolesInUserInfoEndpoint(true);

        Client client = AdminClientUtil.createResteasyClient();

        try {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);
            Response response = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, accessTokenResponse.getToken());

            UserInfo userInfo = testSuccessfulUserInfoResponse(response);
            testRolesInUserInfoResponse(userInfo);
        } finally {
            client.close();
            switchIncludeRolesInUserInfoEndpoint(false);
        }
    }

    private AccessTokenResponse executeGrantAccessTokenRequest(Client client) {
        return executeGrantAccessTokenRequest(client, false, true);
    }

    private AccessTokenResponse executeGrantAccessTokenRequest(Client client, boolean requestOfflineToken, boolean openid) {
        UriBuilder builder = UriBuilder.fromUri(AUTH_SERVER_ROOT);
            URI grantUri = OIDCLoginProtocolService.tokenUrl(builder).build("test");
        WebTarget grantTarget = client.target(grantUri);

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
        events.expect(EventType.USER_INFO_REQUEST)
                .session(Matchers.notNullValue(String.class))
                .detail(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN)
                .detail(Details.USERNAME, "test-user@localhost")
                .detail(Details.SIGNATURE_REQUIRED, "false")
                .client(expectedClientId)
                .assertEvent();
        return UserInfoClientUtil.testSuccessfulUserInfoResponse(response, "test-user@localhost", "test-user@localhost");
    }

    private void testSuccessSignedResponse(String sigAlg, String acceptHeader) throws Exception {

        try {
            // Require signed userInfo request
            ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
            ClientRepresentation clientRep = clientResource.toRepresentation();
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUserInfoSignedResponseAlg(sigAlg);
            clientResource.update(clientRep);

            // test signed response
            Client client = AdminClientUtil.createResteasyClient();

            try {
                AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);

                Response response = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, accessTokenResponse.getToken(), acceptHeader);

                events.expect(EventType.USER_INFO_REQUEST)
                        .session(Matchers.notNullValue(String.class))
                        .detail(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN)
                        .detail(Details.USERNAME, "test-user@localhost")
                        .detail(Details.SIGNATURE_REQUIRED, "true")
                        .detail(Details.SIGNATURE_ALGORITHM, sigAlg)
                        .assertEvent();

                Assert.assertEquals(200, response.getStatus());
                Assert.assertEquals(response.getHeaderString(HttpHeaders.CONTENT_TYPE), MediaType.APPLICATION_JWT);
                String signedResponse = response.readEntity(String.class);
                response.close();

                JWSInput jwsInput = new JWSInput(signedResponse);

                assertEquals(sigAlg, jwsInput.getHeader().getAlgorithm().name());

                UserInfo userInfo = JsonSerialization.readValue(jwsInput.getContent(), UserInfo.class);

                Assert.assertNotNull(userInfo);
                Assert.assertNotNull(userInfo.getSubject());
                Assert.assertEquals("test-user@localhost", userInfo.getEmail());
                Assert.assertEquals("test-user@localhost", userInfo.getPreferredUsername());

                Assert.assertTrue(userInfo.hasAudience("test-app"));
                String expectedIssuer = Urls.realmIssuer(new URI(AUTH_SERVER_ROOT), "test");
                Assert.assertEquals(expectedIssuer, userInfo.getIssuer());

            } finally {
                client.close();
            }

            // Revert signed userInfo request
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUserInfoSignedResponseAlg(null);
            clientResource.update(clientRep);
        } finally {
            TokenSignatureUtil.changeRealmTokenSignatureProvider(adminClient, Algorithm.RS256);
        }
    }

    private org.keycloak.testsuite.util.oauth.AccessTokenResponse loginAndForceNewLoginPage() {
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.parseLoginResponse().getCode();

        org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);

        setTimeOffset(1);

        oauth.loginForm().prompt(OIDCLoginProtocol.PROMPT_VALUE_LOGIN).open();

        loginPage.assertCurrent();

        return tokenResponse;
    }

    private void switchIncludeRolesInUserInfoEndpoint(boolean includeRoles) {
        ClientScopesResource clientScopesResource = adminClient.realm("test").clientScopes();
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

        realmRolesMapper.getConfig().put(INCLUDE_IN_USERINFO, String.valueOf(includeRoles));

        ProtocolMapperRepresentation clientRolesMapper = protocolMappersResource.getMappers().stream()
                .filter(mapper -> "client roles".equals(mapper.getName()))
                .findAny()
                .get();

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
        Client client = AdminClientUtil.createResteasyClient();

        try {
            AccessTokenResponse accessTokenResponse = executeGrantAccessTokenRequest(client);

            WebTarget userInfoTarget = UserInfoClientUtil.getUserInfoWebTarget(client);
            Response response = userInfoTarget.request()
                    .header(HttpHeaders.AUTHORIZATION, "bearer " + accessTokenResponse.getToken())
                    .build("POST")
                    .invoke();

            Assert.assertEquals(200, response.getStatus());
            Assert.assertEquals("OK", response.getStatusInfo().toString());

        } finally {
            client.close();
        }
    }
}
