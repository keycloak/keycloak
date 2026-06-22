/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.oauth;

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
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginPage;
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

    @InjectOAuthClient(ref = "oauth1", webDriverRef = "webDriver1")
    OAuthClient oauth1;

    @InjectOAuthClient(ref = "oauth2", webDriverRef = "webDriver2")
    OAuthClient oauth2;

    @InjectOAuthClient(ref = "oauth3", webDriverRef = "webDriver3")
    OAuthClient oauth3;

    @InjectOAuthClient(ref = "oauth4", webDriverRef = "webDriver4")
    OAuthClient oauth4;

    @InjectOAuthClient(ref = "oauth5", webDriverRef = "webDriver5")
    OAuthClient oauth5;

    @InjectOAuthClient(ref = "oauth6", webDriverRef = "webDriver6")
    OAuthClient oauth6;

    @InjectOAuthClient(ref = "oauth7", webDriverRef = "webDriver7")
    OAuthClient oauth7;

    @InjectOAuthClient(ref = "oauth8", webDriverRef = "webDriver8")
    OAuthClient oauth8;

    @InjectPage(ref = "loginPage1", webDriverRef = "webDriver1")
    LoginPage loginPage1;

    @InjectPage(ref = "loginPage2", webDriverRef = "webDriver2")
    LoginPage loginPage2;

    @InjectPage(ref = "loginPage3", webDriverRef = "webDriver3")
    LoginPage loginPage3;

    @InjectPage(ref = "loginPage4", webDriverRef = "webDriver4")
    LoginPage loginPage4;

    @InjectPage(ref = "loginPage5", webDriverRef = "webDriver5")
    LoginPage loginPage5;

    @InjectPage(ref = "loginPage6", webDriverRef = "webDriver6")
    LoginPage loginPage6;

    @InjectPage(ref = "loginPage7", webDriverRef = "webDriver7")
    LoginPage loginPage7;

    @InjectPage(ref = "loginPage8", webDriverRef = "webDriver8")
    LoginPage loginPage8;

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


        // do a login in ssodomain to obtain an access token with agent
        oauth1.realm("ssodomain").client("agent", "password").openLoginForm();
        loginPage1.fillLogin("testuser", "password");
        loginPage1.submit();
        AccessTokenResponse tokenResponse = oauth1.doAccessTokenRequest(oauth1.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse.isSuccess());

        // perform token exchange to get the assertion grant
        tokenResponse = oauth1.tokenExchangeRequest(tokenResponse.getIdToken(),"urn:ietf:params:oauth:token-type:id_token")
                .audience("https://login.saas-tool.example/")
                .requestedTokenType("urn:ietf:params:oauth:token-type:id-jag")
                .scope("read:something")
                .send();

        Assertions.assertTrue(tokenResponse.isSuccess());
        Assertions.assertNull(tokenResponse.getRefreshToken());
        Assertions.assertEquals("urn:ietf:params:oauth:token-type:id-jag",tokenResponse.getIssuedTokenType());
        Assertions.assertEquals("N_A",tokenResponse.getTokenType());
        Assertions.assertEquals("read:something",tokenResponse.getScope());

        IDJAG idjag = oauth1.parseToken(tokenResponse.getAccessToken(), IDJAG.class);
        Assertions.assertEquals("agent-at-todo", idjag.getClient_id());
        Assertions.assertEquals("https://login.saas-tool.example/", idjag.getAudience()[0]);
        Assertions.assertEquals("read:something", idjag.getScope());
        Assertions.assertNotNull(idjag.getSessionId());
        Assertions.assertEquals("http://localhost:8080/realms/ssodomain", idjag.getIssuer());
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
        oauth2.realm("ssodomain").client("agent", "password").openLoginForm();
        loginPage2.fillLogin("testuser", "password");
        loginPage2.submit();
        AccessTokenResponse tokenResponse = oauth2.doAccessTokenRequest(oauth2.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse.isSuccess());

        tokenResponse = oauth2.tokenExchangeRequest(tokenResponse.getIdToken(),"urn:ietf:params:oauth:token-type:id_token")
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
        oauth3.realm("ssodomain").client("wrong_agent", "password").openLoginForm();
        loginPage3.fillLogin("testuser", "password");
        loginPage3.submit();
        AccessTokenResponse tokenResponse3 = oauth3.doAccessTokenRequest(oauth3.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse3.isSuccess());

        oauth4.realm("ssodomain").client("agent", "password").openLoginForm();
        loginPage4.fillLogin("testuser", "password");
        loginPage4.submit();
        AccessTokenResponse tokenResponse4 = oauth4.doAccessTokenRequest(oauth4.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse4.isSuccess());

        tokenResponse4 = oauth4.tokenExchangeRequest(tokenResponse3.getIdToken(),"urn:ietf:params:oauth:token-type:id_token")
                .audience("https://login.saas-tool.example/")
                .requestedTokenType("urn:ietf:params:oauth:token-type:id-jag")
                .scope("read:something")
                .send();

        Assertions.assertFalse(tokenResponse4.isSuccess());
        Assertions.assertEquals(403, tokenResponse4.getStatusCode());
        Assertions.assertNull(tokenResponse4.getAccessToken());
        Assertions.assertEquals("access_denied", tokenResponse4.getError());
        Assertions.assertEquals("Client is not within the token audience", tokenResponse4.getErrorDescription());
    }

    @Test
    public void IDJAGIssuance_wrong_subject_token_type() throws Exception {
        oauth5.realm("ssodomain").client("agent", "password").openLoginForm();
        loginPage5.fillLogin("testuser", "password");
        loginPage5.submit();
        AccessTokenResponse tokenResponse = oauth5.doAccessTokenRequest(oauth5.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse.isSuccess());

        tokenResponse = oauth5.tokenExchangeRequest(tokenResponse.getIdToken(),"urn:ietf:params:oauth:token-type:id-jag")
                .audience("https://login.saas-tool.example/")
                .requestedTokenType("urn:ietf:params:oauth:token-type:id-jag")
                .scope("read:something")
                .send();
        

        Assertions.assertFalse(tokenResponse.isSuccess());
        Assertions.assertEquals(400, tokenResponse.getStatusCode());
        Assertions.assertNull(tokenResponse.getAccessToken());
        Assertions.assertEquals("invalid_request", tokenResponse.getError());
        Assertions.assertEquals("Parameter 'subject_token' supports access tokens only", tokenResponse.getErrorDescription());
    }

    @Test
    public void IDJAGIssuance_wrong_subject_token() throws Exception {
        oauth6.realm("ssodomain").client("agent", "password").openLoginForm();
        loginPage6.fillLogin("testuser", "password");
        loginPage6.submit();
        AccessTokenResponse tokenResponse = oauth6.doAccessTokenRequest(oauth6.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse.isSuccess());

        tokenResponse = oauth6.tokenExchangeRequest(tokenResponse.getAccessToken(),"urn:ietf:params:oauth:token-type:id_token")
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
    public void IDJAGIssuance_wrong_requested_token_tyoe() throws Exception {
 
        oauth7.realm("ssodomain").client("agent", "password").openLoginForm();
        loginPage7.fillLogin("testuser", "password");
        loginPage7.submit();
        AccessTokenResponse tokenResponse = oauth7.doAccessTokenRequest(oauth7.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse.isSuccess());

        tokenResponse = oauth7.tokenExchangeRequest(tokenResponse.getIdToken(),"urn:ietf:params:oauth:token-type:id_token")
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
 
        oauth8.realm("ssodomain").client("agent", "password").openLoginForm();
        loginPage8.fillLogin("testuser", "password");
        loginPage8.submit();
        AccessTokenResponse tokenResponse = oauth8.doAccessTokenRequest(oauth8.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse.isSuccess());

        tokenResponse = oauth8.tokenExchangeRequest(tokenResponse.getIdToken(),"urn:ietf:params:oauth:token-type:id_token")
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
