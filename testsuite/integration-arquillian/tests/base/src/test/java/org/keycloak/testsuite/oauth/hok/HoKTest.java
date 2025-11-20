package org.keycloak.testsuite.oauth.hok;

import java.io.IOException;
import java.net.URI;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.jose.jws.crypto.HashUtils;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.oidc.TokenMetadataRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.client.KeycloakTestingClient;
import org.keycloak.testsuite.drone.Different;
import org.keycloak.testsuite.oauth.RefreshTokenTest;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.KeycloakModelUtils;
import org.keycloak.testsuite.util.MutualTLSUtils;
import org.keycloak.testsuite.util.UserInfoClientUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.LogoutResponse;
import org.keycloak.testsuite.util.oauth.OAuthClient;

import org.apache.http.impl.client.CloseableHttpClient;
import org.hamcrest.Matchers;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.WebDriver;

import static org.keycloak.testsuite.admin.ApiUtil.findUserByUsername;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_SSL_REQUIRED;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class HoKTest extends AbstractTestRealmKeycloakTest {
    // KEYCLOAK-6771 Certificate Bound Token
    // https://tools.ietf.org/html/draft-ietf-oauth-mtls-08#section-3

    @Drone
    @Different
    protected WebDriver driver2;

    private static final List<String> CLIENT_LIST = Arrays.asList("test-app", "named-test-app", "service-account-client");

    public static class HoKAssertEvents extends AssertEvents {

        public HoKAssertEvents(AbstractKeycloakTest ctx) {
            super(ctx);
        }

        private final String defaultRedirectUri = "https://localhost:8543/auth/realms/master/app/auth";

        @Override
        public ExpectedEvent expectLogin() {
            return expect(EventType.LOGIN)
                    .detail(Details.CODE_ID, isCodeId())
                    //.detail(Details.USERNAME, DEFAULT_USERNAME)
                    //.detail(Details.AUTH_METHOD, OIDCLoginProtocol.LOGIN_PROTOCOL)
                    //.detail(Details.AUTH_TYPE, AuthorizationEndpoint.CODE_AUTH_TYPE)
                    .detail(Details.REDIRECT_URI, defaultRedirectUri)
                    .detail(Details.CONSENT, Details.CONSENT_VALUE_NO_CONSENT_REQUIRED)
                    .session(isSessionId());
        }
    }

    @Rule
    public HoKAssertEvents events = new HoKAssertEvents(this);

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        // override due to effects caused by enabling TLS
        for (String clientId : CLIENT_LIST) addRedirectUrlForTls(testRealm, clientId);

        // for token introspection
        configTestRealmForTokenIntrospection(testRealm);
    }

    @BeforeClass
    public static void checkIfTLSIsTurnedOn() {
        Assume.assumeTrue(AUTH_SERVER_SSL_REQUIRED);
    }

    private void addRedirectUrlForTls(RealmRepresentation testRealm, String clientId) {
        for (ClientRepresentation client : testRealm.getClients()) {
            if (client.getClientId().equals(clientId)) {
                URI baseUri = URI.create(client.getRedirectUris().get(0));
                URI redir = URI.create("https://localhost:" + System.getProperty("auth.server.https.port", "8543") + baseUri.getRawPath());
                client.getRedirectUris().add(redir.toString());
                break;
            }
        }
    }
    
    private void configTestRealmForTokenIntrospection(RealmRepresentation testRealm) {
        ClientRepresentation confApp = KeycloakModelUtils.createClient(testRealm, "confidential-cli");
        confApp.setSecret("secret1");
        confApp.setServiceAccountsEnabled(Boolean.TRUE);

        ClientRepresentation serviceAccountApp = KeycloakModelUtils.createClient(testRealm, "service-account-client");
        serviceAccountApp.setSecret("secret1");
        serviceAccountApp.setServiceAccountsEnabled(Boolean.TRUE);
        serviceAccountApp.setDirectAccessGrantsEnabled(Boolean.TRUE);

        ClientRepresentation pubApp = KeycloakModelUtils.createClient(testRealm, "public-cli");
        pubApp.setPublicClient(Boolean.TRUE);

        UserRepresentation user = new UserRepresentation();
        user.setUsername("no-permissions");
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType("password");
        credential.setValue("password");
        List<CredentialRepresentation> creds = new ArrayList<>();
        creds.add(credential);
        user.setCredentials(creds);
        user.setEnabled(Boolean.TRUE);
        List<String> realmRoles = new ArrayList<>();
        realmRoles.add("user");
        user.setRealmRoles(realmRoles);
        testRealm.getUsers().add(user);
    }

    // enable HoK Token as default
    @Before
    public void enableHoKToken() {
        // Enable MTLS HoK Token
        for (String clientId : CLIENT_LIST) enableHoKToken(clientId);
    }

    private void enableHoKToken(String clientId) {
        // Enable MTLS HoK Token
        ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm("test"), clientId);
        ClientRepresentation clientRep = clientResource.toRepresentation();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUseMtlsHoKToken(true);
        clientResource.update(clientRep);
    }
    
    // Authorization Code Flow 
    // Bind HoK Token

    @Test
    public void accessTokenRequestWithClientCertificate() throws Exception {
        oauth.doLogin("test-user@localhost", "password");
        EventRepresentation loginEvent = events.expectLogin().assertEvent();
        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);
        String code = oauth.parseLoginResponse().getCode();

        AccessTokenResponse response;
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore()) {
            oauth.httpClient().set(client);
            response = oauth.doAccessTokenRequest(code);
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            oauth.httpClient().reset();
        }

        // Success Pattern
        expectSuccessfulResponseFromTokenEndpoint(sessionId, codeId, response);
        verifyHoKTokenDefaultCertThumbPrint(response);
    }
    
    @Test
    public void accessTokenRequestWithoutClientCertificate() throws Exception {
        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.parseLoginResponse().getCode();

        AccessTokenResponse response;
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithoutKeyStoreAndTrustStore()) {
            oauth.httpClient().set(client);
            response = oauth.doAccessTokenRequest(code);
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            oauth.httpClient().reset();
        }

        // Error Pattern
        assertEquals(400, response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
        assertEquals("Client Certification missing for MTLS HoK Token Binding", response.getErrorDescription());
    }

    private void expectSuccessfulResponseFromTokenEndpoint(String sessionId, String codeId, AccessTokenResponse response) throws Exception {
        assertEquals(200, response.getStatusCode());

        assertThat(response.getExpiresIn(), allOf(greaterThanOrEqualTo(250), lessThanOrEqualTo(300)));
        assertThat(response.getRefreshExpiresIn(), allOf(greaterThanOrEqualTo(1750), lessThanOrEqualTo(1800)));

        assertEquals("Bearer", response.getTokenType());

        String expectedKid = Stream.of(oauth.keys().getRealmKeys().getKeys())
                .filter(jwk -> KeyUse.SIG.getSpecName().equals(jwk.getPublicKeyUse()))
                .map(JWK::getKeyId)
                .findFirst().orElseThrow(() -> new AssertionError("Was not able to find key with usage SIG in the 'test' realm keys"));

        JWSHeader header = new JWSInput(response.getAccessToken()).getHeader();
        assertEquals("RS256", header.getAlgorithm().name());
        assertEquals("JWT", header.getType());
        assertEquals(expectedKid, header.getKeyId());
        assertNull(header.getContentType());

        header = new JWSInput(response.getIdToken()).getHeader();
        assertEquals("RS256", header.getAlgorithm().name());
        assertEquals("JWT", header.getType());
        assertEquals(expectedKid, header.getKeyId());
        assertNull(header.getContentType());

        header = new JWSInput(response.getRefreshToken()).getHeader();
        assertEquals(Constants.INTERNAL_SIGNATURE_ALGORITHM, header.getAlgorithm().name());
        assertEquals("JWT", header.getType());
        assertNull(header.getContentType());

        AccessToken token = oauth.verifyToken(response.getAccessToken());

        assertEquals(findUserByUsername(adminClient.realm("test"), "test-user@localhost").getId(), token.getSubject());
        // The following check is not valid anymore since file store does have the same ID, and is redundant due to the previous line
        // Assert.assertNotEquals("test-user@localhost", token.getSubject());

        assertEquals(sessionId, token.getSessionState());

        assertEquals(2, token.getRealmAccess().getRoles().size());
        assertTrue(token.getRealmAccess().isUserInRole("user"));

        assertEquals(1, token.getResourceAccess(oauth.getClientId()).getRoles().size());
        assertTrue(token.getResourceAccess(oauth.getClientId()).isUserInRole("customer-user"));

        EventRepresentation event = events.expectCodeToToken(codeId, sessionId).assertEvent();
        assertEquals(token.getId(), event.getDetails().get(Details.TOKEN_ID));
        assertEquals(oauth.parseRefreshToken(response.getRefreshToken()).getId(), event.getDetails().get(Details.REFRESH_TOKEN_ID));
        assertEquals(sessionId, token.getSessionState());
    }

    // verify HoK Token - Token Refresh

    @Test
    public void refreshTokenRequestByHoKRefreshTokenByOtherClient() throws Exception {
        // first client user login
        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse tokenResponse = null;
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore()) {
            oauth.httpClient().set(client);
            tokenResponse = oauth.doAccessTokenRequest(code);
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            oauth.httpClient().reset();
        }
        verifyHoKTokenDefaultCertThumbPrint(tokenResponse);
        String refreshTokenString = tokenResponse.getRefreshToken();

        // second client user login
        OAuthClient oauth2 = oauth.newConfig().driver(driver2);
        oauth2.doLogin("john-doh@localhost", "password");
        String code2 = oauth2.parseLoginResponse().getCode();
        AccessTokenResponse tokenResponse2 = null;
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithOtherKeyStoreAndTrustStore()) {
            oauth2.httpClient().set(client);
            tokenResponse2 = oauth2.doAccessTokenRequest(code2);
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            oauth2.httpClient().reset();
        }
        verifyHoKTokenOtherCertThumbPrint(tokenResponse2);

        // token refresh by second client by first client's refresh token
        AccessTokenResponse response = null;
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithOtherKeyStoreAndTrustStore()) {
            oauth2.httpClient().set(client);
            response = oauth2.doRefreshTokenRequest(refreshTokenString);
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            oauth2.httpClient().reset();
        }

        // Error Pattern
        assertEquals(401, response.getStatusCode());
        assertEquals(OAuthErrorException.UNAUTHORIZED_CLIENT, response.getError());
        assertEquals("Client certificate missing, or its thumbprint and one in the refresh token did NOT match", response.getErrorDescription());
    }

    @Test
    public void refreshTokenRequestByHoKRefreshTokenWithClientCertificate() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();
        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);
        String code = oauth.parseLoginResponse().getCode();

        AccessTokenResponse tokenResponse = null;
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore()) {
            oauth.httpClient().set(client);
            tokenResponse = oauth.doAccessTokenRequest(code);
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            oauth.httpClient().reset();
        }

        verifyHoKTokenDefaultCertThumbPrint(tokenResponse);
        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
        String refreshTokenString = tokenResponse.getRefreshToken();
        RefreshToken refreshToken = oauth.parseRefreshToken(refreshTokenString);
        EventRepresentation tokenEvent = events.expectCodeToToken(codeId, sessionId).assertEvent();

        Assert.assertNotNull(refreshTokenString);
        assertEquals("Bearer", tokenResponse.getTokenType());
        assertThat(token.getExp() - getCurrentTime(), allOf(greaterThanOrEqualTo(200L), lessThanOrEqualTo(350L)));
        long actual = refreshToken.getExp() - getCurrentTime();
        assertThat(actual, allOf(greaterThanOrEqualTo(1799L - RefreshTokenTest.ALLOWED_CLOCK_SKEW), lessThanOrEqualTo(1800L + RefreshTokenTest.ALLOWED_CLOCK_SKEW)));
        assertEquals(sessionId, refreshToken.getSessionState());

        setTimeOffset(2);

        AccessTokenResponse response = null;
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore()) {
            oauth.httpClient().set(client);
            response = oauth.doRefreshTokenRequest(refreshTokenString);
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            oauth.httpClient().reset();
        }

        // Success Pattern
        expectSuccessfulResponseFromTokenEndpoint(response, sessionId, token, refreshToken, tokenEvent);
        verifyHoKTokenDefaultCertThumbPrint(response);
    }

    @Test
    public void refreshTokenRequestByRefreshTokenWithoutClientCertificate() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();
        String sessionId = loginEvent.getSessionId();
        String code = oauth.parseLoginResponse().getCode();

        AccessTokenResponse tokenResponse = null;
        tokenResponse = oauth.doAccessTokenRequest(code);

        verifyHoKTokenDefaultCertThumbPrint(tokenResponse);
        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
        String refreshTokenString = tokenResponse.getRefreshToken();
        RefreshToken refreshToken = oauth.parseRefreshToken(refreshTokenString);

        Assert.assertNotNull(refreshTokenString);
        assertEquals("Bearer", tokenResponse.getTokenType());
        assertThat(token.getExp() - getCurrentTime(), allOf(greaterThanOrEqualTo(200L), lessThanOrEqualTo(350L)));
        long actual = refreshToken.getExp() - getCurrentTime();
        assertThat(actual, allOf(greaterThanOrEqualTo(1799L - RefreshTokenTest.ALLOWED_CLOCK_SKEW), lessThanOrEqualTo(1800L + RefreshTokenTest.ALLOWED_CLOCK_SKEW)));
        assertEquals(sessionId, refreshToken.getSessionState());

        setTimeOffset(2);

        AccessTokenResponse response = null;
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithoutKeyStoreAndTrustStore()) {
            oauth.httpClient().set(client);
            response = oauth.doRefreshTokenRequest(refreshTokenString);
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            oauth.httpClient().reset();
        }

        // Error Pattern
        assertEquals(401, response.getStatusCode());
        assertEquals(OAuthErrorException.UNAUTHORIZED_CLIENT, response.getError());
        assertEquals("Client certificate missing, or its thumbprint and one in the refresh token did NOT match", response.getErrorDescription());
    }

    private void expectSuccessfulResponseFromTokenEndpoint(AccessTokenResponse response, String sessionId, AccessToken token, RefreshToken refreshToken, EventRepresentation tokenEvent) {
        expectSuccessfulResponseFromTokenEndpoint(oauth, "test-user@localhost", response, sessionId, token, refreshToken, tokenEvent);
    }
    
    private void expectSuccessfulResponseFromTokenEndpoint(OAuthClient oauth, String username, AccessTokenResponse response, String sessionId, AccessToken token, RefreshToken refreshToken, EventRepresentation tokenEvent) {
        AccessToken refreshedToken = oauth.verifyToken(response.getAccessToken());
        RefreshToken refreshedRefreshToken = oauth.parseRefreshToken(response.getRefreshToken());
        if (refreshedToken.getConfirmation() != null) {
            log.warnf("refreshed access token's cnf-x5t#256 = %s", refreshedToken.getConfirmation().getCertThumbprint());
            log.warnf("refreshed refresh token's cnf-x5t#256 = %s", refreshedRefreshToken.getConfirmation().getCertThumbprint());
        }

        assertEquals(200, response.getStatusCode());

        assertEquals(sessionId, refreshedToken.getSessionState());
        assertEquals(sessionId, refreshedRefreshToken.getSessionState());

        assertThat(response.getExpiresIn(), allOf(greaterThanOrEqualTo(250), lessThanOrEqualTo(300)));
        assertThat(refreshedToken.getExp() - getCurrentTime(), allOf(greaterThanOrEqualTo(250L - RefreshTokenTest.ALLOWED_CLOCK_SKEW), lessThanOrEqualTo(300L + RefreshTokenTest.ALLOWED_CLOCK_SKEW)));

        assertThat(refreshedToken.getExp() - token.getExp(), allOf(greaterThanOrEqualTo(1L), lessThanOrEqualTo(10L)));
        assertThat(refreshedRefreshToken.getExp() - refreshToken.getExp(), allOf(greaterThanOrEqualTo(1L), lessThanOrEqualTo(10L)));

        Assert.assertNotEquals(token.getId(), refreshedToken.getId());
        Assert.assertNotEquals(refreshToken.getId(), refreshedRefreshToken.getId());

        assertEquals("Bearer", response.getTokenType());

        assertEquals(findUserByUsername(adminClient.realm("test"), username).getId(), refreshedToken.getSubject());
        // The following check is not valid anymore since file store does have the same ID, and is redundant due to the previous line
        // Assert.assertNotEquals("test-user@localhost", token.getSubject());

        assertEquals(2, refreshedToken.getRealmAccess().getRoles().size());
        Assert.assertTrue(refreshedToken.getRealmAccess().isUserInRole("user"));

        assertEquals(1, refreshedToken.getResourceAccess(oauth.getClientId()).getRoles().size());
        Assert.assertTrue(refreshedToken.getResourceAccess(oauth.getClientId()).isUserInRole("customer-user"));

        EventRepresentation refreshEvent = events.expectRefresh(tokenEvent.getDetails().get(Details.REFRESH_TOKEN_ID), sessionId).user(refreshToken.getSubject()).assertEvent();
        Assert.assertNotEquals(tokenEvent.getDetails().get(Details.TOKEN_ID), refreshEvent.getDetails().get(Details.TOKEN_ID));
        Assert.assertNotEquals(tokenEvent.getDetails().get(Details.REFRESH_TOKEN_ID), refreshEvent.getDetails().get(Details.UPDATED_REFRESH_TOKEN_ID));

        setTimeOffset(0);
    }

    // verify HoK Token - Get UserInfo

    @Test
    public void getUserInfoByHoKAccessTokenWithClientCertificate() throws Exception {
        // get an access token
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();
        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);
        String code = oauth.parseLoginResponse().getCode();

        AccessTokenResponse tokenResponse = null;
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore()) {
            oauth.httpClient().set(client);
            tokenResponse = oauth.doAccessTokenRequest(code);
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            oauth.httpClient().reset();
        }
        verifyHoKTokenDefaultCertThumbPrint(tokenResponse);
        events.expectCodeToToken(codeId, sessionId).assertEvent();

        // execute the access token to get UserInfo with token binded client certificate in mutual authentication TLS
        ClientBuilder clientBuilder = ClientBuilder.newBuilder();
        KeyStore keystore = null;
        keystore = KeystoreUtil.loadKeyStore(MutualTLSUtils.DEFAULT_KEYSTOREPATH, MutualTLSUtils.DEFAULT_KEYSTOREPASSWORD);
        clientBuilder.keyStore(keystore, MutualTLSUtils.DEFAULT_KEYSTOREPASSWORD);
        Client client = clientBuilder.build();
        WebTarget userInfoTarget = null;
        Response response = null;
        try {
            userInfoTarget = UserInfoClientUtil.getUserInfoWebTarget(client);
            response = userInfoTarget.request().header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenResponse.getAccessToken()).get();
            testSuccessfulUserInfoResponse(response);
        } finally {
            response.close();
            client.close();
        }

    }

    @Test
    public void getUserInfoByHoKAccessTokenWithoutClientCertificate() throws Exception {
        // get an access token
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();
        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);
        String code = oauth.parseLoginResponse().getCode();

        AccessTokenResponse tokenResponse = null;
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore()) {
            oauth.httpClient().set(client);
            tokenResponse = oauth.doAccessTokenRequest(code);
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            oauth.httpClient().reset();
        }
        verifyHoKTokenDefaultCertThumbPrint(tokenResponse);
        events.expectCodeToToken(codeId, sessionId).assertEvent();

        // execute the access token to get UserInfo without token binded client certificate in mutual authentication TLS
        Client client = KeycloakTestingClient.getRestEasyClientBuilder().build();
        WebTarget userInfoTarget = null;
        Response response = null;
        try {
            userInfoTarget = UserInfoClientUtil.getUserInfoWebTarget(client);
            response = userInfoTarget.request().header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenResponse.getAccessToken()).get();
            assertEquals(401, response.getStatus());
        } finally {
            if (response != null) response.close();
            client.close();
        }

    }

    private void testSuccessfulUserInfoResponse(Response response) {
        events.expect(EventType.USER_INFO_REQUEST)
                .session(Matchers.notNullValue(String.class))
                .detail(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN)
                .detail(Details.USERNAME, "test-user@localhost")
                .detail(Details.SIGNATURE_REQUIRED, "false")
                .assertEvent();
        UserInfoClientUtil.testSuccessfulUserInfoResponse(response, "test-user@localhost", "test-user@localhost");
    }

    // verify HoK Token - Back Channel Logout

    @Test
    public void postLogoutByHoKRefreshTokenWithClientCertificate() throws Exception {
        String refreshTokenString = execPreProcessPostLogout();

        LogoutResponse response = null;
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore()) {
            oauth.httpClient().set(client);
            response = oauth.doLogout(refreshTokenString);
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            oauth.httpClient().reset();
        }

        // Success Pattern
        assertTrue(response.isSuccess());
        assertNotNull(testingClient.testApp().getAdminLogoutAction());
    }

    @Test
    public void postLogoutByHoKRefreshTokenWithoutClientCertificate() throws Exception {
        String refreshTokenString = execPreProcessPostLogout();

        LogoutResponse response = null;
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithoutKeyStoreAndTrustStore()) {
            oauth.httpClient().set(client);
            response = oauth.doLogout(refreshTokenString);
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            oauth.httpClient().reset();
        }
        // Error Pattern
        assertEquals(401, response.getStatusCode());
    }

    private String execPreProcessPostLogout() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        verifyHoKTokenDefaultCertThumbPrint(tokenResponse);

        return tokenResponse.getRefreshToken();
    }

    // Hybrid Code Flow : response_type = code id_token
    // Bind HoK Token
    
    @Test
    public void accessTokenRequestWithClientCertificateInHybridFlowWithCodeIDToken() throws Exception {
        String nonce = "ckw938gnspa93dj";
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").standardFlow(true).implicitFlow(true);
        oauth.client("test-app", "password");
        oauth.responseType(OIDCResponseType.CODE + " " + OIDCResponseType.ID_TOKEN);

        oauth.loginForm().nonce(nonce).doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();
        AuthorizationEndpointResponse authzResponse = oauth.parseLoginResponse();
        Assert.assertNotNull(authzResponse.getSessionState());
        List<IDToken> idTokens = testAuthzResponseAndRetrieveIDTokens(authzResponse, loginEvent);
        for (IDToken idToken : idTokens) {
            Assert.assertEquals(nonce, idToken.getNonce());
            Assert.assertEquals(authzResponse.getSessionState(), idToken.getSessionState());
        }
    }

    protected List<IDToken> testAuthzResponseAndRetrieveIDTokens(AuthorizationEndpointResponse authzResponse, EventRepresentation loginEvent) {
        Assert.assertEquals(OIDCResponseType.CODE + " " + OIDCResponseType.ID_TOKEN, loginEvent.getDetails().get(Details.RESPONSE_TYPE));

        // IDToken from the authorization response
        Assert.assertNull(authzResponse.getAccessToken());
        String idTokenStr = authzResponse.getIdToken();
        IDToken idToken = oauth.verifyIDToken(idTokenStr);

        // Validate "c_hash"
        Assert.assertNull(idToken.getAccessTokenHash());
        Assert.assertNotNull(idToken.getCodeHash());
        Assert.assertEquals(idToken.getCodeHash(), HashUtils.accessTokenHash(Algorithm.RS256, authzResponse.getCode()));

        // IDToken exchanged for the code
        IDToken idToken2 = sendTokenRequestAndGetIDToken(loginEvent);

        return Arrays.asList(idToken, idToken2);
    }

    @Test
    public void testIntrospectHoKAccessToken() throws Exception {
        // get an access token with client certificate in mutual authenticate TLS
        // mimic Client
        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.parseLoginResponse().getCode();
        EventRepresentation loginEvent = events.expectLogin().assertEvent();
        AccessTokenResponse accessTokenResponse = null;
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore()) {
            oauth.httpClient().set(client);
            accessTokenResponse = oauth.doAccessTokenRequest(code);
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            oauth.httpClient().reset();
        }

        // Do token introspection
        // mimic Resource Server
        TokenMetadataRepresentation rep;
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithoutKeyStoreAndTrustStore()) {
            oauth.client("confidential-cli", "secret1").httpClient().set(client);
            rep = oauth.doIntrospectionRequest(accessTokenResponse.getAccessToken(), "access_token").asTokenMetadata();
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            oauth.httpClient().reset();
        }
        JWSInput jws = new JWSInput(accessTokenResponse.getAccessToken());
        AccessToken at = jws.readJsonContent(AccessToken.class);
        jws = new JWSInput(accessTokenResponse.getRefreshToken());
        RefreshToken rt = jws.readJsonContent(RefreshToken.class);
        String certThumprintFromAccessToken = at.getConfirmation().getCertThumbprint();
        String certThumprintFromRefreshToken = rt.getConfirmation().getCertThumbprint();
        String certThumprintFromTokenIntrospection = rep.getConfirmation().getCertThumbprint();
        String certThumprintFromBoundClientCertificate = MutualTLSUtils.getThumbprintFromDefaultClientCert();

        assertTrue(rep.isActive());
        assertEquals("test-user@localhost", rep.getUserName());
        assertEquals("test-app", rep.getClientId());
        assertEquals(loginEvent.getUserId(), rep.getSubject());

        assertEquals(certThumprintFromTokenIntrospection, certThumprintFromBoundClientCertificate);
        assertEquals(certThumprintFromBoundClientCertificate, certThumprintFromAccessToken);
        assertEquals(certThumprintFromAccessToken, certThumprintFromRefreshToken);

    }

    @Test
    public void serviceAccountWithClientCertificate() throws Exception {
        oauth.client("service-account-client", "secret1");

        AccessTokenResponse response;

        try {
            // Request without HoK should fail
            try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithoutKeyStoreAndTrustStore()) {
                oauth.httpClient().set(client);
                response = oauth.doClientCredentialsGrantAccessTokenRequest();
                assertEquals(400, response.getStatusCode());
                assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
                assertEquals("Client Certification missing for MTLS HoK Token Binding", response.getErrorDescription());
            } finally {
                oauth.httpClient().reset();
            }

            // Request with HoK - success
            try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore()) {
                oauth.httpClient().set(client);
                response = oauth.doClientCredentialsGrantAccessTokenRequest();
                assertEquals(200, response.getStatusCode());

                // Success Pattern
                verifyHoKTokenCertThumbPrint(response, MutualTLSUtils.getThumbprintFromDefaultClientCert(), false);
            } finally {
                oauth.httpClient().reset();
            }
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @Test
    public void resourceOwnerPasswordCredentialsGrantWithClientCertificate() throws Exception {
        oauth.client("service-account-client", "secret1");

        AccessTokenResponse response;

        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithoutKeyStoreAndTrustStore()) {
            // Request without HoK should fail
            oauth.httpClient().set(client);
            response = oauth.doPasswordGrantRequest("test-user@localhost", "password");
            assertEquals(400, response.getStatusCode());
            assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
            assertEquals("Client Certification missing for MTLS HoK Token Binding", response.getErrorDescription());
        } finally {
            oauth.httpClient().reset();
        }

        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore()) {
            // Request with HoK - success
            oauth.httpClient().set(client);
            response = oauth.doPasswordGrantRequest("test-user@localhost", "password");
            assertEquals(200, response.getStatusCode());

            // Success Pattern
            verifyHoKTokenCertThumbPrint(response, MutualTLSUtils.getThumbprintFromDefaultClientCert(), false);
        } finally {
            oauth.httpClient().reset();
        }
    }

    private void verifyHoKTokenDefaultCertThumbPrint(AccessTokenResponse response) throws Exception {
        verifyHoKTokenCertThumbPrint(response, MutualTLSUtils.getThumbprintFromDefaultClientCert(), true);
    }

    private void verifyHoKTokenOtherCertThumbPrint(AccessTokenResponse response) throws Exception {
        verifyHoKTokenCertThumbPrint(response, MutualTLSUtils.getThumbprintFromOtherClientCert(), true);
    }

    private void verifyHoKTokenCertThumbPrint(AccessTokenResponse response, String certThumbPrint, boolean checkRefreshToken) {
        JWSInput jws = null;
        AccessToken at = null;
        try {
            jws = new JWSInput(response.getAccessToken());
            at = jws.readJsonContent(AccessToken.class);
        } catch (JWSInputException e) {
            Assert.fail(e.toString());
        }
        assertTrue(MessageDigest.isEqual(certThumbPrint.getBytes(), at.getConfirmation().getCertThumbprint().getBytes()));

        if (checkRefreshToken) {
            RefreshToken rt = null;
            try {
                jws = new JWSInput(response.getRefreshToken());
                rt = jws.readJsonContent(RefreshToken.class);
            } catch (JWSInputException e) {
                Assert.fail(e.toString());
            }
            assertTrue(MessageDigest.isEqual(certThumbPrint.getBytes(), rt.getConfirmation().getCertThumbprint().getBytes()));
        }
    }
}
