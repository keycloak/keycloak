package org.keycloak.tests.oauth.tokenexchange;


import java.util.logging.Logger;

import org.keycloak.OAuthErrorException;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
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

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests")
    RunOnServerClient runOnServer;

    @InjectSimpleHttp
    SimpleHttp simpleHttp;

    public static class SSODomainConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name("ssodomain");
        
            // test client for a Resource Authorization Server in IdP
            realm.clients(ClientBuilder.create().clientId("https://login.saas-tool.example/")
                    .secret("password")
                    .redirectUris("https://login.saas-tool.example/callback"));

            
            // test client to request the Token exchange in ssodomain
            realm.clients(ClientBuilder.create().clientId("agent")
                    .secret("password")
                    .redirectUris("*")
                    .attribute("access.token.lifespan", "-1")
                    .attribute("standard.token.exchange.enabled", "true"));

            realm.clients(ClientBuilder.create().clientId("wrong_agent")
                    .secret("password")
                    .redirectUris("*")
                    .attribute("standard.token.exchange.enabled", "true"));
  

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
    public void testTokenExchangeSuceedsWithValidIDJAG() throws Exception {

        try {
            ssodomain.admin().logoutAll();
        } catch (Exception e) {
            LOG.log(java.util.logging.Level.WARNING, "Failed to logout all sessions via Admin API", e);
        }

        final String targetAudience = "https://login.saas-tool.example/";
        final String expectedClientId = "agent-at-todo";
        final String scopeName = "idjag-saas-scope";
        final String oidcProtocol = org.keycloak.protocol.oidc.OIDCLoginProtocol.LOGIN_PROTOCOL;

        java.net.URI baseUri = java.net.URI.create(oauth.getRedirectUri());
        final String testHostAndPort = baseUri.getScheme() + "://" + baseUri.getAuthority();
        final String exactRedirectUri  =  oauth.getRedirectUri();

        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName("ssodomain");
            ClientModel requestingClient = realm.getClientByClientId("agent");
            ClientModel targetClient = realm.getClientByClientId(targetAudience);

            requestingClient.addRedirectUri(exactRedirectUri);

            org.keycloak.models.RoleModel targetRole = targetClient.getRole("target-role");
            if (targetRole == null) {
                targetRole = targetClient.addRole("target-role");
            }

            UserModel user = session.users().getUserByUsername(realm, "testuser");
            if (user != null && !user.hasRole(targetRole)) {
                user.grantRole(targetRole);
            }

            org.keycloak.models.ClientScopeModel clientScope = org.keycloak.models.utils.KeycloakModelUtils.getClientScopeByName(realm, scopeName);
            if (clientScope == null) {
                clientScope = realm.addClientScope(scopeName);
                clientScope.setProtocol(oidcProtocol);
            }

            clientScope.addScopeMapping(targetRole);

            // Hardcoded Claim Mapper (Add to ID token: true)
            if (clientScope.getProtocolMapperByName(oidcProtocol, "hardcoded-client-id") == null) {
                org.keycloak.models.ProtocolMapperModel clientIdMapper = new org.keycloak.models.ProtocolMapperModel();
                clientIdMapper.setName("hardcoded-client-id");
                clientIdMapper.setProtocol(oidcProtocol);
                clientIdMapper.setProtocolMapper(org.keycloak.protocol.oidc.mappers.HardcodedClaim.PROVIDER_ID);
                
                java.util.Map<String, String> config = new java.util.HashMap<>();
                config.put("claim.name", "client_id");
                config.put("claim.value", expectedClientId);
                config.put("jsonType.label", "String");
                config.put("id.token.claim", "true");
                config.put("access.token.claim", "true");
                config.put("userinfo.token.claim", "false");
                clientIdMapper.setConfig(config);

                clientScope.addProtocolMapper(clientIdMapper);
            }

            // Pairwise Sub Mapper (SHA-256)
            if (clientScope.getProtocolMapperByName(oidcProtocol, "pairwise-sub-mapper") == null) {
                org.keycloak.models.ProtocolMapperModel pairwiseMapper = new org.keycloak.models.ProtocolMapperModel();
                pairwiseMapper.setName("pairwise-sub-mapper");
                pairwiseMapper.setProtocol(oidcProtocol);
                pairwiseMapper.setProtocolMapper(new org.keycloak.protocol.oidc.mappers.SHA256PairwiseSubMapper().getId());

                java.util.Map<String, String> config = new java.util.HashMap<>();
                config.put(org.keycloak.protocol.oidc.mappers.PairwiseSubMapperHelper.SECTOR_IDENTIFIER_URI,
                    testHostAndPort + "/sector");
                config.put(org.keycloak.protocol.oidc.mappers.PairwiseSubMapperHelper.PAIRWISE_SUB_ALGORITHM_SALT,
                    "agent-salt");
                pairwiseMapper.setConfig(config);

                clientScope.addProtocolMapper(pairwiseMapper);
            }

            // assigned to Requesting Client ("agent") as Default Client Scope
            requestingClient.addClientScope(clientScope, true);

            if (targetClient.getProtocolMapperByName(oidcProtocol, "target-pairwise-sub-mapper") == null) {
                org.keycloak.models.ProtocolMapperModel targetPairwiseMapper = new org.keycloak.models.ProtocolMapperModel();
                targetPairwiseMapper.setName("target-pairwise-sub-mapper");
                targetPairwiseMapper.setProtocol(oidcProtocol);
                targetPairwiseMapper.setProtocolMapper(new org.keycloak.protocol.oidc.mappers.SHA256PairwiseSubMapper().getId());

                java.util.Map<String, String> targetConfig = new java.util.HashMap<>();
                targetConfig.put(org.keycloak.protocol.oidc.mappers.PairwiseSubMapperHelper.SECTOR_IDENTIFIER_URI,
                    "https://login.saas-tool.example/sector");
                targetConfig.put(org.keycloak.protocol.oidc.mappers.PairwiseSubMapperHelper.PAIRWISE_SUB_ALGORITHM_SALT,
                    "agent-salt");
                targetPairwiseMapper.setConfig(targetConfig);

                targetClient.addProtocolMapper(targetPairwiseMapper);
            }

        });


        // do a login in ssodomain to obtain an access token with agent
        oauth.realm("ssodomain").client("agent", "password").redirectUri(exactRedirectUri).openLoginForm();
        oauth.fillLoginForm("testuser", "password");
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse.isSuccess());
        JWSInput jws;
        try {
            jws = new JWSInput(tokenResponse.getIdToken());
        } catch (JWSInputException e) {
            throw new RuntimeException("The provided assertion is not a valid JWT");
        }

        String subject_of_idtoken = jws.readJsonContent(org.keycloak.representations.IDToken.class).getSubject();

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

        JsonWebToken idjag = oauth.parseToken(tokenResponse.getAccessToken(), JsonWebToken.class);
        Assertions.assertEquals("agent-at-todo", idjag.getOtherClaims().get("client_id"));

        String exchangedSub = idjag.getSubject();
        Assertions.assertNotNull(exchangedSub);
        Assertions.assertNotEquals(subject_of_idtoken, exchangedSub,
            "Target client must receive a different pairwise subject");
        Assertions.assertFalse(exchangedSub.isBlank());

        Assertions.assertEquals("https://login.saas-tool.example/", idjag.getAudience()[0]);
        Assertions.assertEquals("read:something", idjag.getOtherClaims().get("scope"));
        Assertions.assertEquals(oauth.getEndpoints().getIssuer(), idjag.getIssuer());
        Assertions.assertTrue(idjag.getExp() > Time.currentTimeSeconds(), "ID-JAG must not be immediately expired");
        
        try {
            JWSInput idJagJws = new JWSInput(tokenResponse.getAccessToken());
            Assertions.assertEquals(jws.getHeader().getAlgorithm(), idJagJws.getHeader().getAlgorithm());
            jws = idJagJws;
        } catch (JWSInputException e) {
            throw new RuntimeException("The provided assertion is not a valid JWT");
        }

        String jwtTokenType = jws.getHeader().getType();
        Assertions.assertEquals("oauth-id-jag+jwt", jwtTokenType);

    }

    @Test
    public void testTokenExchangeFailsForInvalidClient() throws Exception {

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
    public void testTokenExchangeFailsForClientSessionNotFound() throws Exception {

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

        String sessionId = tokenResponse_for_wrong_agent.getSessionState();

        runOnServer.run(session -> {
            org.keycloak.models.RealmModel realm = session.realms().getRealmByName("ssodomain");
            org.keycloak.models.UserSessionModel userSession = session.sessions().getUserSession(realm, sessionId);
            org.keycloak.models.ClientModel client = realm.getClientByClientId("wrong_agent");
            
            if (userSession != null && client != null) {
                userSession.removeAuthenticatedClientSessions(java.util.Collections.singleton(client.getId()));
            }
        });
        
        tokenResponse = oauth.tokenExchangeRequest(tokenResponse_for_wrong_agent.getIdToken(),"urn:ietf:params:oauth:token-type:id_token")
                .audience("https://login.saas-tool.example/")
                .requestedTokenType("urn:ietf:params:oauth:token-type:id-jag")
                .scope("read:something")
                .send();

        Assertions.assertFalse(tokenResponse.isSuccess());
        Assertions.assertEquals(400, tokenResponse.getStatusCode());
        Assertions.assertNull(tokenResponse.getAccessToken());
        Assertions.assertEquals("invalid_token", tokenResponse.getError());
        Assertions.assertEquals("Client session not found or revoked", tokenResponse.getErrorDescription());
    }

    @Test
    public void testTokenExchangeFailsForUnsupportedSubjectTokenType() throws Exception {

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
    public void testTokenExchangeFailsForInvalidSubjectToken() throws Exception {

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
    public void testTokenExchangeFailsForUnsupportedRequestedTokenType() throws Exception {
 
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
    public void testTokenExchangeFailsForInvalidAudience() throws Exception {

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
        Assertions.assertEquals("Client not found for audience parameter: https://wrong-login.saas-tool.example/", tokenResponse.getErrorDescription());
    }

    @Test
    public void testTokenExchangeFailsForInvalidClientSession() throws Exception {
        try {
            ssodomain.admin().logoutAll();
        } catch (Exception e) {
            LOG.log(java.util.logging.Level.WARNING, "Failed to logout all sessions via Admin API", e);
        }

        oauth.realm("ssodomain").client("agent", "password").openLoginForm();
        oauth.fillLoginForm("testuser", "password");
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse.isSuccess());

        String sessionState = tokenResponse.getSessionState();
        runOnServer.run(session -> {
            RealmModel realmModel = session.realms().getRealmByName("ssodomain");
            if (realmModel != null) {
                ClientModel client = realmModel.getClientByClientId("agent");
                UserSessionModel userSession = session.sessions().getUserSession(realmModel, sessionState);
                if (userSession != null && client != null) {
                    AuthenticatedClientSessionModel clientSession =
                        userSession.getAuthenticatedClientSessionByClient(client.getId());
                    if (clientSession != null) {
                        clientSession.setAction(AuthenticatedClientSessionModel.Action.LOGGED_OUT.name());
                        clientSession.setTimestamp(0);
                        clientSession.setNote(AuthenticatedClientSessionModel.STARTED_AT_NOTE, "0");
                    }
                }
            }
        });

        tokenResponse = oauth.tokenExchangeRequest(tokenResponse.getIdToken(), "urn:ietf:params:oauth:token-type:id_token")
            .audience("https://login.saas-tool.example/")
            .requestedTokenType("urn:ietf:params:oauth:token-type:id-jag")
            .scope("read:something")
            .send();

        Assertions.assertFalse(tokenResponse.isSuccess());
        Assertions.assertEquals(400, tokenResponse.getStatusCode());
        Assertions.assertNull(tokenResponse.getAccessToken());
        Assertions.assertEquals("invalid_token", tokenResponse.getError());
        Assertions.assertEquals("Client session not found or revoked", tokenResponse.getErrorDescription());
    }

    @Test
    public void testTokenExchangeWithUnsupportedActorToken() throws Exception {
        try {
            ssodomain.admin().logoutAll();
        } catch (Exception e) {
            LOG.log(java.util.logging.Level.WARNING, "Failed to logout all sessions via Admin API", e);
        }
        
        oauth.realm("ssodomain").client("agent", "password").openLoginForm();
        oauth.fillLoginForm("testuser", "password");
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse.isSuccess());
        
        String dummyActorToken = tokenResponse.getIdToken();
        
        try {
            AccessTokenResponse exchangeResponse = oauth.tokenExchangeRequest(tokenResponse.getIdToken(), "urn:ietf:params:oauth:token-type:id_token")
                .audience("https://saas-tool.example")
                .requestedTokenType("urn:ietf:params:oauth:token-type:id-jag")
                .scope("read:something")
                .actorToken(dummyActorToken)
                .actorTokenType("urn:ietf:params:oauth:token-type:id_token")
                .send();
                
            Assertions.assertFalse(exchangeResponse.isSuccess());
            Assertions.assertEquals(OAuthErrorException.INVALID_REQUEST, exchangeResponse.getError());
            Assertions.assertTrue(exchangeResponse.getErrorDescription().contains("Actor tokens are not supported for ID-JAG token exchange"));
        
        } catch (Exception e) {
            LOG.log(java.util.logging.Level.WARNING, "Failed to execute actor token test", e);
            Assertions.fail("Exception occurred during actor token test: " + e.getMessage());
        }
    }

    public static class JWTAuthorizationGrantServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.IDENTITY_ASSERTION_JWT);
        }
    }
}
