package org.keycloak.testsuite.oauth;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.util.Base64Url;
import org.keycloak.crypto.KeyUse;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.UserBuilder;
import jakarta.ws.rs.core.UriBuilder;
import java.security.MessageDigest;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.admin.ApiUtil.findUserByUsername;

//https://tools.ietf.org/html/rfc7636

/**
 * @author <a href="takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class OAuthProofKeyForCodeExchangeTest extends AbstractKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);


    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();
    }

    @Before
    public void clientConfiguration() {
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").directAccessGrant(true);
        /*
         * Configure the default client ID. Seems like OAuthClient is keeping the state of clientID
         * For example: If some test case configure oauth.clientId("sample-public-client"), other tests
         * will faile and the clientID will always be "sample-public-client
         * @see AccessTokenTest#testAuthorizationNegotiateHeaderIgnored()
         */
        oauth.clientId("test-app");
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {

        RealmRepresentation realm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);

        UserBuilder user = UserBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .username("no-permissions")
                .addRoles("user")
                .password("password");
        realm.getUsers().add(user.build());

        testRealms.add(realm);

    }

    @Test
    
    public void accessTokenRequestWithoutPKCE() throws Exception {
    	// test case : success : A-1-1
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();
        
        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        
        expectSuccessfulResponseFromTokenEndpoint(codeId, sessionId, code);
    }

    @Test
    public void accessTokenRequestInPKCEValidS256CodeChallengeMethod() throws Exception {
    	// test case : success : A-1-2
    	String codeVerifier = "1234567890123456789012345678901234567890123"; // 43
    	String codeChallenge = generateS256CodeChallenge(codeVerifier);
    	oauth.codeChallenge(codeChallenge);
    	oauth.codeChallengeMethod(OAuth2Constants.PKCE_METHOD_S256);
    	
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        oauth.codeVerifier(codeVerifier);
        
        expectSuccessfulResponseFromTokenEndpoint(codeId, sessionId, code);
    }

    @Test
    public void accessTokenRequestInPKCEUnmatchedCodeVerifierWithS256CodeChallengeMethod() throws Exception {
    	// test case : failure : A-1-5
    	String codeVerifier = "1234567890123456789012345678901234567890123";
    	String codeChallenge = codeVerifier;
    	oauth.codeChallenge(codeChallenge);
    	oauth.codeChallengeMethod(OAuth2Constants.PKCE_METHOD_S256);
    	
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        oauth.codeVerifier(codeVerifier);
        
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");
        
        assertEquals(400, response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_GRANT, response.getError());
        assertEquals("PKCE verification failed: Code mismatch", response.getErrorDescription());
        
        events.expectCodeToToken(codeId, sessionId).error(Errors.PKCE_VERIFICATION_FAILED).clearDetails().assertEvent();
    }
    
    @Test
    
    public void accessTokenRequestInPKCEValidPlainCodeChallengeMethod() throws Exception {
    	// test case : success : A-1-3
    	oauth.codeChallenge(".234567890-234567890~234567890_234567890123");
    	oauth.codeChallengeMethod(OAuth2Constants.PKCE_METHOD_PLAIN);
    	
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        
        oauth.codeVerifier(".234567890-234567890~234567890_234567890123");
        
        expectSuccessfulResponseFromTokenEndpoint(codeId, sessionId, code);
    }

    @Test
    public void accessTokenRequestInPKCEUnmachedCodeVerifierWithPlainCodeChallengeMethod() throws Exception {
    	// test case : failure : A-1-6
    	oauth.codeChallenge("1234567890123456789012345678901234567890123");
    	oauth.codeChallengeMethod(OAuth2Constants.PKCE_METHOD_PLAIN);
    	
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        
        oauth.codeVerifier("aZ_-.~1234567890123456789012345678901234567890123Za");
        
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");
        
        assertEquals(400, response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_GRANT, response.getError());
        assertEquals("PKCE verification failed: Code mismatch", response.getErrorDescription());
        
        events.expectCodeToToken(codeId, sessionId).error(Errors.PKCE_VERIFICATION_FAILED).clearDetails().assertEvent();
    }
    
    @Test
    
    public void accessTokenRequestInPKCEValidDefaultCodeChallengeMethod() throws Exception {
    	// test case : success : A-1-4
    	oauth.codeChallenge("1234567890123456789012345678901234567890123");
    	
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        oauth.codeVerifier("1234567890123456789012345678901234567890123");
        
        expectSuccessfulResponseFromTokenEndpoint(codeId, sessionId, code);
    }
    
    @Test
    public void accessTokenRequestInPKCEWithoutCodeChallengeWithValidCodeChallengeMethod() throws Exception {
    	// test case : failure : A-1-7
    	oauth.codeChallengeMethod(OAuth2Constants.PKCE_METHOD_PLAIN);
        UriBuilder b = UriBuilder.fromUri(oauth.getLoginFormUrl());
        
        driver.navigate().to(b.build().toURL());
    	
        OAuthClient.AuthorizationEndpointResponse errorResponse = new OAuthClient.AuthorizationEndpointResponse(oauth);

        Assert.assertTrue(errorResponse.isRedirected());
        Assert.assertEquals(errorResponse.getError(), OAuthErrorException.INVALID_REQUEST);
        Assert.assertEquals(errorResponse.getErrorDescription(), "Missing parameter: code_challenge");
        
        events.expectLogin().error(Errors.INVALID_REQUEST).user((String) null).session((String) null).clearDetails().assertEvent();
    }
    
    @Test
    public void accessTokenRequestInPKCEInvalidUnderCodeChallengeWithS256CodeChallengeMethod() throws Exception {
    	// test case : failure : A-1-8
    	oauth.codeChallengeMethod(OAuth2Constants.PKCE_METHOD_S256);
    	oauth.codeChallenge("ABCDEFGabcdefg1234567ABCDEFGabcdefg1234567"); // 42
        UriBuilder b = UriBuilder.fromUri(oauth.getLoginFormUrl());
        
        driver.navigate().to(b.build().toURL());
    	
        OAuthClient.AuthorizationEndpointResponse errorResponse = new OAuthClient.AuthorizationEndpointResponse(oauth);

        Assert.assertTrue(errorResponse.isRedirected());
        Assert.assertEquals(errorResponse.getError(), OAuthErrorException.INVALID_REQUEST);
        Assert.assertEquals(errorResponse.getErrorDescription(), "Invalid parameter: code_challenge");
        
        events.expectLogin().error(Errors.INVALID_REQUEST).user((String) null).session((String) null).clearDetails().assertEvent();
    }
    
    @Test
    public void accessTokenRequestInPKCEInvalidOverCodeChallengeWithPlainCodeChallengeMethod() throws Exception {
    	// test case : failure : A-1-9
    	oauth.codeChallengeMethod(OAuth2Constants.PKCE_METHOD_PLAIN);
    	oauth.codeChallenge("3fRc92kac_keic8c7al-3ncbdoaie.DDeizlck3~3fRc92kac_keic8c7al-3ncbdoaie.DDeizlck3~3fRc92kac_keic8c7al-3ncbdoaie.DDeizlck3~123456789"); // 129

    	UriBuilder b = UriBuilder.fromUri(oauth.getLoginFormUrl());
        
        driver.navigate().to(b.build().toURL());
    	
        OAuthClient.AuthorizationEndpointResponse errorResponse = new OAuthClient.AuthorizationEndpointResponse(oauth);

        Assert.assertTrue(errorResponse.isRedirected());
        Assert.assertEquals(errorResponse.getError(), OAuthErrorException.INVALID_REQUEST);
        Assert.assertEquals(errorResponse.getErrorDescription(), "Invalid parameter: code_challenge");
        
        events.expectLogin().error(Errors.INVALID_REQUEST).user((String) null).session((String) null).clearDetails().assertEvent();
    }
    
    @Test
    public void accessTokenRequestInPKCEInvalidUnderCodeVerifierWithS256CodeChallengeMethod() throws Exception {
    	// test case : success : A-1-10
    	String codeVerifier = "ABCDEFGabcdefg1234567ABCDEFGabcdefg1234567"; // 42
    	String codeChallenge = generateS256CodeChallenge(codeVerifier);

    	oauth.codeChallenge(codeChallenge);
    	oauth.codeChallengeMethod(OAuth2Constants.PKCE_METHOD_S256);
    	
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        oauth.codeVerifier(codeVerifier);
        
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");
        
        assertEquals(400, response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_GRANT, response.getError());
        assertEquals("PKCE verification failed: Invalid code verifier", response.getErrorDescription());
        
        events.expectCodeToToken(codeId, sessionId).error(Errors.INVALID_CODE_VERIFIER).clearDetails().assertEvent();
    }
    
    @Test
    public void accessTokenRequestInPKCEInvalidOverCodeVerifierWithS256CodeChallengeMethod() throws Exception {
    	// test case : success : A-1-11
    	String codeVerifier = "3fRc92kac_keic8c7al-3ncbdoaie.DDeizlck3~3fRc92kac_keic8c7al-3ncbdoaie.DDeizlck3~3fRc92kac_keic8c7al-3ncbdoaie.DDeizlck3~123456789"; // 129
    	String codeChallenge = generateS256CodeChallenge(codeVerifier);
    	oauth.codeChallenge(codeChallenge);
    	oauth.codeChallengeMethod(OAuth2Constants.PKCE_METHOD_S256);
    	
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        oauth.codeVerifier(codeVerifier);
        
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");
        
        assertEquals(400, response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_GRANT, response.getError());
        assertEquals("PKCE verification failed: Invalid code verifier", response.getErrorDescription());
        
        events.expectCodeToToken(codeId, sessionId).error(Errors.INVALID_CODE_VERIFIER).clearDetails().assertEvent();
    }

    @Test
    public void accessTokenRequestInPKCEWIthoutCodeVerifierWithS256CodeChallengeMethod() throws Exception {
    	// test case : failure : A-1-12
    	String codeVerifier = "1234567890123456789012345678901234567890123";
    	String codeChallenge = codeVerifier;
    	oauth.codeChallenge(codeChallenge);
    	oauth.codeChallengeMethod(OAuth2Constants.PKCE_METHOD_S256);
    	
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
       
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");
        
        assertEquals(400, response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_GRANT, response.getError());
        assertEquals("PKCE code verifier not specified", response.getErrorDescription());
        
        events.expectCodeToToken(codeId, sessionId).error(Errors.CODE_VERIFIER_MISSING).clearDetails().assertEvent();
    }

    @Test
    public void accessTokenRequestInPKCEInvalidCodeChallengeWithS256CodeChallengeMethod() throws Exception {
    	// test case : failure : A-1-13
    	String codeVerifier = "1234567890123456789=12345678901234567890123";
    	String codeChallenge = codeVerifier;
    	oauth.codeChallenge(codeChallenge);
    	oauth.codeChallengeMethod(OAuth2Constants.PKCE_METHOD_S256);
    	
    	UriBuilder b = UriBuilder.fromUri(oauth.getLoginFormUrl());
        
        driver.navigate().to(b.build().toURL());
    	
        OAuthClient.AuthorizationEndpointResponse errorResponse = new OAuthClient.AuthorizationEndpointResponse(oauth);

        Assert.assertTrue(errorResponse.isRedirected());
        Assert.assertEquals(errorResponse.getError(), OAuthErrorException.INVALID_REQUEST);
        Assert.assertEquals(errorResponse.getErrorDescription(), "Invalid parameter: code_challenge");
        
        events.expectLogin().error(Errors.INVALID_REQUEST).user((String) null).session((String) null).clearDetails().assertEvent();
    }

    @Test
    public void accessTokenRequestInPKCEInvalidCodeVerifierWithS256CodeChallengeMethod() throws Exception {
    	// test case : failure : A-1-14
    	String codeVerifier = "123456789.123456789-123456789~1234$6789_123";
    	String codeChallenge = generateS256CodeChallenge(codeVerifier);
    	oauth.codeChallenge(codeChallenge);
    	oauth.codeChallengeMethod(OAuth2Constants.PKCE_METHOD_S256);
    	
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        oauth.codeVerifier(codeVerifier);
        
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");
        
        assertEquals(400, response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_GRANT, response.getError());
        assertEquals("PKCE verification failed: Invalid code verifier", response.getErrorDescription());
        
        events.expectCodeToToken(codeId, sessionId).error(Errors.INVALID_CODE_VERIFIER).clearDetails().assertEvent();
    }

    @Test
    public void accessTokenRequestInPKCECodeVerifierWithNoCodeChallenge() throws Exception {
        String codeVerifier = "12345678e01234567890g2345678h012a4567j90123"; // 43

        // send oauth request without code_challenge because intercepted
        oauth.codeChallenge(null);
        oauth.codeChallengeMethod(null);
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();
        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        // get the code and add codeVerifier
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        oauth.codeVerifier(codeVerifier);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");

        // assert invalid code because no challenge in authorization
        assertEquals(400, response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_GRANT, response.getError());
        assertEquals("PKCE code verifier specified but challenge not present in authorization", response.getErrorDescription());

        events.expectCodeToToken(codeId, sessionId).error(Errors.INVALID_CODE_VERIFIER).clearDetails().assertEvent();
    }
    
    private String generateS256CodeChallenge(String codeVerifier) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(codeVerifier.getBytes("ISO_8859_1"));
        byte[] digestBytes = md.digest();
        String codeChallenge = Base64Url.encode(digestBytes);
        return codeChallenge;
    }
 
    private void expectSuccessfulResponseFromTokenEndpoint(String codeId, String sessionId, String code)  throws Exception {
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");

        assertEquals(200, response.getStatusCode());
        assertThat(response.getExpiresIn(), allOf(greaterThanOrEqualTo(250), lessThanOrEqualTo(300)));
        assertThat(response.getRefreshExpiresIn(), allOf(greaterThanOrEqualTo(1750), lessThanOrEqualTo(1800)));
        assertEquals("Bearer", response.getTokenType());

        String expectedKid = Stream.of(oauth.doCertsRequest("test").getKeys())
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
        
        // make sure PKCE does not affect token refresh on Token Endpoint
        
        String refreshTokenString = response.getRefreshToken();
        RefreshToken refreshToken = oauth.parseRefreshToken(refreshTokenString);

        Assert.assertNotNull(refreshTokenString);
        assertThat(token.getExp() - getCurrentTime(), allOf(greaterThanOrEqualTo(200L), lessThanOrEqualTo(350L)));
        long actual = refreshToken.getExp() - getCurrentTime();
        assertThat(actual, allOf(greaterThanOrEqualTo(1799L - RefreshTokenTest.ALLOWED_CLOCK_SKEW), lessThanOrEqualTo(1800L + RefreshTokenTest.ALLOWED_CLOCK_SKEW)));
        assertEquals(sessionId, refreshToken.getSessionState());

        setTimeOffset(2);

        OAuthClient.AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(refreshTokenString, "password");
        
        AccessToken refreshedToken = oauth.verifyToken(refreshResponse.getAccessToken());
        RefreshToken refreshedRefreshToken = oauth.parseRefreshToken(refreshResponse.getRefreshToken());

        assertEquals(200, refreshResponse.getStatusCode());
        assertEquals(sessionId, refreshedToken.getSessionState());
        assertEquals(sessionId, refreshedRefreshToken.getSessionState());

        assertThat(refreshResponse.getExpiresIn(), allOf(greaterThanOrEqualTo(250), lessThanOrEqualTo(300)));
        assertThat(refreshedToken.getExp() - getCurrentTime(), allOf(greaterThanOrEqualTo(250L - RefreshTokenTest.ALLOWED_CLOCK_SKEW), lessThanOrEqualTo(300L + RefreshTokenTest.ALLOWED_CLOCK_SKEW)));

        assertThat(refreshedToken.getExp() - token.getExp(), allOf(greaterThanOrEqualTo(1L), lessThanOrEqualTo(10L)));
        assertThat(refreshedRefreshToken.getExp() - refreshToken.getExp(), allOf(greaterThanOrEqualTo(1L), lessThanOrEqualTo(10L)));

        Assert.assertNotEquals(token.getId(), refreshedToken.getId());
        Assert.assertNotEquals(refreshToken.getId(), refreshedRefreshToken.getId());

        assertEquals("Bearer", refreshResponse.getTokenType());

        assertEquals(findUserByUsername(adminClient.realm("test"), "test-user@localhost").getId(), refreshedToken.getSubject());
        // The following check is not valid anymore since file store does have the same ID, and is redundant due to the previous line
        // Assert.assertNotEquals("test-user@localhost", refreshedToken.getSubject());

        assertEquals(2, refreshedToken.getRealmAccess().getRoles().size());
        Assert.assertTrue(refreshedToken.getRealmAccess().isUserInRole("user"));

        assertEquals(1, refreshedToken.getResourceAccess(oauth.getClientId()).getRoles().size());
        Assert.assertTrue(refreshedToken.getResourceAccess(oauth.getClientId()).isUserInRole("customer-user"));

        EventRepresentation refreshEvent = events.expectRefresh(event.getDetails().get(Details.REFRESH_TOKEN_ID), sessionId).assertEvent();
        Assert.assertNotEquals(event.getDetails().get(Details.TOKEN_ID), refreshEvent.getDetails().get(Details.TOKEN_ID));
        Assert.assertNotEquals(event.getDetails().get(Details.REFRESH_TOKEN_ID), refreshEvent.getDetails().get(Details.UPDATED_REFRESH_TOKEN_ID));

        setTimeOffset(0);
    }

    // KEYCLOAK-10747 Explicit Proof Key for Code Exchange Activation Settings

    private void setPkceActivationSettings(String clientId, String codeChallengeMethodName) {
        ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm("test"), clientId);
        ClientRepresentation clientRep = clientResource.toRepresentation();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setPkceCodeChallengeMethod(codeChallengeMethodName);
        clientResource.update(clientRep);
    }

    @Test
    public void accessTokenRequestValidS256CodeChallengeMethodPkceEnforced() throws Exception {
        try {
            setPkceActivationSettings("test-app", OAuth2Constants.PKCE_METHOD_S256);
            String codeVerifier = "1a345A7890123456r8901c3456789012b45K7890l23"; // 43
            String codeChallenge = generateS256CodeChallenge(codeVerifier);
            oauth.codeChallenge(codeChallenge);
            oauth.codeChallengeMethod(OAuth2Constants.PKCE_METHOD_S256);

            oauth.doLogin("test-user@localhost", "password");

            EventRepresentation loginEvent = events.expectLogin().assertEvent();

            String sessionId = loginEvent.getSessionId();
            String codeId = loginEvent.getDetails().get(Details.CODE_ID);

            String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

            oauth.codeVerifier(codeVerifier);

            expectSuccessfulResponseFromTokenEndpoint(codeId, sessionId, code);
        } finally {
            setPkceActivationSettings("test-app", null);
        }
    }

    @Test
     // unstable
    //accessTokenRequestValidPlainCodeChallengeMethodPkceEnforced:561->expectSuccessfulResponseFromTokenEndpoint:465
    //  Expected: (a value equal to or greater than <1799> and a value less than or equal to <1800>)
    //  but: a value equal to or greater than <1799> <1798> was less than <1799>
    public void accessTokenRequestValidPlainCodeChallengeMethodPkceEnforced() throws Exception {
        try {
            setPkceActivationSettings("test-app", OAuth2Constants.PKCE_METHOD_PLAIN);
            String codeVerifier = "12E45r78901d3456789G12y45G78901234B67v901u3"; // 43
            String codeChallenge = codeVerifier;
            oauth.codeChallenge(codeChallenge);
            oauth.codeChallengeMethod(OAuth2Constants.PKCE_METHOD_PLAIN);

            oauth.doLogin("test-user@localhost", "password");

            EventRepresentation loginEvent = events.expectLogin().assertEvent();

            String sessionId = loginEvent.getSessionId();
            String codeId = loginEvent.getDetails().get(Details.CODE_ID);

            String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

            oauth.codeVerifier(codeVerifier);

            expectSuccessfulResponseFromTokenEndpoint(codeId, sessionId, code);
        } finally {
            setPkceActivationSettings("test-app", null);
        }
    }
 
    @Test
    public void accessTokenRequestCodeChallengeMethodMismatchPkceEnforced() throws Exception {
        try {
            setPkceActivationSettings("test-app", OAuth2Constants.PKCE_METHOD_S256);
            String codeVerifier = "12345678e01234567890g2345678h012a4567j90123"; // 43
            String codeChallenge = generateS256CodeChallenge(codeVerifier);
            oauth.codeChallenge(codeChallenge);
            oauth.codeChallengeMethod(OAuth2Constants.PKCE_METHOD_PLAIN);

            UriBuilder b = UriBuilder.fromUri(oauth.getLoginFormUrl());

            driver.navigate().to(b.build().toURL());

            OAuthClient.AuthorizationEndpointResponse errorResponse = new OAuthClient.AuthorizationEndpointResponse(oauth);

            Assert.assertTrue(errorResponse.isRedirected());
            Assert.assertEquals(errorResponse.getError(), OAuthErrorException.INVALID_REQUEST);
            Assert.assertEquals(errorResponse.getErrorDescription(), "Invalid parameter: code challenge method is not matching the configured one");

            events.expectLogin().error(Errors.INVALID_REQUEST).user((String) null).session((String) null).clearDetails().assertEvent();
        } finally {
            setPkceActivationSettings("test-app", null);
        }
    }

    @Test
    public void accessTokenRequestCodeChallengeMethodMissingPkceEnforced() throws Exception {
        try {
            setPkceActivationSettings("test-app", OAuth2Constants.PKCE_METHOD_S256);
            String codeVerifier = "1234567890123456789012345678901234567890123"; // 43
            String codeChallenge = generateS256CodeChallenge(codeVerifier);
            oauth.codeChallenge(codeChallenge);

            UriBuilder b = UriBuilder.fromUri(oauth.getLoginFormUrl());

            driver.navigate().to(b.build().toURL());

            OAuthClient.AuthorizationEndpointResponse errorResponse = new OAuthClient.AuthorizationEndpointResponse(oauth);

            Assert.assertTrue(errorResponse.isRedirected());
            Assert.assertEquals(errorResponse.getError(), OAuthErrorException.INVALID_REQUEST);
            Assert.assertEquals(errorResponse.getErrorDescription(), "Missing parameter: code_challenge_method");

            events.expectLogin().error(Errors.INVALID_REQUEST).user((String) null).session((String) null).clearDetails().assertEvent();

        } finally {
            setPkceActivationSettings("test-app", null);
        }
    }

    @Test
    public void accessTokenRequestCodeChallengeMissingPkceEnforced() throws Exception {
        try {
            setPkceActivationSettings("test-app", OAuth2Constants.PKCE_METHOD_S256);
            oauth.codeChallengeMethod(OAuth2Constants.PKCE_METHOD_S256);

            UriBuilder b = UriBuilder.fromUri(oauth.getLoginFormUrl());

            driver.navigate().to(b.build().toURL());

            OAuthClient.AuthorizationEndpointResponse errorResponse = new OAuthClient.AuthorizationEndpointResponse(oauth);

            Assert.assertTrue(errorResponse.isRedirected());
            Assert.assertEquals(errorResponse.getError(), OAuthErrorException.INVALID_REQUEST);
            Assert.assertEquals(errorResponse.getErrorDescription(), "Missing parameter: code_challenge");

            events.expectLogin().error(Errors.INVALID_REQUEST).user((String) null).session((String) null).clearDetails().assertEvent();

        } finally {
            setPkceActivationSettings("test-app", null);
        }
    }

    @Test
    public void accessTokenRequestInvalidCodeChallengePkceEnforced() throws Exception {
        try {
            setPkceActivationSettings("test-app", OAuth2Constants.PKCE_METHOD_S256);
            oauth.codeChallenge("invalid");
            oauth.codeChallengeMethod(OAuth2Constants.PKCE_METHOD_S256);

            UriBuilder b = UriBuilder.fromUri(oauth.getLoginFormUrl());

            driver.navigate().to(b.build().toURL());

            OAuthClient.AuthorizationEndpointResponse errorResponse = new OAuthClient.AuthorizationEndpointResponse(oauth);

            Assert.assertTrue(errorResponse.isRedirected());
            Assert.assertEquals(errorResponse.getError(), OAuthErrorException.INVALID_REQUEST);
            Assert.assertEquals(errorResponse.getErrorDescription(), "Invalid parameter: code_challenge");

            events.expectLogin().error(Errors.INVALID_REQUEST).user((String) null).session((String) null).clearDetails().assertEvent();

        } finally {
            setPkceActivationSettings("test-app", null);
        }
    }

    @Test
    public void accessTokenRequestWithoutCodeVerifierPkceEnforced() throws Exception {
        try {
            setPkceActivationSettings("test-app", OAuth2Constants.PKCE_METHOD_S256);
            String codeVerifier = "1234567890123456789012345678901234567890123";
            String codeChallenge = generateS256CodeChallenge(codeVerifier);
            oauth.codeChallenge(codeChallenge);
            oauth.codeChallengeMethod(OAuth2Constants.PKCE_METHOD_S256);

            oauth.doLogin("test-user@localhost", "password");

            EventRepresentation loginEvent = events.expectLogin().assertEvent();

            String sessionId = loginEvent.getSessionId();
            String codeId = loginEvent.getDetails().get(Details.CODE_ID);

            String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

            OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");

            assertEquals(400, response.getStatusCode());
            assertEquals(OAuthErrorException.INVALID_GRANT, response.getError());
            assertEquals("PKCE code verifier not specified", response.getErrorDescription());

            events.expectCodeToToken(codeId, sessionId).error(Errors.CODE_VERIFIER_MISSING).clearDetails().assertEvent();
        } finally {
            setPkceActivationSettings("test-app", null);
        }
    }
}
