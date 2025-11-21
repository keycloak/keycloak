/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientAttributeCertificateResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.common.util.KeystoreUtil.KeystoreFormat;
import org.keycloak.common.util.PemUtils;
import org.keycloak.common.util.Time;
import org.keycloak.common.util.UriUtils;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.ECDSAAlgorithm;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.protocol.oidc.client.authentication.JWTClientCredentialsProvider;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.KeyStoreConfig;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.CertificateRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.util.CertificateInfoHelper;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.client.resources.TestApplicationResourceUrls;
import org.keycloak.testsuite.client.resources.TestOIDCEndpointsApplicationResource;
import org.keycloak.testsuite.rest.resource.TestingOIDCEndpointsApplicationResource;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.KeystoreUtils;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.SignatureSignerUtil;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.OAuthClient;
import org.keycloak.util.JsonSerialization;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public abstract class AbstractClientAuthSignedJWTTest extends AbstractKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @ClassRule
    public static TemporaryFolder folder = new TemporaryFolder();

    protected static KeystoreUtils.KeystoreInfo generatedKeystoreClient1;
    protected static KeyPair keyPairClient1;

    @BeforeClass
    public static void generateClient1KeyPair() throws Exception {
        generatedKeystoreClient1 = KeystoreUtils.generateKeystore(folder, KeystoreFormat.JKS, "clientkey", "storepass", "keypass");
        PublicKey publicKey = PemUtils.decodePublicKey(generatedKeystoreClient1.getCertificateInfo().getPublicKey());
        PrivateKey privateKey = PemUtils.decodePrivateKey(generatedKeystoreClient1.getCertificateInfo().getPrivateKey());
        keyPairClient1 = new KeyPair(publicKey, privateKey);
    }

    protected static String client1SAUserId;

    protected static RealmRepresentation testRealm;
    protected static ClientRepresentation app1, app2, app3;
    protected static UserRepresentation defaultUser, serviceAccountUser;

    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();
    }

    protected void allowMultipleAudiencesForClientJWTOnServer(boolean allowMultipleAudiences) {
        getTestingClient().testing().setSystemPropertyOnServer("oidc." + OIDCLoginProtocolFactory.CONFIG_OIDC_ALLOW_MULTIPLE_AUDIENCES_FOR_JWT_CLIENT_AUTHENTICATION, String.valueOf(allowMultipleAudiences));
        getTestingClient().testing().reinitializeProviderFactoryWithSystemPropertiesScope(LoginProtocol.class.getName(), OIDCLoginProtocol.LOGIN_PROTOCOL, "oidc.");
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmBuilder realmBuilder = RealmBuilder.create().name("test")
                .testEventListener();

        app1 = ClientBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .clientId("client1")
                .attribute(JWTClientAuthenticator.CERTIFICATE_ATTR, generatedKeystoreClient1.getCertificateInfo().getCertificate())
                .attribute(OIDCConfigAttributes.USE_REFRESH_TOKEN_FOR_CLIENT_CREDENTIALS_GRANT, "true")
                .authenticatorType(JWTClientAuthenticator.PROVIDER_ID)
                .serviceAccountsEnabled(true)
                .build();

        realmBuilder.client(app1);

        app2 = ClientBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .clientId("client2")
                .directAccessGrants()
                .serviceAccountsEnabled(true)
                .redirectUris(OAuthClient.APP_ROOT + "/auth")
                .attribute(JWTClientAuthenticator.CERTIFICATE_ATTR, "MIICnTCCAYUCBgFPPQDGxTANBgkqhkiG9w0BAQsFADASMRAwDgYDVQQDDAdjbGllbnQxMB4XDTE1MDgxNzE4NTAwNVoXDTI1MDgxNzE4NTE0NVowEjEQMA4GA1UEAwwHY2xpZW50MTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAMMw3PaBffWxgS2PYSDDBp6As+cNvv9kt2C4f/RDAGmvSIHPFev9kuQiKs3Oaws3ZsV4JG3qHEuYgnh9W4vfe3DwNwtD1bjL5FYBhPBFTw0lAQECYxaBHnkjHwUKp957FqdSPPICm3LjmTcEdlH+9dpp9xHCMbbiNiWDzWI1xSxC8Fs2d0hwz1sd+Q4QeTBPIBWcPM+ICZtNG5MN+ORfayu4X+Me5d0tXG2fQO//rAevk1i5IFjKZuOjTwyKB5SJIY4b8QTeg0g/50IU7Ht00Pxw6CK02dHS+FvXHasZlD3ckomqCDjStTBWdhJo5dST0CbOqalkkpLlCCbGA1yEQRsCAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAUIMeJ+EAo8eNpCG/nXImacjrKakbFnZYBGD/gqeTGaZynkX+jgBSructTHR83zSH+yELEhsAy+3BfK4EEihp+PEcRnK2fASVkHste8AQ7rlzC+HGGirlwrVhWCdizNUCGK80DE537IZ7nmZw6LFG9P5/Q2MvCsOCYjRUvMkukq6TdXBXR9tETwZ+0gpSfsOxjj0ZF7ftTRUSzx4rFfcbM9fRNdVizdOuKGc8HJPA5lLOxV6CyaYIvi3y5RlQI1OHeS34lE4w9CNPRFa/vdxXvN7ClyzA0HMFNWxBN7pC/Ht/FbhSvaAagJBHg+vCrcY5C26Oli7lAglf/zZrwUPs0w==")
                .authenticatorType(JWTClientAuthenticator.PROVIDER_ID)
                .build();

        realmBuilder.client(app2);

        defaultUser = UserBuilder.create()
                .id(KeycloakModelUtils.generateId())
                //.serviceAccountId(app1.getClientId())
                .username("test-user@localhost")
                .password("password")
                .build();
        realmBuilder.user(defaultUser);

        client1SAUserId = KeycloakModelUtils.generateId();

        serviceAccountUser = UserBuilder.create()
                .id(client1SAUserId)
                .username(ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + app1.getClientId())
                .serviceAccountId(app1.getClientId())
                .build();
        realmBuilder.user(serviceAccountUser);

        testRealm = realmBuilder.build();
        testRealms.add(testRealm);
    }

    @Before
    public void recreateApp3() {
        app3 = ClientBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .clientId("client3")
                .directAccessGrants()
                .redirectUris(OAuthClient.APP_ROOT + "/auth")
                .authenticatorType(JWTClientAuthenticator.PROVIDER_ID)
                .build();

        Response resp = adminClient.realm("test").clients().create(app3);
        getCleanup().addClientUuid(ApiUtil.getCreatedId(resp));
        resp.close();
    }

    public void testCodeToTokenRequestSuccess(String algorithm) throws Exception {
        testCodeToTokenRequestSuccess("client2", getClient2KeyPair(), algorithm, null);
    }

    public void testCodeToTokenRequestSuccessForceAlgInClient(String algorithm) throws Exception {
        ClientManager.realm(adminClient.realm("test")).clientId("client2")
                .updateAttribute(OIDCConfigAttributes.TOKEN_ENDPOINT_AUTH_SIGNING_ALG, algorithm);
        try {
            testCodeToTokenRequestSuccess(algorithm);
        } finally {
            ClientManager.realm(adminClient.realm("test")).clientId("client2")
                    .updateAttribute(OIDCConfigAttributes.TOKEN_ENDPOINT_AUTH_SIGNING_ALG, null);
        }
    }

    protected void testECDSASignatureLength(String clientSignedToken, String alg) {
        String encodedSignature = clientSignedToken.split("\\.",3)[2];
        byte[] signature = Base64Url.decode(encodedSignature);
        assertEquals(ECDSAAlgorithm.getSignatureLength(alg), signature.length);
    }

    protected String getClientSignedToken(String alg) throws Exception {
        ClientRepresentation clientRepresentation = app2;
        ClientResource clientResource = getClient(testRealm.getRealm(), clientRepresentation.getId());
        clientRepresentation = clientResource.toRepresentation();
        String clientSignedToken;
        try {
            // setup Jwks
            KeyPair keyPair = setupJwksUrl(alg, clientRepresentation, clientResource);
            PublicKey publicKey = keyPair.getPublic();
            PrivateKey privateKey = keyPair.getPrivate();

            // test
            oauth.clientId("client2");
            oauth.doLogin("test-user@localhost", "password");

            String code = oauth.parseLoginResponse().getCode();
            clientSignedToken = createSignedRequestToken("client2", getRealmInfoUrl(), privateKey, publicKey, alg);
            AccessTokenResponse response = doAccessTokenRequest(code, clientSignedToken);

            assertEquals(200, response.getStatusCode());
            oauth.verifyToken(response.getAccessToken());
            oauth.logoutForm().idTokenHint(response.getIdToken()).open();
            return clientSignedToken;
        } finally {
            // Revert jwks_url settings
            revertJwksUriSettings(clientRepresentation, clientResource);
        }
    }

    public void testUploadCertificatePEM(KeyPair keyPair, String algorithm, String curve) throws Exception {
        KeystoreUtils.assumeKeystoreTypeSupported(KeystoreFormat.BCFKS);
        KeystoreUtils.KeystoreInfo ksInfo = KeystoreUtils.generateKeystore(folder, KeystoreFormat.BCFKS, "clientkey", "pwd2", "keypass", keyPair);
        try {
            Path tempFile = Files.createTempFile("cert_", ".pem");
            try (BufferedWriter writer = Files.newBufferedWriter(tempFile)) {
                writer.write(ksInfo.getCertificateInfo().getCertificate());
            }
            testUploadKeystore(org.keycloak.services.resources.admin.ClientAttributeCertificateResource.CERTIFICATE_PEM,
                    tempFile.toFile().getAbsolutePath(), "undefined", "undefined");
            Files.delete(tempFile);

            testCodeToTokenRequestSuccess("client3", keyPair, algorithm, curve);
        } finally {
            ksInfo.getKeystoreFile().delete();
        }
    }

    protected void testUploadPublicKeyPem(KeyPair keyPair, String algorithm, String curve) throws Exception {
        KeystoreUtils.assumeKeystoreTypeSupported(KeystoreFormat.BCFKS);
        KeystoreUtils.KeystoreInfo ksInfo = KeystoreUtils.generateKeystore(folder, KeystoreFormat.BCFKS, "clientkey", "pwd2", "keypass", keyPair);
        try {
            Path tempFile = Files.createTempFile("pubkey_", ".pem");
            try (BufferedWriter writer = Files.newBufferedWriter(tempFile)) {
                writer.write(ksInfo.getCertificateInfo().getPublicKey());
            }
            testUploadKeystore(org.keycloak.services.resources.admin.ClientAttributeCertificateResource.PUBLIC_KEY_PEM,
                    tempFile.toFile().getAbsolutePath(), "undefined", "undefined");
            Files.delete(tempFile);

            testCodeToTokenRequestSuccess("client3", keyPair, algorithm, curve);
        } finally {
            ksInfo.getKeystoreFile().delete();
        }
    }

    protected void testCodeToTokenRequestSuccess(String algorithm, boolean useJwksUri) throws Exception {
        testCodeToTokenRequestSuccess(algorithm, null, useJwksUri);
    }

    private KeyPair setupKeyPair(ClientRepresentation clientRepresentation, ClientResource clientResource,
            String algorithm, String curve, boolean useJwksUri) throws Exception {
        if (useJwksUri) {
            return setupJwksUrl(algorithm, curve, true, false, null, clientRepresentation, clientResource);
        } else {
            return setupJwks(algorithm, curve, clientRepresentation, clientResource);
        }
    }

    protected void testCodeToTokenRequestSuccess(String algorithm, String curve, boolean useJwksUri) throws Exception {
        ClientRepresentation clientRepresentation = app2;
        ClientResource clientResource = getClient(testRealm.getRealm(), clientRepresentation.getId());
        clientRepresentation = clientResource.toRepresentation();
        try {
            KeyPair keyPair = setupKeyPair(clientRepresentation, clientResource, algorithm, curve, useJwksUri);
            testCodeToTokenRequestSuccess("client2", keyPair, algorithm, curve);
        } finally {
            // Revert jwks settings
            if (useJwksUri) {
                revertJwksUriSettings(clientRepresentation, clientResource);
            } else {
                revertJwksSettings(clientRepresentation, clientResource);
            }
        }
    }

    protected void testCodeToTokenRequestSuccess(String clientId, KeyPair keyPair, String algorithm, String curve) throws Exception {
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        // test
        oauth.realm("test").clientId(clientId);
        oauth.doLogin("test-user@localhost", "password");
        EventRepresentation loginEvent = events.expectLogin()
                .client(clientId)
                .assertEvent();

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = doAccessTokenRequest(code,
                createSignedRequestToken(clientId, getRealmInfoUrl(), privateKey, publicKey, algorithm, curve));

        assertEquals(200, response.getStatusCode());
        oauth.verifyToken(response.getAccessToken());
        oauth.parseRefreshToken(response.getRefreshToken());
        events.expectCodeToToken(loginEvent.getDetails().get(Details.CODE_ID), loginEvent.getSessionId())
                .client(clientId)
                .detail(Details.CLIENT_AUTH_METHOD, JWTClientAuthenticator.PROVIDER_ID)
                .assertEvent();
    }

    protected void testDirectGrantRequestSuccess(String algorithm) throws Exception {
        ClientRepresentation clientRepresentation = app2;
        ClientResource clientResource = getClient(testRealm.getRealm(), clientRepresentation.getId());
        clientRepresentation = clientResource.toRepresentation();
        try {
            // setup Jwks
            KeyPair keyPair = setupJwksUrl(algorithm, clientRepresentation, clientResource);
            PublicKey publicKey = keyPair.getPublic();
            PrivateKey privateKey = keyPair.getPrivate();

            // test
            oauth.clientId("client2");
            AccessTokenResponse response = doGrantAccessTokenRequest("test-user@localhost", "password", createSignedRequestToken("client2", getRealmInfoUrl(), privateKey, publicKey, algorithm));

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
        } finally {
            // Revert jwks_url settings
            revertJwksUriSettings(clientRepresentation, clientResource);
        }
    }

    protected void testClientWithGeneratedKeys(String format, Integer keySize, Integer validity) throws Exception {
        ClientRepresentation client = app3;
        UserRepresentation user = defaultUser;
        final String keyAlias = "somekey";
        final String keyPassword = "pwd1";
        final String storePassword = "pwd2";


        // Generate new keystore (which is intended for sending to the user and store in a client app)
        // with public/private keys; in KC, store the certificate itself

        KeyStoreConfig keyStoreConfig = new KeyStoreConfig();
        keyStoreConfig.setFormat(format);
        keyStoreConfig.setKeyPassword(keyPassword);
        keyStoreConfig.setStorePassword(storePassword);
        keyStoreConfig.setKeyAlias(keyAlias);
        if (keySize != null) {
            keyStoreConfig.setKeySize(keySize);
        }
        if (validity != null) {
            keyStoreConfig.setValidity(validity);
        }

        client = getClient(testRealm.getRealm(), client.getId()).toRepresentation();
        final String certOld = client.getAttributes().get(JWTClientAuthenticator.CERTIFICATE_ATTR);

        int expectedValidity = validity == null ? 3 : validity;

        Calendar beforeCreateCalendar = Calendar.getInstance();
        beforeCreateCalendar.add(Calendar.YEAR, expectedValidity);
        long beforeCertCreateTime = beforeCreateCalendar.getTime().getTime();

        // Generate the keystore and save the new certificate in client (in KC)
        byte[] keyStoreBytes = getClientAttributeCertificateResource(testRealm.getRealm(), client.getId())
                .generateAndGetKeystore(keyStoreConfig);

        ByteArrayInputStream keyStoreIs = new ByteArrayInputStream(keyStoreBytes);
        KeyStore keyStore = getKeystore(keyStoreIs, storePassword, format);
        keyStoreIs.close();

        client = getClient(testRealm.getRealm(), client.getId()).toRepresentation();
        X509Certificate x509Cert = (X509Certificate) keyStore.getCertificate(keyAlias);

        assertCertificate(client, certOld,
                KeycloakModelUtils.getPemFromCertificate(x509Cert));
        MatcherAssert.assertThat(x509Cert.getPublicKey(), Matchers.instanceOf(RSAKey.class));
        Assert.assertEquals(keySize == null ? 4096 : keySize, ((RSAKey) x509Cert.getPublicKey()).getModulus().bitLength());

        Calendar afterCreateCalendar = Calendar.getInstance();
        afterCreateCalendar.add(Calendar.YEAR, expectedValidity);
        long afterCertCreateTime = afterCreateCalendar.getTime().getTime();

        // Assert expected "not after" time on certificate. Need some tollerance as "not after" time on certificate is rounded to seconds
        MatcherAssert.assertThat(x509Cert.getNotAfter().getTime(), Matchers.allOf(
                Matchers.greaterThan(beforeCertCreateTime - 1000), Matchers.lessThan(afterCertCreateTime + 1000)));


        // Try to login with the new keys

        oauth.clientId(client.getClientId());
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(keyAlias, keyPassword.toCharArray());
        KeyPair keyPair = new KeyPair(x509Cert.getPublicKey(), privateKey);

        AccessTokenResponse response = doGrantAccessTokenRequest(user.getUsername(),
                user.getCredentials().get(0).getValue(),
                getClientSignedJWT(keyPair, client.getClientId()));

        assertEquals(200, response.getStatusCode());

        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        RefreshToken refreshToken = oauth.parseRefreshToken(response.getRefreshToken());

        events.expectLogin()
                .client(client.getClientId())
                .session(accessToken.getSessionState())
                .detail(Details.GRANT_TYPE, OAuth2Constants.PASSWORD)
                .detail(Details.TOKEN_ID, accessToken.getId())
                .detail(Details.REFRESH_TOKEN_ID, refreshToken.getId())
                .detail(Details.USERNAME, user.getUsername())
                .detail(Details.CLIENT_AUTH_METHOD, JWTClientAuthenticator.PROVIDER_ID)
                .removeDetail(Details.CODE_ID)
                .removeDetail(Details.REDIRECT_URI)
                .removeDetail(Details.CONSENT)
                .assertEvent();
    }


    // We need to test this as a genuine REST API HTTP request
    // since there's no easy and direct way to call ClientAttributeCertificateResource.uploadJksCertificate
    // (and especially to create MultipartFormDataInput)
    protected void testUploadKeystore(String keystoreFormat, String filePath, String keyAlias, String storePassword) throws Exception {
        ClientRepresentation client = getClient(testRealm.getRealm(), app3.getId()).toRepresentation();
        final String certOld = client.getAttributes().get(JWTClientAuthenticator.CERTIFICATE_ATTR);

        // Load the keystore file
        URL fileUrl = (getClass().getClassLoader().getResource(filePath));
        File keystoreFile = fileUrl != null ? new File(fileUrl.getFile()) : new File(filePath);
        if (!keystoreFile.exists()) {
            throw new IOException("File not found: " + keystoreFile.getAbsolutePath());
        }

        // Get admin access token, no matter it's master realm's admin
        AccessTokenResponse accessTokenResponse = oauth.realm(AuthRealm.MASTER).client("admin-cli").doPasswordGrantRequest(
                AuthRealm.ADMIN, AuthRealm.ADMIN);
        assertEquals(200, accessTokenResponse.getStatusCode());

        final String url = suiteContext.getAuthServerInfo().getContextRoot()
                + "/auth/admin/realms/" + testRealm.getRealm()
                + "/clients/" + client.getId() + "/certificates/jwt.credential/upload-certificate";

        // Prepare the HTTP request
        FileBody fileBody = new FileBody(keystoreFile);
        HttpEntity entity = MultipartEntityBuilder.create()
                .addPart("file", fileBody)
                .addTextBody("keystoreFormat", keystoreFormat)
                .addTextBody("keyAlias", keyAlias)
                .addTextBody("storePassword", storePassword)
                .addTextBody("keyPassword", "undefined")
                .build();
        HttpPost httpRequest = new HttpPost(url);
        httpRequest.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenResponse.getAccessToken());
        httpRequest.setEntity(entity);

        // Send the request
        HttpClient httpClient = HttpClients.createDefault();
        HttpResponse httpResponse = httpClient.execute(httpRequest);
        assertEquals(200, httpResponse.getStatusLine().getStatusCode());

        client = getClient(testRealm.getRealm(), client.getId()).toRepresentation();

        // Assert the uploaded certificate
        if (keystoreFormat.equals(org.keycloak.services.resources.admin.ClientAttributeCertificateResource.PUBLIC_KEY_PEM)) {
            String pem = new String(Files.readAllBytes(keystoreFile.toPath()));
            final String publicKeyNew = client.getAttributes().get(JWTClientAuthenticator.ATTR_PREFIX + "." + CertificateInfoHelper.PUBLIC_KEY);
            assertEquals("Certificates don't match", pem, publicKeyNew);
        } else if (keystoreFormat.equals(org.keycloak.services.resources.admin.ClientAttributeCertificateResource.JSON_WEB_KEY_SET)) {
            Assert.assertEquals("true", client.getAttributes().get(OIDCConfigAttributes.USE_JWKS_STRING));
            String jwks = new String(Files.readAllBytes(keystoreFile.toPath()));
            Assert.assertEquals(jwks, client.getAttributes().get(OIDCConfigAttributes.JWKS_STRING));
            CertificateRepresentation info = getClient(testRealm.getRealm(), client.getId())
                    .getCertficateResource(JWTClientAuthenticator.ATTR_PREFIX).getKeyInfo();
            Assert.assertNotNull(info.getPublicKey());
            // Just assert it's valid public key
            PublicKey pk = KeycloakModelUtils.getPublicKey(info.getPublicKey());
            Assert.assertNotNull(pk);
        } else if (keystoreFormat.equals(org.keycloak.services.resources.admin.ClientAttributeCertificateResource.CERTIFICATE_PEM)) {
            String pem = new String(Files.readAllBytes(keystoreFile.toPath()));
            assertCertificate(client, certOld, pem);
        } else {
            InputStream keystoreIs = new FileInputStream(keystoreFile);
            KeyStore keyStore = getKeystore(keystoreIs, storePassword, keystoreFormat);
            keystoreIs.close();
            String pem = KeycloakModelUtils.getPemFromCertificate((X509Certificate) keyStore.getCertificate(keyAlias));
            assertCertificate(client, certOld, pem);
        }
    }

    protected List<NameValuePair> createTokenWithSpecifiedAudience(ClientResource clientResource, ClientRepresentation clientRepresentation, String... audience) throws Exception {
        KeyPair keyPair = setupJwksUrl(Algorithm.PS256, clientRepresentation, clientResource);
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();
        JsonWebToken assertion = createRequestToken(app2.getClientId(), getRealmInfoUrl());

        assertion.audience(audience);

        List<NameValuePair> parameters = new LinkedList<NameValuePair>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CLIENT_CREDENTIALS));
        parameters
                .add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION,
                createSignedRequestToken(privateKey, publicKey, Algorithm.PS256, null, assertion, null)));
        return parameters;
    }

    protected void testEndpointAsAudience(String endpointUrl) throws Exception {
        ClientRepresentation clientRepresentation = app2;
        ClientResource clientResource = getClient(testRealm.getRealm(), clientRepresentation.getId());
        clientRepresentation = clientResource.toRepresentation();
        try {
            List<NameValuePair> parameters = createTokenWithSpecifiedAudience(clientResource, clientRepresentation, endpointUrl);

            try (CloseableHttpResponse resp = sendRequest(oauth.getEndpoints().getToken(), parameters)) {
                AccessTokenResponse response = new AccessTokenResponse(resp);
                assertNotNull(response.getAccessToken());
            }
        } finally {
            revertJwksUriSettings(clientRepresentation, clientResource);
        }
    }

    protected AccessTokenResponse testMissingClaim(String... claims) throws Exception {
        return testMissingClaim(0, claims);
    }

    protected AccessTokenResponse testMissingClaim(int tokenTimeOffset, String... claims) throws Exception {
        CustomJWTClientCredentialsProvider jwtProvider = new CustomJWTClientCredentialsProvider();
        jwtProvider.setupKeyPair(keyPairClient1);
        jwtProvider.setTokenTimeout(10);

        for (String claim : claims) {
            jwtProvider.enableClaim(claim, false);
        }

        Time.setOffset(tokenTimeOffset);
        String jwt;
        try {
            jwt = jwtProvider.createSignedRequestToken(app1.getClientId(), getRealmInfoUrl());
        } finally {
            Time.setOffset(0);
        }
        return doClientCredentialsGrantRequest(jwt);
    }

    protected void assertError(AccessTokenResponse response, String clientId, String responseError, String eventError) {
        assertEquals(400, response.getStatusCode());
        assertMessageError(response,clientId,responseError,eventError);
    }

    protected void assertError(AccessTokenResponse response, int erroCode, String clientId, String responseError, String eventError) {
        assertEquals(erroCode, response.getStatusCode());
        assertMessageError(response, clientId, responseError, eventError);
    }

    protected void assertMessageError(AccessTokenResponse response, String clientId, String responseError, String eventError) {
        assertEquals(responseError, response.getError());

        events.expectClientLogin()
                .client(clientId)
                .session((String) null)
                .clearDetails()
                .error(eventError)
                .user((String) null)
                .assertEvent();
    }

    protected void assertSuccess(AccessTokenResponse response, String clientId, String userId, String userName) {
        assertEquals(200, response.getStatusCode());

        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        RefreshToken refreshToken = oauth.parseRefreshToken(response.getRefreshToken());

        events.expectClientLogin()
                .client(clientId)
                .user(userId)
                .session(accessToken.getSessionState())
                .detail(Details.TOKEN_ID, accessToken.getId())
                .detail(Details.REFRESH_TOKEN_ID, refreshToken.getId())
                .detail(Details.USERNAME, userName)
                .detail(Details.CLIENT_AUTH_METHOD, JWTClientAuthenticator.PROVIDER_ID)
                .assertEvent();
    }

    protected static void assertCertificate(ClientRepresentation client, String certOld, String pem) {
        pem = PemUtils.removeBeginEnd(pem);
        final String certNew = client.getAttributes().get(JWTClientAuthenticator.CERTIFICATE_ATTR);
        assertNotEquals("The old and new certificates shouldn't match", certOld, certNew);
        assertEquals("Certificates don't match", pem, certNew);
    }

    protected void testCodeToTokenRequestFailure(String algorithm, String error, String description) throws Exception {
        ClientRepresentation clientRepresentation = app2;
        ClientResource clientResource = getClient(testRealm.getRealm(), clientRepresentation.getId());
        clientRepresentation = clientResource.toRepresentation();
        try {
            // setup Jwks
            KeyPair keyPair = setupJwksUrl(algorithm, clientRepresentation, clientResource);
            PublicKey publicKey = keyPair.getPublic();
            PrivateKey privateKey = keyPair.getPrivate();

            // test
            oauth.clientId("client2");
            oauth.doLogin("test-user@localhost", "password");
            EventRepresentation loginEvent = events.expectLogin()
                    .client("client2")
                    .assertEvent();

            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse response = doAccessTokenRequest(code, getClient2SignedJWT());

            assertEquals(400, response.getStatusCode());
            assertEquals(error, response.getError());

            events.expect(EventType.CODE_TO_TOKEN_ERROR)
                    .client("client2")
                    .session((String) null)
                    .clearDetails()
                    .error(description)
                    .user((String) null)
                    .assertEvent();
        } finally {
            // Revert jwks_url settings
            revertJwksUriSettings(clientRepresentation, clientResource);
        }
    }

    protected void testDirectGrantRequestFailure(String algorithm) throws Exception {
        ClientRepresentation clientRepresentation = app2;
        ClientResource clientResource = getClient(testRealm.getRealm(), clientRepresentation.getId());
        clientRepresentation = clientResource.toRepresentation();
        try {
            // setup Jwks
            setupJwksUrl(algorithm, clientRepresentation, clientResource);

            // test
            oauth.clientId("client2");
            AccessTokenResponse response = doGrantAccessTokenRequest("test-user@localhost", "password", getClient2SignedJWT());

            assertEquals(400, response.getStatusCode());
            assertEquals(OAuthErrorException.INVALID_CLIENT, response.getError());

            events.expect(EventType.LOGIN_ERROR)
                    .client("client2")
                    .session((String) null)
                    .clearDetails()
                    .error("client_credentials_setup_required")
                    .user((String) null)
                    .assertEvent();
        } finally {
            // Revert jwks_url settings
            revertJwksUriSettings(clientRepresentation, clientResource);
        }
    }

    // HELPER METHODS

    protected AccessTokenResponse doAccessTokenRequest(String code, String signedJwt) throws Exception {
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.AUTHORIZATION_CODE));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CODE, code));
        parameters.add(new BasicNameValuePair(OAuth2Constants.REDIRECT_URI, oauth.getRedirectUri()));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, signedJwt));

        CloseableHttpResponse response = sendRequest(oauth.getEndpoints().getToken(), parameters);
        return new AccessTokenResponse(response);
    }

    protected AccessTokenResponse doRefreshTokenRequest(String refreshToken, String signedJwt) throws Exception {
        List<NameValuePair> parameters = new LinkedList<NameValuePair>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.REFRESH_TOKEN));
        parameters.add(new BasicNameValuePair(OAuth2Constants.REFRESH_TOKEN, refreshToken));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, signedJwt));

        CloseableHttpResponse response = sendRequest(oauth.getEndpoints().getToken(), parameters);
        return new AccessTokenResponse(response);
    }

    protected HttpResponse doLogout(String refreshToken, String signedJwt) throws Exception {
        List<NameValuePair> parameters = new LinkedList<NameValuePair>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.REFRESH_TOKEN));
        parameters.add(new BasicNameValuePair(OAuth2Constants.REFRESH_TOKEN, refreshToken));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, signedJwt));

        return sendRequest(oauth.getEndpoints().getLogout(), parameters);
    }

    protected AccessTokenResponse doClientCredentialsGrantRequest(String signedJwt) throws Exception {
        List<NameValuePair> parameters = new LinkedList<NameValuePair>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CLIENT_CREDENTIALS));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, signedJwt));

        CloseableHttpResponse response = sendRequest(oauth.getEndpoints().getToken(), parameters);
        return new AccessTokenResponse(response);
    }

    protected AccessTokenResponse doGrantAccessTokenRequest(String username, String password, String signedJwt) throws Exception {
        List<NameValuePair> parameters = new LinkedList<NameValuePair>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD));
        parameters.add(new BasicNameValuePair("username", username));
        parameters.add(new BasicNameValuePair("password", password));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, signedJwt));

        CloseableHttpResponse response = sendRequest(oauth.getEndpoints().getToken(), parameters);
        return new AccessTokenResponse(response);
    }

    protected CloseableHttpResponse sendRequest(String requestUrl, List<NameValuePair> parameters) throws Exception {
        try (CloseableHttpClient client = new DefaultHttpClient()) {
            HttpPost post = new HttpPost(requestUrl);
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
            post.setEntity(formEntity);
            return client.execute(post);
        }
    }

    protected String getClient2SignedJWT(String algorithm) {
        return getClientSignedJWT(getClient2KeyPair(), "client2", algorithm);
    }

    protected String getClient1SignedJWT() throws Exception {
        return getClientSignedJWT(keyPairClient1, "client1", Algorithm.RS256);
    }

    protected String getClient2SignedJWT() {
        return getClientSignedJWT(getClient2KeyPair(), "client2", Algorithm.RS256);
    }

    protected KeyPair getClient2KeyPair() {
        return KeystoreUtil.loadKeyPairFromKeystore("classpath:client-auth-test/keystore-client2.jks",
                "storepass", "keypass", "clientkey", KeystoreUtil.KeystoreFormat.JKS);
    }

    protected String getClientSignedJWT(KeyPair keyPair, String clientId) {
        return getClientSignedJWT(keyPair, clientId, Algorithm.RS256);
    }

    private String getClientSignedJWT(KeyPair keyPair, String clientId, String algorithm) {
        JWTClientCredentialsProvider jwtProvider = new JWTClientCredentialsProvider();
        jwtProvider.setupKeyPair(keyPair, algorithm);
        jwtProvider.setTokenTimeout(10);
        return jwtProvider.createSignedRequestToken(clientId, getRealmInfoUrl());
    }

    protected String getRealmInfoUrl() {
        String authServerBaseUrl = UriUtils.getOrigin(oauth.getRedirectUri()) + "/auth";
        return KeycloakUriBuilder.fromUri(authServerBaseUrl).path(ServiceUrlConstants.REALM_INFO_PATH).build("test").toString();
    }

    protected ClientAttributeCertificateResource getClientAttributeCertificateResource(String realm, String clientId) {
        return getClient(realm, clientId).getCertficateResource("jwt.credential");
    }

    protected ClientResource getClient(String realm, String clientId) {
        return realmsResouce().realm(realm).clients().get(clientId);
    }

    /**
     * Custom JWTClientCredentialsProvider with support for missing JWT claims
     */
    protected class CustomJWTClientCredentialsProvider extends JWTClientCredentialsProvider {
        private Map<String, Boolean> enabledClaims = new HashMap<>();

        public CustomJWTClientCredentialsProvider() {
            super();

            final String[] claims = {"id", "issuer", "subject", "audience", "expiration", "notBefore", "issuedAt"};
            for (String claim : claims) {
                enabledClaims.put(claim, true);
            }
        }

        public void enableClaim(String claim, boolean value) {
            if (!enabledClaims.containsKey(claim)) {
                throw new IllegalArgumentException("Claim \"" + claim + "\" doesn't exist");
            }
            enabledClaims.put(claim, value);
        }

        public boolean isClaimEnabled(String claim) {
            Boolean value = enabledClaims.get(claim);
            if (value == null) {
                throw new IllegalArgumentException("Claim \"" + claim + "\" doesn't exist");
            }
            return value;
        }

        public Set<String> getClaims() {
            return enabledClaims.keySet();
        }

        @Override
        protected JsonWebToken createRequestToken(String clientId, String realmInfoUrl) {
            JsonWebToken reqToken = new JsonWebToken();
            if (isClaimEnabled("id")) reqToken.id(KeycloakModelUtils.generateId());
            if (isClaimEnabled("issuer")) reqToken.issuer(clientId);
            if (isClaimEnabled("subject")) reqToken.subject(clientId);
            if (isClaimEnabled("audience")) reqToken.audience(realmInfoUrl);

            long now = Time.currentTime();
            if (isClaimEnabled("issuedAt")) reqToken.iat(now);
            if (isClaimEnabled("expiration")) reqToken.exp(now + getTokenTimeout());
            if (isClaimEnabled("notBefore")) reqToken.nbf(now);

            return reqToken;
        }
    }

    private static KeyStore getKeystore(InputStream is, String storePassword, String format) throws Exception {
        KeyStore keyStore = CryptoIntegration.getProvider().getKeyStore(KeystoreFormat.valueOf(format));
        keyStore.load(is, storePassword.toCharArray());
        return keyStore;
    }

    protected KeyPair setupJwksUrl(String algorithm, ClientRepresentation clientRepresentation, ClientResource clientResource) throws Exception {
        return setupJwksUrl(algorithm, null, true, false, null, clientRepresentation, clientResource);
    }

    protected KeyPair setupJwksUrl(String algorithm, boolean advertiseJWKAlgorithm, boolean keepExistingKeys, String kid,
                                   ClientRepresentation clientRepresentation, ClientResource clientResource) throws Exception {
        return setupJwksUrl(algorithm, null, advertiseJWKAlgorithm, keepExistingKeys, kid, clientRepresentation, clientResource);
    }

    protected KeyPair setupJwksUrl(String algorithm, String curve, boolean advertiseJWKAlgorithm, boolean keepExistingKeys, String kid,
                                   ClientRepresentation clientRepresentation, ClientResource clientResource) throws Exception {
        // generate and register client keypair
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        oidcClientEndpointsResource.generateKeys(algorithm, curve, advertiseJWKAlgorithm, keepExistingKeys, kid);
        Map<String, String> generatedKeys = oidcClientEndpointsResource.getKeysAsBase64();
        KeyPair keyPair = getKeyPairFromGeneratedBase64(generatedKeys, algorithm, curve);

        // use and set jwks_url
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRepresentation).setUseJwksUrl(true);
        String jwksUrl = TestApplicationResourceUrls.clientJwksUri();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRepresentation).setJwksUrl(jwksUrl);
        clientResource.update(clientRepresentation);

        // set time offset, so that new keys are downloaded
        setTimeOffset(20);

        return keyPair;
    }

    private KeyPair setupJwks(String algorithm, String curve, ClientRepresentation clientRepresentation, ClientResource clientResource)
            throws Exception {
        // generate and register client keypair
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        oidcClientEndpointsResource.generateKeys(algorithm, curve);
        Map<String, String> generatedKeys = oidcClientEndpointsResource.getKeysAsBase64();
        KeyPair keyPair = getKeyPairFromGeneratedBase64(generatedKeys, algorithm, curve);

        // use and set JWKS
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRepresentation).setUseJwksString(true);
        JSONWebKeySet keySet = oidcClientEndpointsResource.getJwks();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRepresentation)
                .setJwksString(JsonSerialization.writeValueAsString(keySet));
        clientResource.update(clientRepresentation);

        // set time offset, so that new keys are downloaded
        setTimeOffset(20);

        return keyPair;
    }

    protected void revertJwksUriSettings(ClientRepresentation clientRepresentation, ClientResource clientResource) {
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRepresentation).setUseJwksUrl(false);
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRepresentation).setJwksUrl(null);
        clientResource.update(clientRepresentation);
    }

    private void revertJwksSettings(ClientRepresentation clientRepresentation, ClientResource clientResource) {
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRepresentation).setUseJwksString(false);
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRepresentation).setJwksString(null);
        clientResource.update(clientRepresentation);
    }

    private KeyPair getKeyPairFromGeneratedBase64(Map<String, String> generatedKeys, String algorithm, String curve) throws Exception {
        // It seems that PemUtils.decodePrivateKey, decodePublicKey can only treat RSA type keys, not EC type keys. Therefore, these are not used.
        String privateKeyBase64 = generatedKeys.get(TestingOIDCEndpointsApplicationResource.PRIVATE_KEY);
        String publicKeyBase64 =  generatedKeys.get(TestingOIDCEndpointsApplicationResource.PUBLIC_KEY);
        PrivateKey privateKey = decodePrivateKey(Base64.getDecoder().decode(privateKeyBase64), algorithm, curve);
        PublicKey publicKey = decodePublicKey(Base64.getDecoder().decode(publicKeyBase64), algorithm, curve);
        return new KeyPair(publicKey, privateKey);
    }

    private PrivateKey decodePrivateKey(byte[] der, String algorithm, String curve) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
        String keyAlg = getKeyAlgorithmFromJwaAlgorithm(algorithm, curve);
        KeyFactory kf = CryptoIntegration.getProvider().getKeyFactory(keyAlg);
        return kf.generatePrivate(spec);
    }

    private PublicKey decodePublicKey(byte[] der, String algorithm, String curve) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
        String keyAlg = getKeyAlgorithmFromJwaAlgorithm(algorithm, curve);
        KeyFactory kf = CryptoIntegration.getProvider().getKeyFactory(keyAlg);
        return kf.generatePublic(spec);
    }

    protected String createSignedRequestToken(String clientId, String realmInfoUrl, PrivateKey privateKey, PublicKey publicKey, String algorithm) {
        return createSignedRequestToken(privateKey, publicKey, algorithm, null, createRequestToken(clientId, realmInfoUrl), null);
    }

    protected String createSignedRequestToken(String clientId, String realmInfoUrl, PrivateKey privateKey, PublicKey publicKey, String algorithm, String curve) {
        return createSignedRequestToken(privateKey, publicKey, algorithm, null, createRequestToken(clientId, realmInfoUrl), curve);
    }

    protected String createSignledRequestToken(PrivateKey privateKey, PublicKey publicKey, String algorithm, String kid, JsonWebToken jwt) {
        return createSignedRequestToken(privateKey, publicKey, algorithm, kid, jwt, null);
    }

    protected String createSignedRequestToken(PrivateKey privateKey, PublicKey publicKey, String algorithm, String kid, JsonWebToken jwt, String curve) {
        if (kid == null) {
            kid = KeyUtils.createKeyId(publicKey);
        }
        SignatureSignerContext signer = SignatureSignerUtil.createSigner(privateKey, kid, algorithm, curve);
        String ret = new JWSBuilder().kid(kid).jsonContent(jwt).sign(signer);
        return ret;
    }

    protected JsonWebToken createRequestToken(String clientId, String realmInfoUrl) {
        JsonWebToken reqToken = new JsonWebToken();
        reqToken.id(KeycloakModelUtils.generateId());
        reqToken.issuer(clientId);
        reqToken.subject(clientId);
        reqToken.audience(realmInfoUrl);

        long now = Time.currentTime();
        reqToken.iat(now);
        reqToken.exp(now + 10);
        reqToken.nbf(now);

        return reqToken;
    }

    protected String getKeyAlgorithmFromJwaAlgorithm(String jwaAlgorithm, String curve) {
        String keyAlg = null;
        switch (jwaAlgorithm) {
            case Algorithm.RS256:
            case Algorithm.RS384:
            case Algorithm.RS512:
            case Algorithm.PS256:
            case Algorithm.PS384:
            case Algorithm.PS512:
                keyAlg = KeyType.RSA;
                break;
            case Algorithm.ES256:
            case Algorithm.ES384:
            case Algorithm.ES512:
                keyAlg = KeyType.EC;
                break;
            default :
                throw new RuntimeException("Unsupported signature algorithm");
        }
        return keyAlg;
    }
}
