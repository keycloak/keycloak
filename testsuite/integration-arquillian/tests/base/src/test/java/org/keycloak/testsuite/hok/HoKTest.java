package org.keycloak.testsuite.hok;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.admin.ApiUtil.findUserByUsername;
import static org.keycloak.testsuite.arquillian.AuthServerTestEnricher.AUTH_SERVER_SSL_REQUIRED;

import java.io.IOException;
import java.net.URI;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.hamcrest.Matchers;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.junit.*;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.crypto.Algorithm;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.jose.jws.crypto.HashUtils;
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
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.drone.Different;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.MutualTLSUtils;
import org.keycloak.testsuite.util.KeycloakModelUtils;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.UserInfoClientUtil;
import org.keycloak.testsuite.util.OAuthClient.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;
import org.openqa.selenium.WebDriver;


public class HoKTest extends AbstractTestRealmKeycloakTest {
    // KEYCLOAK-6771 Certificate Bound Token
    // https://tools.ietf.org/html/draft-ietf-oauth-mtls-08#section-3

    @Drone
    @Different
    protected WebDriver driver2;

    private static final List<String> CLIENT_LIST = Arrays.asList("test-app", "named-test-app");

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
                    .session(isUUID());
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
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        AccessTokenResponse response;
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore()) {
            response = oauth.doAccessTokenRequest(code, "password", client);
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        // Success Pattern
        expectSuccessfulResponseFromTokenEndpoint(sessionId, codeId, response);
        verifyHoKTokenDefaultCertThumbPrint(response);
    }
    
    @Test
    public void accessTokenRequestWithoutClientCertificate() throws Exception {
        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        AccessTokenResponse response;
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithoutKeyStoreAndTrustStore()) {
            response = oauth.doAccessTokenRequest(code, "password", client);
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        // Error Pattern
        assertEquals(400, response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
        assertEquals("Client Certification missing for MTLS HoK Token Binding", response.getErrorDescription());
    }

    private void expectSuccessfulResponseFromTokenEndpoint(String sessionId, String codeId, AccessTokenResponse response) throws Exception {
        assertEquals(200, response.getStatusCode());

        Assert.assertThat(response.getExpiresIn(), allOf(greaterThanOrEqualTo(250), lessThanOrEqualTo(300)));
        Assert.assertThat(response.getRefreshExpiresIn(), allOf(greaterThanOrEqualTo(1750), lessThanOrEqualTo(1800)));

        assertEquals("bearer", response.getTokenType());

        String expectedKid = oauth.doCertsRequest("test").getKeys()[0].getKeyId();

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
        assertEquals("HS256", header.getAlgorithm().name());
        assertEquals("JWT", header.getType());
        assertNull(header.getContentType());

        AccessToken token = oauth.verifyToken(response.getAccessToken());

        assertEquals(findUserByUsername(adminClient.realm("test"), "test-user@localhost").getId(), token.getSubject());
        Assert.assertNotEquals("test-user@localhost", token.getSubject());

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
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        AccessTokenResponse tokenResponse = null;
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore()) {
            tokenResponse = oauth.doAccessTokenRequest(code, "password", client);
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        verifyHoKTokenDefaultCertThumbPrint(tokenResponse);
        String refreshTokenString = tokenResponse.getRefreshToken();

        // second client user login
        OAuthClient oauth2 = new OAuthClient();
        oauth2.init(driver2);
        oauth2.doLogin("john-doh@localhost", "password");
        String code2 = oauth2.getCurrentQuery().get(OAuth2Constants.CODE);
        AccessTokenResponse tokenResponse2 = null;
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithOtherKeyStoreAndTrustStore()) {
            tokenResponse2 = oauth2.doAccessTokenRequest(code2, "password", client);
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        verifyHoKTokenOtherCertThumbPrint(tokenResponse2);

        // token refresh by second client by first client's refresh token
        AccessTokenResponse response = null;
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithOtherKeyStoreAndTrustStore()) {
            response = oauth2.doRefreshTokenRequest(refreshTokenString, "password", client);
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
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
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        AccessTokenResponse tokenResponse = null;
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore()) {
            tokenResponse = oauth.doAccessTokenRequest(code, "password", client);
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        verifyHoKTokenDefaultCertThumbPrint(tokenResponse);
        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
        String refreshTokenString = tokenResponse.getRefreshToken();
        RefreshToken refreshToken = oauth.parseRefreshToken(refreshTokenString);
        EventRepresentation tokenEvent = events.expectCodeToToken(codeId, sessionId).assertEvent();

        Assert.assertNotNull(refreshTokenString);
        assertEquals("bearer", tokenResponse.getTokenType());
        Assert.assertThat(token.getExpiration() - getCurrentTime(), allOf(greaterThanOrEqualTo(200), lessThanOrEqualTo(350)));
        int actual = refreshToken.getExpiration() - getCurrentTime();
        Assert.assertThat(actual, allOf(greaterThanOrEqualTo(1799), lessThanOrEqualTo(1800)));
        assertEquals(sessionId, refreshToken.getSessionState());

        setTimeOffset(2);

        AccessTokenResponse response = null;
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore()) {
            response = oauth.doRefreshTokenRequest(refreshTokenString, "password", client);
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
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
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        AccessTokenResponse tokenResponse = null;
        tokenResponse = oauth.doAccessTokenRequest(code, "password");

        verifyHoKTokenDefaultCertThumbPrint(tokenResponse);
        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
        String refreshTokenString = tokenResponse.getRefreshToken();
        RefreshToken refreshToken = oauth.parseRefreshToken(refreshTokenString);

        Assert.assertNotNull(refreshTokenString);
        assertEquals("bearer", tokenResponse.getTokenType());
        Assert.assertThat(token.getExpiration() - getCurrentTime(), allOf(greaterThanOrEqualTo(200), lessThanOrEqualTo(350)));
        int actual = refreshToken.getExpiration() - getCurrentTime();
        Assert.assertThat(actual, allOf(greaterThanOrEqualTo(1799), lessThanOrEqualTo(1800)));
        assertEquals(sessionId, refreshToken.getSessionState());

        setTimeOffset(2);

        AccessTokenResponse response = null;
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithoutKeyStoreAndTrustStore()) {
            response = oauth.doRefreshTokenRequest(refreshTokenString, "password", client);
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
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
        if (refreshedToken.getCertConf() != null) {
            log.warnf("refreshed access token's cnf-x5t#256 = %s", refreshedToken.getCertConf().getCertThumbprint());
            log.warnf("refreshed refresh token's cnf-x5t#256 = %s", refreshedRefreshToken.getCertConf().getCertThumbprint());    
        }

        assertEquals(200, response.getStatusCode());

        assertEquals(sessionId, refreshedToken.getSessionState());
        assertEquals(sessionId, refreshedRefreshToken.getSessionState());

        Assert.assertThat(response.getExpiresIn(), allOf(greaterThanOrEqualTo(250), lessThanOrEqualTo(300)));
        Assert.assertThat(refreshedToken.getExpiration() - getCurrentTime(), allOf(greaterThanOrEqualTo(250), lessThanOrEqualTo(300)));

        Assert.assertThat(refreshedToken.getExpiration() - token.getExpiration(), allOf(greaterThanOrEqualTo(1), lessThanOrEqualTo(10)));
        Assert.assertThat(refreshedRefreshToken.getExpiration() - refreshToken.getExpiration(), allOf(greaterThanOrEqualTo(1), lessThanOrEqualTo(10)));

        Assert.assertNotEquals(token.getId(), refreshedToken.getId());
        Assert.assertNotEquals(refreshToken.getId(), refreshedRefreshToken.getId());

        assertEquals("bearer", response.getTokenType());

        assertEquals(findUserByUsername(adminClient.realm("test"), username).getId(), refreshedToken.getSubject());
        Assert.assertNotEquals(username, refreshedToken.getSubject());

        assertEquals(2, refreshedToken.getRealmAccess().getRoles().size());
        Assert.assertTrue(refreshedToken.getRealmAccess().isUserInRole("user"));

        assertEquals(1, refreshedToken.getResourceAccess(oauth.getClientId()).getRoles().size());
        Assert.assertTrue(refreshedToken.getResourceAccess(oauth.getClientId()).isUserInRole("customer-user"));

        EventRepresentation refreshEvent = events.expectRefresh(tokenEvent.getDetails().get(Details.REFRESH_TOKEN_ID), sessionId).user(AssertEvents.isUUID()).assertEvent();
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
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        AccessTokenResponse tokenResponse = null;
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore()) {
            tokenResponse = oauth.doAccessTokenRequest(code, "password", client);
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
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
            response = userInfoTarget.request().header(HttpHeaders.AUTHORIZATION, "bearer " + tokenResponse.getAccessToken()).get();
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
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        AccessTokenResponse tokenResponse = null;
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore()) {
            tokenResponse = oauth.doAccessTokenRequest(code, "password", client);
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        verifyHoKTokenDefaultCertThumbPrint(tokenResponse);
        events.expectCodeToToken(codeId, sessionId).assertEvent();

        // execute the access token to get UserInfo without token binded client certificate in mutual authentication TLS
        ClientBuilder clientBuilder = ClientBuilder.newBuilder();
        Client client = clientBuilder.build();
        WebTarget userInfoTarget = null;
        Response response = null;
        try {
            userInfoTarget = UserInfoClientUtil.getUserInfoWebTarget(client);
            response = userInfoTarget.request().header(HttpHeaders.AUTHORIZATION, "bearer " + tokenResponse.getAccessToken()).get();
            assertEquals(401, response.getStatus());
        } finally {
            response.close();
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

        CloseableHttpResponse response = null;
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore()) {
            response = oauth.doLogout(refreshTokenString, "password", client);
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        // Success Pattern
        assertThat(response, org.keycloak.testsuite.util.Matchers.statusCodeIsHC(Status.NO_CONTENT));
        assertNotNull(testingClient.testApp().getAdminLogoutAction());
    }

    @Test
    public void postLogoutByHoKRefreshTokenWithoutClientCertificate() throws Exception {
        String refreshTokenString = execPreProcessPostLogout();

        CloseableHttpResponse response = null;
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithoutKeyStoreAndTrustStore()) {
            response = oauth.doLogout(refreshTokenString, "password", client);
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        // Error Pattern
        assertEquals(401, response.getStatusLine().getStatusCode());
    }

    private String execPreProcessPostLogout() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        oauth.clientSessionState("client-session");
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
        verifyHoKTokenDefaultCertThumbPrint(tokenResponse);

        return tokenResponse.getRefreshToken();
    }

    // Hybrid Code Flow : response_type = code id_token
    // Bind HoK Token
    
    @Test
    public void accessTokenRequestWithClientCertificateInHybridFlowWithCodeIDToken() throws Exception {
        String nonce = "ckw938gnspa93dj";
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").standardFlow(true).implicitFlow(true);
        oauth.clientId("test-app");
        oauth.responseType(OIDCResponseType.CODE + " " + OIDCResponseType.ID_TOKEN);
        oauth.nonce(nonce);

        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();
        OAuthClient.AuthorizationEndpointResponse authzResponse = new OAuthClient.AuthorizationEndpointResponse(oauth, true);
        Assert.assertNotNull(authzResponse.getSessionState());
        List<IDToken> idTokens = testAuthzResponseAndRetrieveIDTokens(authzResponse, loginEvent);
        for (IDToken idToken : idTokens) {
            Assert.assertEquals(nonce, idToken.getNonce());
            Assert.assertEquals(authzResponse.getSessionState(), idToken.getSessionState());
        }
    }

    protected List<IDToken> testAuthzResponseAndRetrieveIDTokens(OAuthClient.AuthorizationEndpointResponse authzResponse, EventRepresentation loginEvent) {
        Assert.assertEquals(OIDCResponseType.CODE + " " + OIDCResponseType.ID_TOKEN, loginEvent.getDetails().get(Details.RESPONSE_TYPE));

        // IDToken from the authorization response
        Assert.assertNull(authzResponse.getAccessToken());
        String idTokenStr = authzResponse.getIdToken();
        IDToken idToken = oauth.verifyIDToken(idTokenStr);

        // Validate "c_hash"
        Assert.assertNull(idToken.getAccessTokenHash());
        Assert.assertNotNull(idToken.getCodeHash());
        Assert.assertEquals(idToken.getCodeHash(), HashUtils.oidcHash(Algorithm.RS256, authzResponse.getCode()));

        // IDToken exchanged for the code
        IDToken idToken2 = sendTokenRequestAndGetIDToken(loginEvent);

        return Arrays.asList(idToken, idToken2);
    }

    @Test
    public void testIntrospectHoKAccessToken() throws Exception {
        // get an access token with client certificate in mutual authenticate TLS
        // mimic Client
        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        EventRepresentation loginEvent = events.expectLogin().assertEvent();
        AccessTokenResponse accessTokenResponse = null;
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore()) {
           accessTokenResponse = oauth.doAccessTokenRequest(code, "password", client);
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        // Do token introspection
        // mimic Resource Server
        String tokenResponse;
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithoutKeyStoreAndTrustStore()) {
            tokenResponse = oauth.introspectTokenWithClientCredential("confidential-cli", "secret1", "access_token", accessTokenResponse.getAccessToken(), client);
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        TokenMetadataRepresentation rep = JsonSerialization.readValue(tokenResponse, TokenMetadataRepresentation.class);
        JWSInput jws = new JWSInput(accessTokenResponse.getAccessToken());
        AccessToken at = jws.readJsonContent(AccessToken.class);
        jws = new JWSInput(accessTokenResponse.getRefreshToken());
        RefreshToken rt = jws.readJsonContent(RefreshToken.class);
        String certThumprintFromAccessToken = at.getCertConf().getCertThumbprint();
        String certThumprintFromRefreshToken = rt.getCertConf().getCertThumbprint();
        String certThumprintFromTokenIntrospection = rep.getCertConf().getCertThumbprint();
        String certThumprintFromBoundClientCertificate = MutualTLSUtils.getThumbprintFromDefaultClientCert();

        assertTrue(rep.isActive());
        assertEquals("test-user@localhost", rep.getUserName());
        assertEquals("test-app", rep.getClientId());
        assertEquals(loginEvent.getUserId(), rep.getSubject());

        assertEquals(certThumprintFromTokenIntrospection, certThumprintFromBoundClientCertificate);
        assertEquals(certThumprintFromBoundClientCertificate, certThumprintFromAccessToken);
        assertEquals(certThumprintFromAccessToken, certThumprintFromRefreshToken);

    }


    private void verifyHoKTokenDefaultCertThumbPrint(AccessTokenResponse response) throws Exception {
        verifyHoKTokenCertThumbPrint(response, MutualTLSUtils.getThumbprintFromDefaultClientCert());
    }

    private void verifyHoKTokenOtherCertThumbPrint(AccessTokenResponse response) throws Exception {
        verifyHoKTokenCertThumbPrint(response, MutualTLSUtils.getThumbprintFromOtherClientCert());
    }

    private void verifyHoKTokenCertThumbPrint(AccessTokenResponse response, String certThumbPrint) {
        JWSInput jws = null;
        AccessToken at = null;
        try {
            jws = new JWSInput(response.getAccessToken());
            at = jws.readJsonContent(AccessToken.class);
        } catch (JWSInputException e) {
            Assert.fail(e.toString());
        }
        assertTrue(MessageDigest.isEqual(certThumbPrint.getBytes(), at.getCertConf().getCertThumbprint().getBytes()));

        RefreshToken rt = null;
        try {
            jws = new JWSInput(response.getRefreshToken());
            rt = jws.readJsonContent(RefreshToken.class);
        } catch (JWSInputException e) {
            Assert.fail(e.toString());
        }
        assertTrue(MessageDigest.isEqual(certThumbPrint.getBytes(), rt.getCertConf().getCertThumbprint().getBytes()));
    }
}
