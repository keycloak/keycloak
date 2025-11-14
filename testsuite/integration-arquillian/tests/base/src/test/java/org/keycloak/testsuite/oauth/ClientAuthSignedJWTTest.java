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

package org.keycloak.testsuite.oauth;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.LinkedList;
import java.util.List;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientSecretAuthenticator;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.common.util.KeystoreUtil.KeystoreFormat;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.KeyUtils;
import org.keycloak.testsuite.util.KeystoreUtils;
import org.keycloak.testsuite.util.SignatureSignerUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class ClientAuthSignedJWTTest extends AbstractClientAuthSignedJWTTest {

    // TEST SUCCESS

    @Test
    public void testServiceAccountAndLogoutSuccess() throws Exception {
        String client1Jwt = getClient1SignedJWT();
        JsonWebToken client1JsonWebToken = new JWSInput(client1Jwt).readJsonContent(JsonWebToken.class);
        AccessTokenResponse response = doClientCredentialsGrantRequest(client1Jwt);

        assertEquals(200, response.getStatusCode());
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        RefreshToken refreshToken = oauth.parseRefreshToken(response.getRefreshToken());

        events.expectClientLogin()
                .client("client1")
                .user(client1SAUserId)
                .session(accessToken.getSessionState())
                .detail(Details.TOKEN_ID, accessToken.getId())
                .detail(Details.REFRESH_TOKEN_ID, refreshToken.getId())
                .detail(Details.USERNAME, ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + "client1")
                .detail(Details.CLIENT_AUTH_METHOD, JWTClientAuthenticator.PROVIDER_ID)
                .detail(Details.CLIENT_ASSERTION_ID, client1JsonWebToken.getId())
                .detail(Details.CLIENT_ASSERTION_ISSUER, "client1")
                .detail(Details.CLIENT_ASSERTION_SUB, "client1")
                .assertEvent();

        assertEquals(accessToken.getSessionState(), refreshToken.getSessionState());

        client1Jwt = getClient1SignedJWT();
        AccessTokenResponse refreshedResponse = doRefreshTokenRequest(response.getRefreshToken(), client1Jwt);
        AccessToken refreshedAccessToken = oauth.verifyToken(refreshedResponse.getAccessToken());
        RefreshToken refreshedRefreshToken = oauth.parseRefreshToken(refreshedResponse.getRefreshToken());

        assertEquals(accessToken.getSessionState(), refreshedAccessToken.getSessionState());
        assertEquals(accessToken.getSessionState(), refreshedRefreshToken.getSessionState());

        events.expectRefresh(refreshToken.getId(), refreshToken.getSessionState())
                .user(client1SAUserId)
                .client("client1")
                .detail(Details.CLIENT_AUTH_METHOD, JWTClientAuthenticator.PROVIDER_ID)
                .assertEvent();

        // Logout and assert refresh will fail
        HttpResponse logoutResponse = doLogout(response.getRefreshToken(), getClient1SignedJWT());
        assertEquals(204, logoutResponse.getStatusLine().getStatusCode());
        events.expectLogout(accessToken.getSessionState())
                .client("client1")
                .user(client1SAUserId)
                .removeDetail(Details.REDIRECT_URI)
                .detail(Details.CLIENT_AUTH_METHOD, JWTClientAuthenticator.PROVIDER_ID)
                .assertEvent();

        response = doRefreshTokenRequest(response.getRefreshToken(), getClient1SignedJWT());
        assertEquals(400, response.getStatusCode());
        assertEquals("invalid_grant", response.getError());

        events.expectRefresh(refreshToken.getId(), refreshToken.getSessionState())
                .client("client1")
                .user((String) null)
                .removeDetail(Details.TOKEN_ID)
                .removeDetail(Details.UPDATED_REFRESH_TOKEN_ID)
                .detail(Details.CLIENT_AUTH_METHOD, JWTClientAuthenticator.PROVIDER_ID)
                .error(Errors.INVALID_TOKEN).assertEvent();

    }

    @Test
    public void testCodeToTokenRequestSuccess() throws Exception {
        testCodeToTokenRequestSuccess(Algorithm.RS256);
    }

    @Test
    public void testCodeToTokenRequestSuccess512() throws Exception {
        testCodeToTokenRequestSuccess(Algorithm.RS512);
    }

    @Test
    public void testCodeToTokenRequestSuccessPS384() throws Exception {
        testCodeToTokenRequestSuccessForceAlgInClient(Algorithm.PS384);
    }

    @Test
    public void testCodeToTokenRequestSuccessPS512() throws Exception {
        testCodeToTokenRequestSuccessForceAlgInClient(Algorithm.PS512);
    }

    @Test
    public void testCodeToTokenRequestSuccessES256usingJwksUri() throws Exception {
        testCodeToTokenRequestSuccess(Algorithm.ES256, true);
    }

    @Test
    public void testCodeToTokenRequestSuccessES256usingJwks() throws Exception {
        testCodeToTokenRequestSuccess(Algorithm.ES256, false);
    }

    @Test
    public void testCodeToTokenRequestSuccessRS256usingJwksUri() throws Exception {
        testCodeToTokenRequestSuccess(Algorithm.RS256, true);
    }

    @Test
    public void testCodeToTokenRequestSuccessRS256usingJwks() throws Exception {
        testCodeToTokenRequestSuccess(Algorithm.RS256, false);
    }

    @Test
    public void testCodeToTokenRequestSuccessPS256usingJwksUri() throws Exception {
        testCodeToTokenRequestSuccess(Algorithm.PS256, true);
    }

    @Test
    public void testCodeToTokenRequestSuccessPS256usingJwks() throws Exception {
        testCodeToTokenRequestSuccess(Algorithm.PS256, false);
    }

    @Test
    public void testECDSASignature() throws Exception {
        testECDSASignatureLength(getClientSignedToken(Algorithm.ES256), Algorithm.ES256);
        testECDSASignatureLength(getClientSignedToken(Algorithm.ES384), Algorithm.ES384);
        testECDSASignatureLength(getClientSignedToken(Algorithm.ES512), Algorithm.ES512);
    }

    @Test
    public void testCodeToTokenRequestSuccessES256Enforced() throws Exception {
        ClientResource clientResource = null;
        ClientRepresentation clientRep = null;
        try {
            clientResource = ApiUtil.findClientByClientId(adminClient.realm("test"), "client2");
            clientRep = clientResource.toRepresentation();
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setTokenEndpointAuthSigningAlg(Algorithm.ES256);
            clientResource.update(clientRep);

            testCodeToTokenRequestSuccess(Algorithm.ES256, true);
        } catch (Exception e) {
            Assert.fail();
        } finally {
            clientResource = ApiUtil.findClientByClientId(adminClient.realm("test"), "client2");
            clientRep = clientResource.toRepresentation();
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setTokenEndpointAuthSigningAlg(null);
            clientResource.update(clientRep);
        }
    }

    @Test
    public void testDirectGrantRequestSuccess() throws Exception {
        oauth.clientId("client2");
        AccessTokenResponse response = doGrantAccessTokenRequest("test-user@localhost", "password", getClient2SignedJWT());

        assertEquals(200, response.getStatusCode());
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        RefreshToken refreshToken = oauth.parseRefreshToken(response.getRefreshToken());

        events.expectLogin()
                .client("client2")
                .session(accessToken.getSessionState())
                .detail(Details.GRANT_TYPE, OAuth2Constants.PASSWORD)
                .detail(Details.TOKEN_ID, accessToken.getId())
                .detail(Details.REFRESH_TOKEN_ID, refreshToken.getId())
                .detail(Details.USERNAME, "test-user@localhost")
                .detail(Details.CLIENT_AUTH_METHOD, JWTClientAuthenticator.PROVIDER_ID)
                .removeDetail(Details.CODE_ID)
                .removeDetail(Details.REDIRECT_URI)
                .removeDetail(Details.CONSENT)
                .assertEvent();
    }

    @Test
    public void testSuccessWhenNoAlgSetInJWK() throws Exception {
        ClientRepresentation clientRepresentation = app2;
        ClientResource clientResource = getClient(testRealm.getRealm(), clientRepresentation.getId());
        clientRepresentation = clientResource.toRepresentation();
        try {
            // setup Jwks
            String signingAlgorithm = Algorithm.PS256;
            KeyPair keyPair = setupJwksUrl(signingAlgorithm, false, false, null, clientRepresentation, clientResource);
            PublicKey publicKey = keyPair.getPublic();
            PrivateKey privateKey = keyPair.getPrivate();

            // test
            oauth.clientId("client2");
            AccessTokenResponse response = doGrantAccessTokenRequest("test-user@localhost", "password", createSignedRequestToken("client2", getRealmInfoUrl(), privateKey, publicKey, signingAlgorithm));

            assertEquals(200, response.getStatusCode());
        } finally {
            // Revert jwks_url settings
            revertJwksUriSettings(clientRepresentation, clientResource);
        }
    }

    @Test
    public void testSuccessDefaultAlgWhenNoAlgSetInJWK() throws Exception {
        ClientRepresentation clientRepresentation = app2;
        ClientResource clientResource = getClient(testRealm.getRealm(), clientRepresentation.getId());
        clientRepresentation = clientResource.toRepresentation();
        try {
            // send a JWS using the default algorithm
            String signingAlgorithm = Algorithm.RS256;
            KeyPair keyPair = setupJwksUrl(signingAlgorithm, false, false, null, clientRepresentation, clientResource);
            PublicKey publicKey = keyPair.getPublic();
            PrivateKey privateKey = keyPair.getPrivate();
            oauth.clientId("client2");
            AccessTokenResponse response = doGrantAccessTokenRequest("test-user@localhost", "password", createSignedRequestToken("client2", getRealmInfoUrl(), privateKey, publicKey, signingAlgorithm));
            assertEquals(200, response.getStatusCode());

            // sending a JWS using another RSA based alg (PS256) should work as alg is not specified
            publicKey = keyPair.getPublic();
            privateKey = keyPair.getPrivate();
            oauth.clientId("client2");
            response = doGrantAccessTokenRequest("test-user@localhost", "password", createSignedRequestToken("client2", getRealmInfoUrl(), privateKey, publicKey, Algorithm.PS256));
            assertEquals(200, response.getStatusCode());

            // sending an invalid EC (ES256) one should not work
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRepresentation).setTokenEndpointAuthSigningAlg(Algorithm.ES256);
            clientResource.update(clientRepresentation);
            response = doGrantAccessTokenRequest("test-user@localhost", "password", createSignedRequestToken("client2", getRealmInfoUrl(), privateKey, publicKey, Algorithm.PS256));
            assertEquals(400, response.getStatusCode());
            assertEquals("Invalid signature algorithm", response.getErrorDescription());
        } finally {
            // Revert jwks_url settings
            revertJwksUriSettings(clientRepresentation, clientResource);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRepresentation).setTokenEndpointAuthSigningAlg(null);
            clientResource.update(clientRepresentation);
        }
    }

    // GH issue 14794
    @Test
    public void testSuccessWhenMultipleKeysWithSameKid() throws Exception {
        ClientRepresentation clientRepresentation = app2;
        ClientResource clientResource = getClient(testRealm.getRealm(), clientRepresentation.getId());
        clientRepresentation = clientResource.toRepresentation();
        String origAccessTokenSignedResponseAlg = clientRepresentation.getAttributes().get(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG);
        try {
            clientRepresentation.getAttributes().put(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG, Algorithm.RS512);
            // setup Jwks
            String signingAlgorithm = Algorithm.RS256;
            KeyPair keyPair = setupJwksUrl(signingAlgorithm, true, true, "my-kid", clientRepresentation, clientResource);

            signingAlgorithm = Algorithm.RS512;
            keyPair = setupJwksUrl(signingAlgorithm, true, true, "my-kid", clientRepresentation, clientResource);
            PublicKey publicKey = keyPair.getPublic();
            PrivateKey privateKey = keyPair.getPrivate();

            // test
            oauth.clientId("client2");
            JsonWebToken clientAuthJwt = createRequestToken("client2", getRealmInfoUrl());
            AccessTokenResponse response = doGrantAccessTokenRequest("test-user@localhost", "password",
                    createSignledRequestToken(privateKey, publicKey, signingAlgorithm, "my-kid", clientAuthJwt));

            assertEquals(200, response.getStatusCode());
        } finally {
            // Revert jwks_url settings and signing algorithm
            clientRepresentation.getAttributes().put(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG, origAccessTokenSignedResponseAlg);
            revertJwksUriSettings(clientRepresentation, clientResource);
        }
    }

    @Test
    public void testDirectGrantRequestSuccessES256() throws Exception {
        testDirectGrantRequestSuccess(Algorithm.ES256);
    }

    @Test
    public void testDirectGrantRequestSuccessRS256() throws Exception {
        testDirectGrantRequestSuccess(Algorithm.RS256);
    }

    @Test
    public void testDirectGrantRequestSuccessPS256() throws Exception {
        testDirectGrantRequestSuccess(Algorithm.PS256);
    }

    @Test
    public void testClientWithGeneratedKeysJKS() throws Exception {
        KeystoreUtils.assumeKeystoreTypeSupported(KeystoreFormat.JKS);
        testClientWithGeneratedKeys("JKS", null, null);
    }

    @Test
    public void testClientWithGeneratedKeysPKCS12() throws Exception {
        KeystoreUtils.assumeKeystoreTypeSupported(KeystoreFormat.PKCS12);
        testClientWithGeneratedKeys("PKCS12", 2048, null);
    }

    @Test
    public void testClientWithGeneratedKeysBCFKS() throws Exception {
        KeystoreUtils.assumeKeystoreTypeSupported(KeystoreFormat.BCFKS);
        testClientWithGeneratedKeys(KeystoreFormat.BCFKS.toString(), 3072, 5);
    }

    @Test
    public void testUploadKeystoreJKS() throws Exception {
        KeystoreUtils.assumeKeystoreTypeSupported(KeystoreFormat.JKS);
        testUploadKeystore("JKS", generatedKeystoreClient1.getKeystoreFile().getAbsolutePath(), "clientkey", "storepass");
        testCodeToTokenRequestSuccess("client3", keyPairClient1, Algorithm.RS256, null);
    }

    @Test
    public void testUploadKeystorePKCS12() throws Exception {
        KeystoreUtils.assumeKeystoreTypeSupported(KeystoreFormat.PKCS12);
        KeyPair keyPair = org.keycloak.common.util.KeyUtils.generateRsaKeyPair(2048);
        KeystoreUtils.KeystoreInfo ksInfo = KeystoreUtils.generateKeystore(folder, KeystoreFormat.PKCS12, "clientkey", "pwd2", "keypass", keyPair);
        try {
            testUploadKeystore(KeystoreFormat.PKCS12.toString(), ksInfo.getKeystoreFile().getAbsolutePath(), "clientkey", "pwd2");
            testCodeToTokenRequestSuccess("client3", keyPair, Algorithm.RS256, null);
        } finally {
            ksInfo.getKeystoreFile().delete();
        }
    }

    @Test
    public void testUploadKeystoreBCFKS() throws Exception {
        KeystoreUtils.assumeKeystoreTypeSupported(KeystoreFormat.BCFKS);
        KeyPair keyPair = org.keycloak.common.util.KeyUtils.generateRsaKeyPair(2048);
        KeystoreUtils.KeystoreInfo ksInfo = KeystoreUtils.generateKeystore(folder, KeystoreFormat.BCFKS, "clientkey", "pwd2", "keypass", keyPair);
        try {
            testUploadKeystore(KeystoreFormat.BCFKS.toString(), ksInfo.getKeystoreFile().getAbsolutePath(), "clientkey", "pwd2");
            testCodeToTokenRequestSuccess("client3", keyPair, Algorithm.RS256, null);
        } finally {
            ksInfo.getKeystoreFile().delete();
        }
    }

    @Test
    public void testUploadCertificatePemRsa() throws Exception {
        testUploadCertificatePEM(org.keycloak.common.util.KeyUtils.generateRsaKeyPair(2048), Algorithm.RS256, null);
    }

    @Test
    public void testUploadCertificatePemEcdsa() throws Exception {
        testUploadCertificatePEM(KeyUtils.generateECKey(Algorithm.ES256), Algorithm.ES256, null);
    }

    @Test
    public void testUploadPublicKeyPemRsa() throws Exception {
        testUploadPublicKeyPem(org.keycloak.common.util.KeyUtils.generateRsaKeyPair(2048), Algorithm.RS256, null);
    }

    @Test
    public void testUploadPublicKeyPemEcdsa() throws Exception {
        testUploadPublicKeyPem(KeyUtils.generateECKey(Algorithm.ES256), Algorithm.ES256, null);
    }

    @Test
    public void testUploadJWKS() throws Exception {
        testUploadKeystore(org.keycloak.services.resources.admin.ClientAttributeCertificateResource.JSON_WEB_KEY_SET, "clientreg-test/jwks.json", "undefined", "undefined");
    }

    // TEST ERRORS

    @Test
    public void testMissingClientAssertionType() throws Exception {
        List<NameValuePair> parameters = new LinkedList<NameValuePair>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CLIENT_CREDENTIALS));

        CloseableHttpResponse resp = sendRequest(oauth.getEndpoints().getToken(), parameters);
        AccessTokenResponse response = new AccessTokenResponse(resp);

        assertError(response, 401, null, "invalid_client", Errors.CLIENT_NOT_FOUND);
    }

    @Test
    public void testInvalidClientAssertionType() throws Exception {
        List<NameValuePair> parameters = new LinkedList<NameValuePair>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CLIENT_CREDENTIALS));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, "invalid"));

        CloseableHttpResponse resp = sendRequest(oauth.getEndpoints().getToken(), parameters);
        AccessTokenResponse response = new AccessTokenResponse(resp);

        assertError(response,401, null, "invalid_client", Errors.CLIENT_NOT_FOUND);

    }

    @Test
    public void testWithClientAndMissingClientAssertionType() throws Exception {
        List<NameValuePair> parameters = new LinkedList<NameValuePair>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, "client1"));
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CLIENT_CREDENTIALS));

        CloseableHttpResponse resp = sendRequest(oauth.getEndpoints().getToken(), parameters);
        AccessTokenResponse response = new AccessTokenResponse(resp);

        assertError(response, 400, "client1", "invalid_client", Errors.INVALID_CLIENT_CREDENTIALS);
    }

    @Test
    public void testWithClientAndInvalidClientAssertionType() throws Exception {
        List<NameValuePair> parameters = new LinkedList<NameValuePair>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, "client1"));
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CLIENT_CREDENTIALS));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, "invalid"));

        CloseableHttpResponse resp = sendRequest(oauth.getEndpoints().getToken(), parameters);
        AccessTokenResponse response = new AccessTokenResponse(resp);

        assertError(response,400, "client1", "invalid_client", Errors.INVALID_CLIENT_CREDENTIALS);

    }

    @Test
    public void testMissingClientAssertion() throws Exception {
        List<NameValuePair> parameters = new LinkedList<NameValuePair>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CLIENT_CREDENTIALS));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));

        CloseableHttpResponse resp = sendRequest(oauth.getEndpoints().getToken(), parameters);
        AccessTokenResponse response = new AccessTokenResponse(resp);

        assertError(response, 401,null, "invalid_client", Errors.CLIENT_NOT_FOUND);
    }

    @Test
    public void testAssertionMissingIssuer() throws Exception {
        String invalidJwt = getClientSignedJWT(keyPairClient1, null);

        List<NameValuePair> parameters = new LinkedList<NameValuePair>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CLIENT_CREDENTIALS));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, invalidJwt));

        CloseableHttpResponse resp = sendRequest(oauth.getEndpoints().getToken(), parameters);
        AccessTokenResponse response = new AccessTokenResponse(resp);

        assertError(response,401, null, "invalid_client", Errors.CLIENT_NOT_FOUND);
    }

    @Test
    public void testAssertionUnknownClient() throws Exception {
        String invalidJwt = getClientSignedJWT(keyPairClient1, "unknown-client");

        List<NameValuePair> parameters = new LinkedList<NameValuePair>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CLIENT_CREDENTIALS));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, invalidJwt));

        CloseableHttpResponse resp = sendRequest(oauth.getEndpoints().getToken(), parameters);
        AccessTokenResponse response = new AccessTokenResponse(resp);

        assertError(response,401, null, "invalid_client", Errors.CLIENT_NOT_FOUND);
    }

    @Test
    public void testAssertionNonMatchingClientIdParameter() throws Exception {
        String invalidJwt = getClient1SignedJWT();

        // client_id parameter does not match the client from JWT (See "client_id" at https://www.rfc-editor.org/rfc/rfc7521.html#section-4.2 )
        List<NameValuePair> parameters = new LinkedList<NameValuePair>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CLIENT_CREDENTIALS));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, invalidJwt));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, "client2"));

        CloseableHttpResponse resp = sendRequest(oauth.getEndpoints().getToken(), parameters);
        AccessTokenResponse response = new AccessTokenResponse(resp);

        assertError(response,"client2", "invalid_client", Errors.INVALID_CLIENT_CREDENTIALS);

        // Matching client_id should work fine
        parameters.remove(3);
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, "client1"));
        resp = sendRequest(oauth.getEndpoints().getToken(), parameters);
        response = new AccessTokenResponse(resp);

        assertEquals(200, response.getStatusCode());
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertEquals(accessToken.getIssuedFor(), "client1");
    }

    @Test
    public void testAssertionDisabledClient() throws Exception {

        ClientManager.realm(adminClient.realm("test")).clientId("client1").enabled(false);

        String invalidJwt = getClient1SignedJWT();

        List<NameValuePair> parameters = new LinkedList<NameValuePair>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CLIENT_CREDENTIALS));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, invalidJwt));

        CloseableHttpResponse resp = sendRequest(oauth.getEndpoints().getToken(), parameters);
        AccessTokenResponse response = new AccessTokenResponse(resp);

        assertError(response,401, "client1", "invalid_client", Errors.CLIENT_DISABLED);

        ClientManager.realm(adminClient.realm("test")).clientId("client1").enabled(true);
    }

    @Test
    public void testAssertionUnconfiguredClientCertificate() throws Exception {
        class CertificateHolder {
            String certificate;
        }
        final CertificateHolder backupClient1Cert = new CertificateHolder();

        backupClient1Cert.certificate = ApiUtil.findClientByClientId(adminClient.realm("test"), "client1")
                .toRepresentation().getAttributes().get(JWTClientAuthenticator.CERTIFICATE_ATTR);

        ClientManager.realm(adminClient.realm("test")).clientId("client1")
                .updateAttribute(JWTClientAuthenticator.CERTIFICATE_ATTR, null);


        String invalidJwt = getClient1SignedJWT();

        List<NameValuePair> parameters = new LinkedList<NameValuePair>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CLIENT_CREDENTIALS));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, invalidJwt));

        CloseableHttpResponse resp = sendRequest(oauth.getEndpoints().getToken(), parameters);
        AccessTokenResponse response = new AccessTokenResponse(resp);

        assertError(response,400, "client1", OAuthErrorException.INVALID_CLIENT, "client_credentials_setup_required");

        ClientManager.realm(adminClient.realm("test")).clientId("client1").updateAttribute(JWTClientAuthenticator.CERTIFICATE_ATTR, backupClient1Cert.certificate);
    }

    @Test
    public void testAssertionInvalidSignature() throws Exception {
        // JWT for client1, but signed by privateKey of client2
        String invalidJwt = getClientSignedJWT(getClient2KeyPair(), "client1");

        List<NameValuePair> parameters = new LinkedList<NameValuePair>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CLIENT_CREDENTIALS));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, invalidJwt));

        CloseableHttpResponse resp = sendRequest(oauth.getEndpoints().getToken(), parameters);
        AccessTokenResponse response = new AccessTokenResponse(resp);

        assertError(response, "client1", OAuthErrorException.INVALID_CLIENT, AuthenticationFlowError.CLIENT_CREDENTIALS_SETUP_REQUIRED.toString().toLowerCase());
    }


    @Test
    public void testAssertionExpired() throws Exception {
        String invalidJwt = getClient1SignedJWT();

        setTimeOffset(1000);

        List<NameValuePair> parameters = new LinkedList<NameValuePair>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CLIENT_CREDENTIALS));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, invalidJwt));

        CloseableHttpResponse resp = sendRequest(oauth.getEndpoints().getToken(), parameters);
        AccessTokenResponse response = new AccessTokenResponse(resp);

        setTimeOffset(0);

        assertError(response, "client1", OAuthErrorException.INVALID_CLIENT, Errors.INVALID_CLIENT_CREDENTIALS);
    }

    @Test
    public void testParEndpointAsAudience() throws Exception {
        testEndpointAsAudience(oauth.getEndpoints().getPushedAuthorizationRequest());
    }

    @Test
    public void testBackchannelAuthenticationEndpointAsAudience() throws Exception {
        testEndpointAsAudience(oauth.getEndpoints().getBackchannelAuthentication());
    }

    @Test
    public void testTokenIntrospectionEndpointAsAudience() throws Exception {
        testEndpointAsAudience(oauth.getEndpoints().getIntrospection());
    }
    @Test
    public void testInvalidAudience() throws Exception {
        ClientRepresentation clientRepresentation = app2;
        ClientResource clientResource = getClient(testRealm.getRealm(), clientRepresentation.getId());
        clientRepresentation = clientResource.toRepresentation();

        try {
            List<NameValuePair> parameters = createTokenWithSpecifiedAudience(clientResource, clientRepresentation, "https://as.other.org");

            try (CloseableHttpResponse resp = sendRequest(oauth.getEndpoints().getToken(), parameters)) {
                AccessTokenResponse response = new AccessTokenResponse(resp);
                assertNull(response.getAccessToken());
                assertEquals(OAuthErrorException.INVALID_CLIENT, response.getError());
            }
        } finally {
            revertJwksUriSettings(clientRepresentation, clientResource);
        }
    }

    @Test
    public void testMultipleAudiencesRejected() throws Exception {
        ClientRepresentation clientRepresentation = app2;
        ClientResource clientResource = getClient(testRealm.getRealm(), clientRepresentation.getId());
        clientRepresentation = clientResource.toRepresentation();

        try {
            List<NameValuePair> parameters = createTokenWithSpecifiedAudience(clientResource, clientRepresentation, getRealmInfoUrl(), oauth.getEndpoints().getToken());

            try (CloseableHttpResponse resp = sendRequest(oauth.getEndpoints().getToken(), parameters)) {
                AccessTokenResponse response = new AccessTokenResponse(resp);
                assertNull(response.getAccessToken());
                assertEquals(OAuthErrorException.INVALID_CLIENT, response.getError());
            }
        } finally {
            revertJwksUriSettings(clientRepresentation, clientResource);
        }

    }

    @Test
    public void testMultipleAudiencesAllowed() throws Exception {
        // TODO: The test might be removed once we remove the option of allow-multiple-audiences-for-jwt-client-authentication
        ClientRepresentation clientRepresentation = app2;
        ClientResource clientResource = getClient(testRealm.getRealm(), clientRepresentation.getId());
        clientRepresentation = clientResource.toRepresentation();

        allowMultipleAudiencesForClientJWTOnServer(true);

        try {
            List<NameValuePair> parameters = createTokenWithSpecifiedAudience(clientResource, clientRepresentation, getRealmInfoUrl(), "https://as.other.org");

            try (CloseableHttpResponse resp = sendRequest(oauth.getEndpoints().getToken(), parameters)) {
                AccessTokenResponse response = new AccessTokenResponse(resp);
                assertNotNull(response.getAccessToken());
                assertNull(response.getError());
            }
        } finally {
            revertJwksUriSettings(clientRepresentation, clientResource);
            allowMultipleAudiencesForClientJWTOnServer(false);
        }

    }

    @Test
    public void testJWTAuthForClientCertWithOnlyAlgProvided() throws Exception {
        ClientRepresentation clientRepresentation = app2;
        ClientResource clientResource = getClient(testRealm.getRealm(), clientRepresentation.getId());
        clientRepresentation = clientResource.toRepresentation();

        try {
            KeyPair keyPair = setupJwksUrl(Algorithm.ES512, clientRepresentation, clientResource);
            PrivateKey privateKey = keyPair.getPrivate();
            JsonWebToken assertion = createRequestToken(app2.getClientId(), getRealmInfoUrl());

            SignatureSignerContext signer = SignatureSignerUtil.createSigner(privateKey, null,  Algorithm.ES512);
            String jws = new JWSBuilder().jsonContent(assertion).sign(signer);

            List<NameValuePair> parameters = new LinkedList<>();
            parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CLIENT_CREDENTIALS));
            parameters
                    .add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
            parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, jws));

            try (CloseableHttpResponse resp = sendRequest(oauth.getEndpoints().getToken(), parameters)) {
                AccessTokenResponse response = new AccessTokenResponse(resp);
                assertNotNull(response.getAccessToken());
            }
        } finally {
            revertJwksUriSettings(clientRepresentation, clientResource);
        }
    }

    @Test
    public void testAssertionInvalidNotBefore() throws Exception {
        String invalidJwt = getClient1SignedJWT();

        setTimeOffset(-1000);

        List<NameValuePair> parameters = new LinkedList<NameValuePair>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CLIENT_CREDENTIALS));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, invalidJwt));

        CloseableHttpResponse resp = sendRequest(oauth.getEndpoints().getToken(), parameters);
        AccessTokenResponse response = new AccessTokenResponse(resp);

        setTimeOffset(0);

        assertError(response, "client1", OAuthErrorException.INVALID_CLIENT, Errors.INVALID_CLIENT_CREDENTIALS);


    }

    @Test
    public void testAssertionReuse() throws Exception {
        String clientJwt = getClient1SignedJWT();

        AccessTokenResponse response = doClientCredentialsGrantRequest(clientJwt);

        assertEquals(200, response.getStatusCode());
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        Assert.assertNotNull(accessToken);
        Assert.assertNull(response.getError());

        // 2nd attempt to reuse same JWT should fail
        response = doClientCredentialsGrantRequest(clientJwt);

        assertEquals(400, response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_CLIENT, response.getError());
    }

    @Test
    public void testAuthenticationFailsWhenClientSecretJWTAuthenticatorSet() throws Exception {
        // Set client authenticator to JWT signed by client secret.
        ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm("test"), "client1");
        ClientRepresentation clientRep = clientResource.toRepresentation();
        clientRep.setClientAuthenticatorType(JWTClientSecretAuthenticator.PROVIDER_ID);
        clientResource.update(clientRep);

        // It should not be possible to use private_key_jwt for the authentication
        try {
            String clientJwt = getClient1SignedJWT();

            AccessTokenResponse response = doClientCredentialsGrantRequest(clientJwt);

            assertEquals(400, response.getStatusCode());
            assertEquals(OAuthErrorException.UNAUTHORIZED_CLIENT, response.getError());
        } finally {
            clientRep.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID);
            clientResource.update(clientRep);
        }
    }

    @Test
    public void testMissingIdClaim() throws Exception {
        AccessTokenResponse response = testMissingClaim("id");
        assertError(response, app1.getClientId(), OAuthErrorException.INVALID_CLIENT, Errors.INVALID_CLIENT_CREDENTIALS);
    }

    @Test
    public void testMissingIssuerClaim() throws Exception {
        AccessTokenResponse response = testMissingClaim("issuer");
        assertError(response,401, null, "invalid_client", Errors.CLIENT_NOT_FOUND);
    }

    @Test
    public void testMissingSubjectClaim() throws Exception {
        AccessTokenResponse response = testMissingClaim("subject");
        assertError(response,401, null, "invalid_client", Errors.CLIENT_NOT_FOUND);
    }

    @Test
    public void testMissingAudienceClaim() throws Exception {
        AccessTokenResponse response = testMissingClaim("audience");
        assertError(response,400, app1.getClientId(), OAuthErrorException.INVALID_CLIENT, Errors.INVALID_CLIENT_CREDENTIALS);
    }

    @Test
    public void testMissingIssuedAtClaim() throws Exception {
        AccessTokenResponse response = testMissingClaim("issuedAt");
        assertSuccess(response, app1.getClientId(), serviceAccountUser.getId(), serviceAccountUser.getUsername());
    }

    @Test
    // KEYCLOAK-2986
    public void testMissingExpirationClaim() throws Exception {
        // Missing only exp; the lifespan should be calculated from issuedAt
        AccessTokenResponse response = testMissingClaim("expiration");
        assertError(response, app1.getClientId(), OAuthErrorException.INVALID_CLIENT, Errors.INVALID_CLIENT_CREDENTIALS);
    }

    @Test
    public void testMissingNotBeforeClaim() throws Exception {
        AccessTokenResponse response = testMissingClaim("notBefore");
        assertSuccess(response, app1.getClientId(), serviceAccountUser.getId(), serviceAccountUser.getUsername());
    }

    @Test
    public void testCodeToTokenRequestFailureRS256() throws Exception {
        testCodeToTokenRequestFailure(Algorithm.RS256,
                OAuthErrorException.INVALID_CLIENT,
                AuthenticationFlowError.CLIENT_CREDENTIALS_SETUP_REQUIRED.toString().toLowerCase());
    }

    @Test
    public void testCodeToTokenRequestFailureES256Enforced() throws Exception {
        ClientResource clientResource = null;
        ClientRepresentation clientRep = null;
        try {
            clientResource = ApiUtil.findClientByClientId(adminClient.realm("test"), "client2");
            clientRep = clientResource.toRepresentation();
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setTokenEndpointAuthSigningAlg(Algorithm.ES256);
            clientResource.update(clientRep);

            testCodeToTokenRequestFailure(Algorithm.RS256, "invalid_client", Errors.INVALID_CLIENT_CREDENTIALS);
        } catch (Exception e) {
            Assert.fail();
        } finally {
            clientResource = ApiUtil.findClientByClientId(adminClient.realm("test"), "client2");
            clientRep = clientResource.toRepresentation();
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setTokenEndpointAuthSigningAlg(null);
            clientResource.update(clientRep);
        }
    }

    @Test
    public void testDirectGrantRequestFailureES256() throws Exception {
        testDirectGrantRequestFailure(Algorithm.ES256);
    }

    @Test
    public void testClockSkew() throws Exception {
        AccessTokenResponse response = testMissingClaim(15, "issuedAt", "notBefore"); // allowable clock skew is 15 sec
        assertSuccess(response, app1.getClientId(), serviceAccountUser.getId(), serviceAccountUser.getUsername());

        // excess allowable clock skew
        response = testMissingClaim(15 + 15, "issuedAt");
        assertError(response, app1.getClientId(), OAuthErrorException.INVALID_CLIENT, Errors.INVALID_CLIENT_CREDENTIALS);
        response = testMissingClaim(15 + 15, "notBefore");
        assertError(response, app1.getClientId(), OAuthErrorException.INVALID_CLIENT, Errors.INVALID_CLIENT_CREDENTIALS);
    }

    @Test
    public void testLongExpirationWithIssuedAt() throws Exception {
        CustomJWTClientCredentialsProvider jwtProvider = new CustomJWTClientCredentialsProvider();
        jwtProvider.setupKeyPair(keyPairClient1);
        jwtProvider.setTokenTimeout(3600); // one hour of token expiration

        // the token should be valid the first time inside the max-exp window
        String jwt = jwtProvider.createSignedRequestToken(app1.getClientId(), getRealmInfoUrl());
        AccessTokenResponse response = doClientCredentialsGrantRequest(jwt);
        assertSuccess(response, app1.getClientId(), serviceAccountUser.getId(), serviceAccountUser.getUsername());

        // in the max-exp window the token should be detected as already used
        setTimeOffset(30);
        response = doClientCredentialsGrantRequest(jwt);
        assertError(response, app1.getClientId(), OAuthErrorException.INVALID_CLIENT, Errors.INVALID_CLIENT_CREDENTIALS);
        assertThat(response.getErrorDescription(), containsString("Token reuse detected"));

        // after the max-exp window the token cannot be used because iat is too far in the past
        setTimeOffset(65);
        response = doClientCredentialsGrantRequest(jwt);
        assertError(response, app1.getClientId(), OAuthErrorException.INVALID_CLIENT, Errors.INVALID_CLIENT_CREDENTIALS);
        assertThat(response.getErrorDescription(), containsString("Token was issued too far in the past to be used now"));
    }

    @Test
    public void testLongExpirationWithoutIssuedAt() throws Exception {
        CustomJWTClientCredentialsProvider jwtProvider = new CustomJWTClientCredentialsProvider();
        jwtProvider.setupKeyPair(keyPairClient1);
        jwtProvider.setTokenTimeout(3600); // one hour of token expiration
        jwtProvider.enableClaim("issuedAt", false);

        // the token should not be valid because expiration is to far in the future
        String jwt = jwtProvider.createSignedRequestToken(app1.getClientId(), getRealmInfoUrl());
        AccessTokenResponse response = doClientCredentialsGrantRequest(jwt);
        assertError(response, app1.getClientId(), OAuthErrorException.INVALID_CLIENT, Errors.INVALID_CLIENT_CREDENTIALS);
        assertThat(response.getErrorDescription(), containsString("Token expiration is too far in the future and iat claim not present in token"));
    }
}
