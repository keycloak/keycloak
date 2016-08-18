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

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.AdapterUtils;
import org.keycloak.adapters.authentication.JWTClientCredentialsProvider;
import org.keycloak.admin.client.resource.ClientAttributeCertificateResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.common.util.*;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.KeyStoreConfig;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class ClientAuthSignedJWTTest extends AbstractKeycloakTest {
    public static final String CERTIFICATE_PEM = "Certificate PEM";

    @Rule
    public AssertEvents events = new AssertEvents(this);
    private String client1SAUserId;

    private RealmRepresentation testRealm;
    private ClientRepresentation app1, app2, app3;
    private UserRepresentation defaultUser, serviceAccountUser;

    @BeforeClass
    public static void beforeClientAuthSignedJWTTest() {
        BouncyIntegration.init();
    }

    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmBuilder realmBuilder = RealmBuilder.create().name("test")
                .privateKey("MIICXAIBAAKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQABAoGAfmO8gVhyBxdqlxmIuglbz8bcjQbhXJLR2EoS8ngTXmN1bo2L90M0mUKSdc7qF10LgETBzqL8jYlQIbt+e6TH8fcEpKCjUlyq0Mf/vVbfZSNaVycY13nTzo27iPyWQHK5NLuJzn1xvxxrUeXI6A2WFpGEBLbHjwpx5WQG9A+2scECQQDvdn9NE75HPTVPxBqsEd2z10TKkl9CZxu10Qby3iQQmWLEJ9LNmy3acvKrE3gMiYNWb6xHPKiIqOR1as7L24aTAkEAtyvQOlCvr5kAjVqrEKXalj0Tzewjweuxc0pskvArTI2Oo070h65GpoIKLc9jf+UA69cRtquwP93aZKtW06U8dQJAF2Y44ks/mK5+eyDqik3koCI08qaC8HYq2wVl7G2QkJ6sbAaILtcvD92ToOvyGyeE0flvmDZxMYlvaZnaQ0lcSQJBAKZU6umJi3/xeEbkJqMfeLclD27XGEFoPeNrmdx0q10Azp4NfJAY+Z8KRyQCR2BEG+oNitBOZ+YXF9KCpH3cdmECQHEigJhYg+ykOvr1aiZUMFT72HU0jnmQe2FVekuG+LJUt2Tm7GtMjTFoGpf0JwrVuZN39fOYAlo+nTixgeW7X8Y=")
                .publicKey("MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQAB")
                .testEventListener();

        app1 = ClientBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .clientId("client1")
                .attribute(JWTClientAuthenticator.CERTIFICATE_ATTR, "MIICnTCCAYUCBgFPPLDaTzANBgkqhkiG9w0BAQsFADASMRAwDgYDVQQDDAdjbGllbnQxMB4XDTE1MDgxNzE3MjI0N1oXDTI1MDgxNzE3MjQyN1owEjEQMA4GA1UEAwwHY2xpZW50MTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAIUjjgv+V3s96O+Za9002Lp/trtGuHBeaeVL9dFKMKzO2MPqdRmHB4PqNlDdd28Rwf5Xn6iWdFpyUKOnI/yXDLhdcuFpR0sMNK/C9Lt+hSpPFLuzDqgtPgDotlMxiHIWDOZ7g9/gPYNXbNvjv8nSiyqoguoCQiiafW90bPHsiVLdP7ZIUwCcfi1qQm7FhxRJ1NiW5dvUkuCnnWEf0XR+Wzc5eC9EgB0taLFiPsSEIlWMm5xlahYyXkPdNOqZjiRnrTWm5Y4uk8ZcsD/KbPTf/7t7cQXipVaswgjdYi1kK2/zRwOhg1QwWFX/qmvdd+fLxV0R6VqRDhn7Qep2cxwMxLsCAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAKE6OA46sf20bz8LZPoiNsqRwBUDkaMGXfnob7s/hJZIIwDEx0IAQ3uKsG7q9wb+aA6s+v7S340zb2k3IxuhFaHaZpAd4CyR5cn1FHylbzoZ7rI/3ASqHDqpljdJaFqPH+m7nZWtyDvtZf+gkZ8OjsndwsSBK1d/jMZPp29qYbl1+XfO7RCp/jDqro/R3saYFaIFiEZPeKn1hUJn6BO48vxH1xspSu9FmlvDOEAOz4AuM58z4zRMP49GcFdCWr1wkonJUHaSptJaQwmBwLFUkCbE5I1ixGMb7mjEud6Y5jhfzJiZMo2U8RfcjNbrN0diZl3jB6LQIwESnhYSghaTjNQ==")
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

        app3 = ClientBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .clientId("client3")
                .directAccessGrants()
                .authenticatorType(JWTClientAuthenticator.PROVIDER_ID)
                .build();

        realmBuilder.client(app3);

        // This one is for keystore-client2.p12 , which doesn't work on Sun JDK
//            app2.setAttribute(JWTClientAuthenticator.CERTIFICATE_ATTR, "MIICnTCCAYUCBgFPPLGHHjANBgkqhkiG9w0BAQsFADASMRAwDgYDVQQDDAdjbGllbnQxMB4XDTE1MDgxNzE3MjMzMVoXDTI1MDgxNzE3MjUxMVowEjEQMA4GA1UEAwwHY2xpZW50MTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAIsatXj38fFD9fHslNrsWrubobudXYwwdZpGYqkHIhuDeSojGvhBSLmKIFmtbHMVcLEbS0dIEsSbNVrwjdFfuRuvd9Vu6Ng0JUC8fRhSeQniC3jcBuP8P4WlXK4+ir3Wlya+T6Hum9b68BiH0KyNZtFGJ6zLHuCcq9Bl0JifvibnUkDeTZPwgJNA9+GxS/x8fAkApcAbJrgBZvr57PwhbgHoZdB8aAY5f5ogbGzKDtSUMvFh+Jah39gWtn7p3VOuuMXA8SugogoH8C5m2itrPBL1UPhAcKUeWiqx4SmZe/lZo7x2WbSecNiFaiqBhIW+QbqCYW6I4u0YvuLuEe3+TC8CAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAZzW5DZviCxUQdV5Ab07PZkUfvImHZ73oWWHZqzUQtZtbVdzfp3cnbb2wyXtlOvingO3hgpoTxV8vbKgLbIQfvkGGHBG1F5e0QVdtikfdcwWb7cy4/9F80OD7cgG0ZAzFbQ8ZY7iS3PToBp3+4tbIK2NK0ntt/MYgJnPbHeG4V4qfgUbFm1YgEK7WpbSVU8jGuJ5DWE+mlYgECZKZ5TSlaVGs2XOm6WXrJScucNekwcBWWiHyRsFHZEDzWmzt8TLTLnnb0vVjhx3qCYxah3RbyyMZm6WLZlLAaGEcwNDO8jaA3hAjrxoOA1xEaolQfGVsb/ElelHcR1Zfe0u4Ekd4tw==");

        defaultUser = UserBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .serviceAccountId(app1.getClientId())
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

    // TEST SUCCESS

    @Test
    public void testServiceAccountAndLogoutSuccess() throws Exception {
        String client1Jwt = getClient1SignedJWT();
        OAuthClient.AccessTokenResponse response = doClientCredentialsGrantRequest(client1Jwt);

        assertEquals(200, response.getStatusCode());
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        RefreshToken refreshToken = oauth.verifyRefreshToken(response.getRefreshToken());

        events.expectClientLogin()
                .client("client1")
                .user(client1SAUserId)
                .session(accessToken.getSessionState())
                .detail(Details.TOKEN_ID, accessToken.getId())
                .detail(Details.REFRESH_TOKEN_ID, refreshToken.getId())
                .detail(Details.USERNAME, ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + "client1")
                .detail(Details.CLIENT_AUTH_METHOD, JWTClientAuthenticator.PROVIDER_ID)
                .assertEvent();

        assertEquals(accessToken.getSessionState(), refreshToken.getSessionState());

        client1Jwt = getClient1SignedJWT();
        OAuthClient.AccessTokenResponse refreshedResponse = doRefreshTokenRequest(response.getRefreshToken(), client1Jwt);
        AccessToken refreshedAccessToken = oauth.verifyToken(refreshedResponse.getAccessToken());
        RefreshToken refreshedRefreshToken = oauth.verifyRefreshToken(refreshedResponse.getRefreshToken());

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
                .user(client1SAUserId)
                .removeDetail(Details.TOKEN_ID)
                .removeDetail(Details.UPDATED_REFRESH_TOKEN_ID)
                .detail(Details.CLIENT_AUTH_METHOD, JWTClientAuthenticator.PROVIDER_ID)
                .error(Errors.INVALID_TOKEN).assertEvent();

    }

    @Test
    public void testCodeToTokenRequestSuccess() throws Exception {
        oauth.clientId("client2");
        oauth.doLogin("test-user@localhost", "password");
        EventRepresentation loginEvent = events.expectLogin()
                .client("client2")
                .assertEvent();

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = doAccessTokenRequest(code, getClient2SignedJWT());

        assertEquals(200, response.getStatusCode());
        oauth.verifyToken(response.getAccessToken());
        oauth.verifyRefreshToken(response.getRefreshToken());
        events.expectCodeToToken(loginEvent.getDetails().get(Details.CODE_ID), loginEvent.getSessionId())
                .client("client2")
                .detail(Details.CLIENT_AUTH_METHOD, JWTClientAuthenticator.PROVIDER_ID)
                .assertEvent();
    }

    @Test
    public void testDirectGrantRequestSuccess() throws Exception {
        oauth.clientId("client2");
        OAuthClient.AccessTokenResponse response = doGrantAccessTokenRequest("test-user@localhost", "password", getClient2SignedJWT());

        assertEquals(200, response.getStatusCode());
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        RefreshToken refreshToken = oauth.verifyRefreshToken(response.getRefreshToken());

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
    public void testClientWithGeneratedKeysJKS() throws Exception {
        testClientWithGeneratedKeys("JKS");
    }

    @Test
    public void testClientWithGeneratedKeysPKCS12() throws Exception {
        testClientWithGeneratedKeys("PKCS12");
    }

    private void testClientWithGeneratedKeys(String format) throws Exception {
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

        client = getClient(testRealm.getRealm(), client.getId()).toRepresentation();
        final String certOld = client.getAttributes().get(JWTClientAuthenticator.CERTIFICATE_ATTR);

        // Generate the keystore and save the new certificate in client (in KC)
        byte[] keyStoreBytes = getClientAttributeCertificateResource(testRealm.getRealm(), client.getId())
                .generateAndGetKeystore(keyStoreConfig);

        ByteArrayInputStream keyStoreIs = new ByteArrayInputStream(keyStoreBytes);
        KeyStore keyStore = getKeystore(keyStoreIs, storePassword, format);
        keyStoreIs.close();

        client = getClient(testRealm.getRealm(), client.getId()).toRepresentation();

        assertCertificate(client, certOld,
                KeycloakModelUtils.getPemFromCertificate((X509Certificate) keyStore.getCertificate(keyAlias)));


        // Try to login with the new keys

        oauth.clientId(client.getClientId());
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(keyAlias, keyPassword.toCharArray());
        OAuthClient.AccessTokenResponse response = doGrantAccessTokenRequest(user.getUsername(),
                                                      user.getCredentials().get(0).getValue(),
                                                        getClientSignedJWT(privateKey, client.getClientId()));

        assertEquals(200, response.getStatusCode());

        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        RefreshToken refreshToken = oauth.verifyRefreshToken(response.getRefreshToken());

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

    @Test
    public void testUploadKeystoreJKS() throws Exception {
        testUploadKeystore("JKS", "client-auth-test/keystore-client1.jks", "clientkey", "storepass");
    }

    @Test
    public void testUploadKeystorePKCS12() throws Exception {
        testUploadKeystore("PKCS12", "client-auth-test/keystore-client2.p12", "clientkey", "pwd2");
    }

    @Test
    public void testUploadPEM() throws Exception {
        testUploadKeystore(CERTIFICATE_PEM, "client-auth-test/certificate.pem", "undefined", "undefined");
    }

    // We need to test this as a genuine REST API HTTP request
    // since there's no easy and direct way to call ClientAttributeCertificateResource.uploadJksCertificate
    // (and especially to create MultipartFormDataInput)
    private void testUploadKeystore(String keystoreFormat, String filePath, String keyAlias, String storePassword) throws Exception {
        ClientRepresentation client = getClient(testRealm.getRealm(), app3.getId()).toRepresentation();
        final String certOld = client.getAttributes().get(JWTClientAuthenticator.CERTIFICATE_ATTR);

        // Load the keystore file
        URL fileUrl = (getClass().getClassLoader().getResource(filePath));
        if (fileUrl == null) {
            throw new IOException("File not found: " + filePath);
        }
        File keystoreFile = new File(fileUrl.getFile());

        // Get admin access token, no matter it's master realm's admin
        OAuthClient.AccessTokenResponse accessTokenResponse = oauth.doGrantAccessTokenRequest(
                AuthRealm.MASTER, AuthRealm.ADMIN, AuthRealm.ADMIN, null, "admin-cli", null);
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
        String pem;

        // Assert the uploaded certificate
        if (!keystoreFormat.equals(CERTIFICATE_PEM)) {
            InputStream keystoreIs = new FileInputStream(keystoreFile);
            KeyStore keyStore = getKeystore(keystoreIs, storePassword, keystoreFormat);
            keystoreIs.close();
            pem = KeycloakModelUtils.getPemFromCertificate((X509Certificate) keyStore.getCertificate(keyAlias));
        }
        else {
            pem = new String(Files.readAllBytes(keystoreFile.toPath()));
        }

        assertCertificate(client, certOld, pem);
    }

    // TEST ERRORS

    @Test
    public void testMissingClientAssertionType() throws Exception {
        List<NameValuePair> parameters = new LinkedList<NameValuePair>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CLIENT_CREDENTIALS));

        HttpResponse resp = sendRequest(oauth.getServiceAccountUrl(), parameters);
        OAuthClient.AccessTokenResponse response = new OAuthClient.AccessTokenResponse(resp);

        assertError(response, null, "unauthorized_client", Errors.INVALID_CLIENT_CREDENTIALS);
    }

    @Test
    public void testInvalidClientAssertionType() throws Exception {
        List<NameValuePair> parameters = new LinkedList<NameValuePair>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CLIENT_CREDENTIALS));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, "invalid"));

        HttpResponse resp = sendRequest(oauth.getServiceAccountUrl(), parameters);
        OAuthClient.AccessTokenResponse response = new OAuthClient.AccessTokenResponse(resp);

        assertError(response, null, "unauthorized_client", Errors.INVALID_CLIENT_CREDENTIALS);
    }

    @Test
    public void testMissingClientAssertion() throws Exception {
        List<NameValuePair> parameters = new LinkedList<NameValuePair>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CLIENT_CREDENTIALS));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));

        HttpResponse resp = sendRequest(oauth.getServiceAccountUrl(), parameters);
        OAuthClient.AccessTokenResponse response = new OAuthClient.AccessTokenResponse(resp);

        assertError(response, null, "unauthorized_client", Errors.INVALID_CLIENT_CREDENTIALS);
    }

    @Test
    public void testAssertionMissingIssuer() throws Exception {
        String invalidJwt = getClientSignedJWT(getClient1PrivateKey(), null);

        List<NameValuePair> parameters = new LinkedList<NameValuePair>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CLIENT_CREDENTIALS));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, invalidJwt));

        HttpResponse resp = sendRequest(oauth.getServiceAccountUrl(), parameters);
        OAuthClient.AccessTokenResponse response = new OAuthClient.AccessTokenResponse(resp);

        assertError(response, null, "unauthorized_client", Errors.INVALID_CLIENT_CREDENTIALS);
    }

    @Test
    public void testAssertionUnknownClient() throws Exception {
        String invalidJwt = getClientSignedJWT(getClient1PrivateKey(), "unknown-client");

        List<NameValuePair> parameters = new LinkedList<NameValuePair>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CLIENT_CREDENTIALS));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, invalidJwt));

        HttpResponse resp = sendRequest(oauth.getServiceAccountUrl(), parameters);
        OAuthClient.AccessTokenResponse response = new OAuthClient.AccessTokenResponse(resp);

        assertError(response, "unknown-client", "unauthorized_client", Errors.INVALID_CLIENT_CREDENTIALS);
    }

    @Test
    public void testAssertionDisabledClient() throws Exception {

        ClientManager.realm(adminClient.realm("test")).clientId("client1").enabled(false);

        String invalidJwt = getClient1SignedJWT();

        List<NameValuePair> parameters = new LinkedList<NameValuePair>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CLIENT_CREDENTIALS));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, invalidJwt));

        HttpResponse resp = sendRequest(oauth.getServiceAccountUrl(), parameters);
        OAuthClient.AccessTokenResponse response = new OAuthClient.AccessTokenResponse(resp);

        assertError(response, "client1", "invalid_client", Errors.CLIENT_DISABLED);

        ClientManager.realm(adminClient.realm("test")).clientId("client1").enabled(false);
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

        HttpResponse resp = sendRequest(oauth.getServiceAccountUrl(), parameters);
        OAuthClient.AccessTokenResponse response = new OAuthClient.AccessTokenResponse(resp);

        assertError(response, "client1", "unauthorized_client", "client_credentials_setup_required");

        ClientManager.realm(adminClient.realm("test")).clientId("client1").updateAttribute(JWTClientAuthenticator.CERTIFICATE_ATTR, backupClient1Cert.certificate);
    }

    @Test
    public void testAssertionInvalidSignature() throws Exception {
        // JWT for client1, but signed by privateKey of client2
        String invalidJwt = getClientSignedJWT(getClient2PrivateKey(), "client1");

        List<NameValuePair> parameters = new LinkedList<NameValuePair>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CLIENT_CREDENTIALS));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, invalidJwt));

        HttpResponse resp = sendRequest(oauth.getServiceAccountUrl(), parameters);
        OAuthClient.AccessTokenResponse response = new OAuthClient.AccessTokenResponse(resp);

        assertError(response, "client1", "unauthorized_client", Errors.INVALID_CLIENT_CREDENTIALS);
    }


    @Test
    public void testAssertionExpired() throws Exception {
        String invalidJwt = getClient1SignedJWT();

        setTimeOffset(1000);

        List<NameValuePair> parameters = new LinkedList<NameValuePair>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CLIENT_CREDENTIALS));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, invalidJwt));

        HttpResponse resp = sendRequest(oauth.getServiceAccountUrl(), parameters);
        OAuthClient.AccessTokenResponse response = new OAuthClient.AccessTokenResponse(resp);

        setTimeOffset(0);

        assertError(response, "client1", "unauthorized_client", Errors.INVALID_CLIENT_CREDENTIALS);
    }

    @Test
    public void testAssertionInvalidNotBefore() throws Exception {
        String invalidJwt = getClient1SignedJWT();

        setTimeOffset(-1000);

        List<NameValuePair> parameters = new LinkedList<NameValuePair>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CLIENT_CREDENTIALS));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, invalidJwt));

        HttpResponse resp = sendRequest(oauth.getServiceAccountUrl(), parameters);
        OAuthClient.AccessTokenResponse response = new OAuthClient.AccessTokenResponse(resp);

        setTimeOffset(0);

        assertError(response, "client1", "unauthorized_client", Errors.INVALID_CLIENT_CREDENTIALS);
    }

    @Test
    public void testMissingIdClaim() throws Exception {
        OAuthClient.AccessTokenResponse response = testMissingClaim("id");
        assertSuccess(response, app1.getClientId(), serviceAccountUser.getId(), serviceAccountUser.getUsername());
    }

    @Test
    public void testMissingIssuerClaim() throws Exception {
        OAuthClient.AccessTokenResponse response = testMissingClaim("issuer");
        assertSuccess(response, app1.getClientId(), serviceAccountUser.getId(), serviceAccountUser.getUsername());
    }

    @Test
    public void testMissingSubjectClaim() throws Exception {
        OAuthClient.AccessTokenResponse response = testMissingClaim("subject");
        assertError(response, null, "unauthorized_client", Errors.INVALID_CLIENT_CREDENTIALS);
    }

    @Test
    public void testMissingAudienceClaim() throws Exception {
        OAuthClient.AccessTokenResponse response = testMissingClaim("audience");
        assertError(response, app1.getClientId(), "unauthorized_client", Errors.INVALID_CLIENT_CREDENTIALS);
    }

    @Test
    public void testMissingIssuedAtClaim() throws Exception {
        OAuthClient.AccessTokenResponse response = testMissingClaim("issuedAt");
        assertSuccess(response, app1.getClientId(), serviceAccountUser.getId(), serviceAccountUser.getUsername());
    }

    @Test
    // KEYCLOAK-2986
    public void testMissingExpirationClaim() throws Exception {
        // Missing only exp; the lifespan should be calculated from issuedAt
        OAuthClient.AccessTokenResponse response = testMissingClaim("expiration");
        assertSuccess(response, app1.getClientId(), serviceAccountUser.getId(), serviceAccountUser.getUsername());

        // Test expired lifespan
        response = testMissingClaim(-11, "expiration");
        assertError(response, app1.getClientId(), "unauthorized_client", Errors.INVALID_CLIENT_CREDENTIALS);

        // Missing exp and issuedAt should return error
        response = testMissingClaim("expiration", "issuedAt");
        assertError(response, app1.getClientId(), "unauthorized_client", Errors.INVALID_CLIENT_CREDENTIALS);
    }

    @Test
    public void testMissingNotBeforeClaim() throws Exception {
        OAuthClient.AccessTokenResponse response = testMissingClaim("notBefore");
        assertSuccess(response, app1.getClientId(), serviceAccountUser.getId(), serviceAccountUser.getUsername());
    }

    private OAuthClient.AccessTokenResponse testMissingClaim(String... claims) throws Exception {
        return testMissingClaim(0, claims);
    }

    private OAuthClient.AccessTokenResponse testMissingClaim(int tokenTimeOffset, String... claims) throws Exception {
        CustomJWTClientCredentialsProvider jwtProvider = new CustomJWTClientCredentialsProvider();
        jwtProvider.setPrivateKey(getClient1PrivateKey());
        jwtProvider.setTokenTimeout(10);

        for (String claim : claims) {
            jwtProvider.enableClaim(claim, false);
        }

        Time.setOffset(tokenTimeOffset);
        String jwt = jwtProvider.createSignedRequestToken(app1.getClientId(), getRealmInfoUrl());
        Time.setOffset(0);
        return doClientCredentialsGrantRequest(jwt);
    }

    private void assertError(OAuthClient.AccessTokenResponse response, String clientId, String responseError, String eventError) {
        assertEquals(400, response.getStatusCode());
        assertEquals(responseError, response.getError());

        events.expectClientLogin()
                .client(clientId)
                .session((String) null)
                .clearDetails()
                .error(eventError)
                .user((String) null)
                .assertEvent();
    }

    private void assertSuccess(OAuthClient.AccessTokenResponse response, String clientId, String userId, String userName) {
        assertEquals(200, response.getStatusCode());

        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        RefreshToken refreshToken = oauth.verifyRefreshToken(response.getRefreshToken());

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

    private static void assertCertificate(ClientRepresentation client, String certOld, String pem) {
        final String certNew = client.getAttributes().get(JWTClientAuthenticator.CERTIFICATE_ATTR);
        assertNotEquals("The old and new certificates shouldn't match", certOld, certNew);
        assertEquals("Certificates don't match", pem, certNew);
    }

    // HELPER METHODS

    private OAuthClient.AccessTokenResponse doAccessTokenRequest(String code, String signedJwt) throws Exception {
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.AUTHORIZATION_CODE));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CODE, code));
        parameters.add(new BasicNameValuePair(OAuth2Constants.REDIRECT_URI, oauth.getRedirectUri()));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, signedJwt));

        HttpResponse response = sendRequest(oauth.getAccessTokenUrl(), parameters);
        return new OAuthClient.AccessTokenResponse(response);
    }

    private OAuthClient.AccessTokenResponse doRefreshTokenRequest(String refreshToken, String signedJwt) throws Exception {
        List<NameValuePair> parameters = new LinkedList<NameValuePair>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.REFRESH_TOKEN));
        parameters.add(new BasicNameValuePair(OAuth2Constants.REFRESH_TOKEN, refreshToken));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, signedJwt));

        HttpResponse response = sendRequest(oauth.getRefreshTokenUrl(), parameters);
        return new OAuthClient.AccessTokenResponse(response);
    }

    private HttpResponse doLogout(String refreshToken, String signedJwt) throws Exception {
        List<NameValuePair> parameters = new LinkedList<NameValuePair>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.REFRESH_TOKEN));
        parameters.add(new BasicNameValuePair(OAuth2Constants.REFRESH_TOKEN, refreshToken));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, signedJwt));

        return sendRequest(oauth.getLogoutUrl(null, null), parameters);
    }

    private OAuthClient.AccessTokenResponse doClientCredentialsGrantRequest(String signedJwt) throws Exception {
        List<NameValuePair> parameters = new LinkedList<NameValuePair>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CLIENT_CREDENTIALS));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, signedJwt));

        HttpResponse response = sendRequest(oauth.getServiceAccountUrl(), parameters);
        return new OAuthClient.AccessTokenResponse(response);
    }

    private OAuthClient.AccessTokenResponse doGrantAccessTokenRequest(String username, String password, String signedJwt) throws Exception {
        List<NameValuePair> parameters = new LinkedList<NameValuePair>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD));
        parameters.add(new BasicNameValuePair("username", username));
        parameters.add(new BasicNameValuePair("password", password));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, signedJwt));

        HttpResponse response = sendRequest(oauth.getResourceOwnerPasswordCredentialGrantUrl(), parameters);
        return new OAuthClient.AccessTokenResponse(response);
    }

    private HttpResponse sendRequest(String requestUrl, List<NameValuePair> parameters) throws Exception {
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

    private String getClient1SignedJWT() {
        return getClientSignedJWT(getClient1PrivateKey(), "client1");
    }

    private String getClient2SignedJWT() {
        return getClientSignedJWT(getClient2PrivateKey(), "client2");
    }

    private PrivateKey getClient1PrivateKey() {
        return KeystoreUtil.loadPrivateKeyFromKeystore("classpath:client-auth-test/keystore-client1.jks",
                "storepass", "keypass", "clientkey", KeystoreUtil.KeystoreFormat.JKS);
    }

    private PrivateKey getClient2PrivateKey() {
        return KeystoreUtil.loadPrivateKeyFromKeystore("classpath:client-auth-test/keystore-client2.jks",
                "storepass", "keypass", "clientkey", KeystoreUtil.KeystoreFormat.JKS);
    }

    private String getClientSignedJWT(PrivateKey privateKey, String clientId) {
        JWTClientCredentialsProvider jwtProvider = new JWTClientCredentialsProvider();
        jwtProvider.setPrivateKey(privateKey);
        jwtProvider.setTokenTimeout(10);
        return jwtProvider.createSignedRequestToken(clientId, getRealmInfoUrl());
    }

    private String getRealmInfoUrl() {
        String authServerBaseUrl = UriUtils.getOrigin(oauth.getRedirectUri()) + "/auth";
        return KeycloakUriBuilder.fromUri(authServerBaseUrl).path(ServiceUrlConstants.REALM_INFO_PATH).build("test").toString();
    }

    private ClientAttributeCertificateResource getClientAttributeCertificateResource(String realm, String clientId) {
        return getClient(realm, clientId).getCertficateResource("jwt.credential");
    }

    private ClientResource getClient(String realm, String clientId) {
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
            if (isClaimEnabled("id")) reqToken.id(AdapterUtils.generateId());
            if (isClaimEnabled("issuer")) reqToken.issuer(clientId);
            if (isClaimEnabled("subject")) reqToken.subject(clientId);
            if (isClaimEnabled("audience")) reqToken.audience(realmInfoUrl);

            int now = Time.currentTime();
            if (isClaimEnabled("issuedAt")) reqToken.issuedAt(now);
            if (isClaimEnabled("expiration")) reqToken.expiration(now + getTokenTimeout());
            if (isClaimEnabled("notBefore")) reqToken.notBefore(now);

            return reqToken;
        }
    }

    private static KeyStore getKeystore(InputStream is, String storePassword, String format) throws Exception {
        KeyStore keyStore = format.equals("JKS") ? KeyStore.getInstance(format) : KeyStore.getInstance(format, "BC");
        keyStore.load(is, storePassword.toCharArray());
        return keyStore;
    }
}
