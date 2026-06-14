package org.keycloak.tests.oauth.tokenexchange;

import java.util.logging.Logger;

import org.keycloak.common.Profile;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.representations.IDJAG;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 *
 * @author yutaka.obuchi.sd@hitachi.com
 */
@KeycloakIntegrationTest(config = IDJAGTokenExchangeTest.JWTAuthorizationGrantServerConfig.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
public class IDJAGTokenExchangeTest {
 
    private static final Logger LOG = Logger.getLogger(IDJAGTokenExchangeTest.class.getName());
    
    @InjectRealm(ref = "ssodomain", config = SSODomainConfig.class)
    ManagedRealm ssodomain;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectOAuthClient(ref = "oauth2", webDriverRef = "webDriver2")
    OAuthClient oauth2;

    public static class SSODomainConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name("ssodomain");
        
            // test client for a Resource Authorization Server in IdP
            realm.clients(ClientBuilder.create().clientId("todo")
                    .secret("password")
                    .redirectUris("https://login.saas-tool.example/callback")
                    .attribute("idjag.resource.authorization.server.identifier","https://login.saas-tool.example/"));

            
            // test client to request the Token exchange in ssodomain
            realm.clients(ClientBuilder.create().clientId("agent")
                    .secret("password")
                    .redirectUris("*")
                    .attribute("standard.token.exchange.enabled", "true")
                    .attribute("idjag.clientid.at.todo","agent-at-todo")
                    .attribute("idjag.permitted.scopes.at.todo","read:something write:something"));

            realm.clients(ClientBuilder.create().clientId("wrong_agent")
                    .secret("password")
                    .redirectUris("*")
                    .attribute("standard.token.exchange.enabled", "true")
                    .attribute("idjag.clientid.at.todo","wrong_agent-at-todo")
                    .attribute("idjag.permitted.scopes.at.todo","read:something write:something"));

            // test user in ssodomain
            realm.users(UserBuilder.create().username("testuser")
                    .name("Test", "User")
                    .email("test@localhost")
                    .emailVerified(Boolean.TRUE)
                    .password("password"));

            return realm;
        
        }
    }

    @Test
    public void IDJAGIssuance() throws Exception {

        try {
            ssodomain.admin().logoutAll();
        } catch (Exception e) {
            LOG.log(java.util.logging.Level.WARNING, "Failed to logout all sessions via Admin API", e);
        }


        // do a login in ssodomain to obtain an access token with agent
        oauth.realm("ssodomain").client("agent", "password").openLoginForm();
        oauth.fillLoginForm("testuser", "password");
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse.isSuccess());

        // perform token exchange to get the assertion grant
        tokenResponse = oauth.tokenExchangeRequest(tokenResponse.getIdToken(),"urn:ietf:params:oauth:token-type:id_token")
                .audience("https://login.saas-tool.example/")
                .requestedTokenType("urn:ietf:params:oauth:token-type:id-jag")
                .scope("read:something")
                .send();

        Assertions.assertTrue(tokenResponse.isSuccess());
        Assertions.assertNull(tokenResponse.getRefreshToken());
        Assertions.assertEquals(org.keycloak.OAuth2Constants.IDENTITY_ASSERTION_JWT_TOKEN_TYPE, tokenResponse.getIssuedTokenType());
        Assertions.assertEquals(org.keycloak.util.TokenUtil.TOKEN_TYPE_NA, tokenResponse.getTokenType());
        Assertions.assertEquals("read:something", tokenResponse.getScope());

        IDJAG idjag = oauth.parseToken(tokenResponse.getAccessToken(), IDJAG.class);
        Assertions.assertEquals("agent-at-todo", idjag.getClientId());
        Assertions.assertEquals("https://login.saas-tool.example/", idjag.getAudience()[0]);
        Assertions.assertEquals("read:something", idjag.getScope());
        Assertions.assertNotNull(idjag.getSessionId());
        Assertions.assertEquals(oauth.getEndpoints().getIssuer(), idjag.getIssuer());
        JWSInput jws;
        try {
            jws = new JWSInput(tokenResponse.getAccessToken());
        } catch (JWSInputException e) {
            throw new RuntimeException("The provided assertion is not a valid JWT");
        }

        String jwtTokenType = jws.getHeader().getType();
        Assertions.assertEquals("oauth-id-jag+jwt", jwtTokenType);

    }

    @Test
    public void IDJAGIssuance_invalidscope() throws Exception {

        try {
            ssodomain.admin().logoutAll();
        } catch (Exception e) {
            LOG.log(java.util.logging.Level.WARNING, "Failed to logout all sessions via Admin API", e);
        }

        oauth.realm("ssodomain").client("agent", "password").openLoginForm();
        oauth.fillLoginForm("testuser", "password");
    
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse.isSuccess());

        tokenResponse = oauth.tokenExchangeRequest(tokenResponse.getIdToken(),"urn:ietf:params:oauth:token-type:id_token")
                .audience("https://login.saas-tool.example/")
                .requestedTokenType("urn:ietf:params:oauth:token-type:id-jag")
                .scope("read:other")
                .send();
        

        Assertions.assertFalse(tokenResponse.isSuccess());
        Assertions.assertEquals(400, tokenResponse.getStatusCode());
        Assertions.assertNull(tokenResponse.getAccessToken());
        Assertions.assertEquals("invalid_scope", tokenResponse.getError());
        Assertions.assertEquals("Invalid scopes: read:other", tokenResponse.getErrorDescription());
    }

    @Test
    public void IDJAGIssuance_invalidclient() throws Exception {

        try {
            ssodomain.admin().logoutAll();
        } catch (Exception e) {
            LOG.log(java.util.logging.Level.WARNING, "Failed to logout all sessions via Admin API", e);
        }

        oauth2.realm("ssodomain").client("wrong_agent", "password").openLoginForm();
        oauth2.fillLoginForm("testuser", "password");
        AccessTokenResponse tokenResponse_for_wrong_agent = oauth2.doAccessTokenRequest(oauth2.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse_for_wrong_agent.isSuccess());

        oauth.realm("ssodomain").client("agent", "password").openLoginForm();
        oauth.fillLoginForm("testuser", "password");
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse.isSuccess());

        tokenResponse = oauth.tokenExchangeRequest(tokenResponse_for_wrong_agent.getIdToken(),"urn:ietf:params:oauth:token-type:id_token")
                .audience("https://login.saas-tool.example/")
                .requestedTokenType("urn:ietf:params:oauth:token-type:id-jag")
                .scope("read:something")
                .send();

        Assertions.assertFalse(tokenResponse.isSuccess());
        Assertions.assertEquals(403, tokenResponse.getStatusCode());
        Assertions.assertNull(tokenResponse.getAccessToken());
        Assertions.assertEquals("access_denied", tokenResponse.getError());
        Assertions.assertEquals("Client is not within the token audience", tokenResponse.getErrorDescription());
    }

    @Test
    public void IDJAGIssuance_wrong_subject_token_type() throws Exception {

        try {
            ssodomain.admin().logoutAll();
        } catch (Exception e) {
            LOG.log(java.util.logging.Level.WARNING, "Failed to logout all sessions via Admin API", e);
        }

        oauth.realm("ssodomain").client("agent", "password").openLoginForm();
        oauth.fillLoginForm("testuser", "password");

        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse.isSuccess());

        tokenResponse = oauth.tokenExchangeRequest(tokenResponse.getIdToken(),"urn:ietf:params:oauth:token-type:id-jag")
                .audience("https://login.saas-tool.example/")
                .requestedTokenType("urn:ietf:params:oauth:token-type:id-jag")
                .scope("read:something")
                .send();

        Assertions.assertFalse(tokenResponse.isSuccess());
        Assertions.assertEquals(400, tokenResponse.getStatusCode());
        Assertions.assertNull(tokenResponse.getAccessToken());
        Assertions.assertEquals("invalid_request", tokenResponse.getError());
        Assertions.assertEquals("Parameter 'subject_token' supports IDToken only", tokenResponse.getErrorDescription());
    }

    @Test
    public void IDJAGIssuance_wrong_subject_token() throws Exception {

        try {
            ssodomain.admin().logoutAll();
        } catch (Exception e) {
            LOG.log(java.util.logging.Level.WARNING, "Failed to logout all sessions via Admin API", e);
        }

        oauth.realm("ssodomain").client("agent", "password").openLoginForm();
        oauth.fillLoginForm("testuser", "password");
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse.isSuccess());

        tokenResponse = oauth.tokenExchangeRequest(tokenResponse.getAccessToken(),"urn:ietf:params:oauth:token-type:id_token")
                .audience("https://login.saas-tool.example/")
                .requestedTokenType("urn:ietf:params:oauth:token-type:id-jag")
                .scope("read:something")
                .send();
        

        Assertions.assertFalse(tokenResponse.isSuccess());
        Assertions.assertEquals(400, tokenResponse.getStatusCode());
        Assertions.assertNull(tokenResponse.getAccessToken());
        Assertions.assertEquals("invalid_token", tokenResponse.getError());
        Assertions.assertEquals("Token type is incorrect. Expected '[ID]' but was 'Bearer'", tokenResponse.getErrorDescription());
    }

    @Test
    public void IDJAGIssuance_wrong_requested_token_type() throws Exception {
 
        try {
            ssodomain.admin().logoutAll();
        } catch (Exception e) {
            LOG.log(java.util.logging.Level.WARNING, "Failed to logout all sessions via Admin API", e);
        }

        oauth.realm("ssodomain").client("agent", "password").openLoginForm();
        oauth.fillLoginForm("testuser", "password");
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse.isSuccess());

        tokenResponse = oauth.tokenExchangeRequest(tokenResponse.getIdToken(),"urn:ietf:params:oauth:token-type:id_token")
                .audience("https://login.saas-tool.example/")
                .requestedTokenType("urn:ietf:params:oauth:token-type:id-jag-wrong")
                .scope("read:something")
                .send();

        Assertions.assertFalse(tokenResponse.isSuccess());
        Assertions.assertEquals(400, tokenResponse.getStatusCode());
        Assertions.assertNull(tokenResponse.getAccessToken());
        Assertions.assertEquals("invalid_request", tokenResponse.getError());
        Assertions.assertEquals("Parameter 'subject_token' supports access tokens only", tokenResponse.getErrorDescription());
    }

    @Test
    public void IDJAGIssuance_wrong_audience() throws Exception {

        try {
            ssodomain.admin().logoutAll();
        } catch (Exception e) {
            LOG.log(java.util.logging.Level.WARNING, "Failed to logout all sessions via Admin API", e);
        }
 
        oauth.realm("ssodomain").client("agent", "password").openLoginForm();
        oauth.fillLoginForm("testuser", "password");
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse.isSuccess());

        tokenResponse = oauth.tokenExchangeRequest(tokenResponse.getIdToken(),"urn:ietf:params:oauth:token-type:id_token")
                .audience("https://wrong-login.saas-tool.example/")
                .requestedTokenType("urn:ietf:params:oauth:token-type:id-jag")
                .scope("read:something")
                .send();

        Assertions.assertFalse(tokenResponse.isSuccess());
        Assertions.assertEquals(400, tokenResponse.getStatusCode());
        Assertions.assertNull(tokenResponse.getAccessToken());
        Assertions.assertEquals("invalid_request", tokenResponse.getError());
        Assertions.assertEquals("Client not found for audience identifier: https://wrong-login.saas-tool.example/", tokenResponse.getErrorDescription());
    }

    public static class JWTAuthorizationGrantServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.IDENTITY_ASSERTION_JWT);
        }
    }
}
