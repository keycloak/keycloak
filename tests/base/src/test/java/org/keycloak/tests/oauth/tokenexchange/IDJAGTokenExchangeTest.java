package org.keycloak.tests.oauth.tokenexchange;


import java.io.IOException;
import java.util.Collections;
import java.util.logging.Logger;

import org.keycloak.OAuthErrorException;
import org.keycloak.common.Profile;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.IDJAG;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
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
        Assertions.assertEquals(400, tokenResponse.getStatusCode());
        Assertions.assertNull(tokenResponse.getAccessToken());
        Assertions.assertEquals("invalid_token", tokenResponse.getError());
        Assertions.assertEquals("Client session not found or revoked", tokenResponse.getErrorDescription());
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

    @Test
    public void IDJAGIssuance_invalid_client_session() throws Exception {
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
    public void testTokenExchangeWithAmbiguousAudienceIdentifier() throws IOException {
        String sharedIdentifier = "https://example.com";
        String attributeKey = "idjag.resource.authorization.server.identifier";
        
        try {
            ssodomain.admin().logoutAll();
        } catch (Exception e) {
            LOG.log(java.util.logging.Level.WARNING, "Failed to logout all sessions via Admin API", e);
        }
        
        ClientRepresentation clientA = new ClientRepresentation();
        clientA.setClientId("ambiguous-client-a");
        clientA.setEnabled(true);
        clientA.setAttributes(Collections.singletonMap(attributeKey, sharedIdentifier));
        jakarta.ws.rs.core.Response responseA = ssodomain.admin().clients().create(clientA);
        Assertions.assertEquals(201, responseA.getStatus());
        responseA.close();
        
        ClientRepresentation clientB = new ClientRepresentation();
        clientB.setClientId("ambiguous-client-b");
        clientB.setEnabled(true);
        clientB.setAttributes(Collections.singletonMap(attributeKey, sharedIdentifier));
        jakarta.ws.rs.core.Response responseB = ssodomain.admin().clients().create(clientB);
        Assertions.assertEquals(201, responseB.getStatus());
        responseB.close();
        
        try {
            oauth.realm("ssodomain").client("agent", "password").openLoginForm();
            oauth.fillLoginForm("testuser", "password");
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
            Assertions.assertTrue(tokenResponse.isSuccess());
            
            AccessTokenResponse exchangeResponse = oauth.tokenExchangeRequest(tokenResponse.getIdToken(), "urn:ietf:params:oauth:token-type:id_token")
                .audience(sharedIdentifier)
                .requestedTokenType("urn:ietf:params:oauth:token-type:id-jag")
                .scope("read:something")
                .send();

            Assertions.assertFalse(exchangeResponse.isSuccess());
            Assertions.assertEquals(400, exchangeResponse.getStatusCode());
            Assertions.assertEquals(OAuthErrorException.INVALID_REQUEST, exchangeResponse.getError());
            Assertions.assertTrue(exchangeResponse.getErrorDescription().contains("Ambiguous target client"));
        } catch (Exception e) {
            LOG.log(java.util.logging.Level.WARNING, "Failed to execute token exchange test", e);
            Assertions.fail("Exception occurred during token exchange test: " + e.getMessage());
        } finally {
            ssodomain.admin().clients().findByClientId("ambiguous-client-a").forEach(c -> ssodomain.admin().clients().get(c.getId()).remove());
            ssodomain.admin().clients().findByClientId("ambiguous-client-b").forEach(c -> ssodomain.admin().clients().get(c.getId()).remove());
        }
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

    @Test
    public void testTokenExchangeFailsForDynamicallyRegisteredClient() throws Exception {
        try {
            ssodomain.admin().logoutAll();
        } catch (Exception e) {
            LOG.log(java.util.logging.Level.WARNING, "Failed to logout all sessions via Admin API", e);
        }
    
        String dynamicClientName = "dynamic-registered-agent";
        
        OIDCClientRepresentation dynamicClient = new OIDCClientRepresentation();

        dynamicClient.setClientName(dynamicClientName);
        dynamicClient.setRedirectUris(Collections.singletonList("*"));
        dynamicClient.setGrantTypes(java.util.Arrays.asList("authorization_code", "password", "client_credentials"));
        
        ClientInitialAccessCreatePresentation iatReq = new ClientInitialAccessCreatePresentation(0, 10);
        ClientInitialAccessPresentation iatRes = ssodomain.admin().clientInitialAccess().create(iatReq);
        String adminToken = iatRes.getToken(); 

        SimpleHttpRequest request = simpleHttp.doPost(ssodomain.getBaseUrl() + "/clients-registrations/openid-connect")
            .json(dynamicClient);
        request.auth(adminToken);
        
        OIDCClientRepresentation createdClient;
        
        try (SimpleHttpResponse response = request.asResponse()) {
            Assertions.assertEquals(201, response.getStatus());
            createdClient = response.asJson(OIDCClientRepresentation.class);
        }
        
        String dynamicClientSecret = createdClient.getClientSecret();
        String generatedClientId = createdClient.getClientId(); 
        String internalId = ssodomain.admin().clients().findByClientId(generatedClientId).get(0).getId();

        ClientRepresentation adminClientRep = ssodomain.admin().clients().get(internalId).toRepresentation();
        java.util.Map<String, String> attributes = adminClientRep.getAttributes();
        if (attributes == null) attributes = new java.util.HashMap<>();
        attributes.put("standard.token.exchange.enabled", "true");
        adminClientRep.setAttributes(attributes);

        ssodomain.admin().clients().get(internalId).update(adminClientRep);
        
        try {
            oauth.realm("ssodomain").client(generatedClientId, dynamicClientSecret).openLoginForm();
            oauth.fillLoginForm("testuser", "password");
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
            Assertions.assertTrue(tokenResponse.isSuccess());

            AccessTokenResponse exchangeResponse = oauth.tokenExchangeRequest(tokenResponse.getIdToken(), "urn:ietf:params:oauth:token-type:id-jag")
                .audience("dummy-audience") 
                .requestedTokenType("urn:ietf:params:oauth:token-type:id-jag")
                .scope("read:something")
                .send();

            Assertions.assertFalse(exchangeResponse.isSuccess(), "Security Vulnerability: Dynamically registered client was allowed to use ID-JAG exchange!");
            Assertions.assertEquals(OAuthErrorException.INVALID_CLIENT, exchangeResponse.getError());
            Assertions.assertTrue(
                exchangeResponse.getErrorDescription().equals("Dynamically registered clients are not allowed"),
                "Expected 'Dynamically registered clients' error message but got: " + exchangeResponse.getErrorDescription()
            );
        
        } catch (Exception e) {
            LOG.log(java.util.logging.Level.WARNING, "Failed to execute dynamic client block test", e);
            Assertions.fail("Exception occurred during dynamic client block test: " + e.getMessage());
        } finally {
            ssodomain.admin().clients().findByClientId(generatedClientId)
            .forEach(c -> ssodomain.admin().clients().get(c.getId()).remove());
        }
    }

    @Test
    public void testTokenExchangeFailsWhenTargetClientIsDynamicallyRegistered() throws Exception {
        try {
            ssodomain.admin().logoutAll();
        } catch (Exception e) {
            LOG.log(java.util.logging.Level.WARNING, "Failed to logout all sessions via Admin API", e);
        }

        String dynamicTargetClientId = "dynamic-target-server";
        String sharedIdentifier = "https://example.com";
        String attributeKey = "idjag.resource.authorization.server.identifier";

        OIDCClientRepresentation dynamicTargetClient = new OIDCClientRepresentation();
        dynamicTargetClient.setClientName(dynamicTargetClientId);
        dynamicTargetClient.setRedirectUris(Collections.singletonList("*"));
        dynamicTargetClient.setGrantTypes(java.util.Arrays.asList("authorization_code", "password", "client_credentials", "urn:ietf:params:oauth:grant-type:jwt-bearer"));

        ClientInitialAccessCreatePresentation iatReq = new ClientInitialAccessCreatePresentation(0, 10);
        ClientInitialAccessPresentation iatRes = ssodomain.admin().clientInitialAccess().create(iatReq);
        String adminToken = iatRes.getToken();

        SimpleHttpRequest request = simpleHttp.doPost(ssodomain.getBaseUrl() + "/clients-registrations/openid-connect")
            .json(dynamicTargetClient);
        request.auth(adminToken);
        OIDCClientRepresentation createdClient;
        try (SimpleHttpResponse response = request.asResponse()) {
            Assertions.assertEquals(201, response.getStatus());
            createdClient = response.asJson(OIDCClientRepresentation.class);
        }
    
        String dynamicClientSecret = createdClient.getClientSecret();
        String generatedClientId = createdClient.getClientId(); 
        String internalId = ssodomain.admin().clients().findByClientId(generatedClientId).get(0).getId();

        ClientRepresentation adminClientRep = ssodomain.admin().clients().get(internalId).toRepresentation();
        java.util.Map<String, String> attributes = adminClientRep.getAttributes();
        if (attributes == null) attributes = new java.util.HashMap<>();
        attributes.put(attributeKey, sharedIdentifier);
        adminClientRep.setAttributes(attributes);

        ssodomain.admin().clients().get(internalId).update(adminClientRep);

        
        String agentInternalId = ssodomain.admin().clients().findByClientId("agent").get(0).getId();
        
        ClientRepresentation agentRep = ssodomain.admin().clients().get(agentInternalId).toRepresentation();
        java.util.Map<String, String> agentAttributes = agentRep.getAttributes();
        if (agentAttributes == null) {
            agentAttributes = new java.util.HashMap<>();
        }
        agentAttributes.put("idjag.clientid.at." + generatedClientId, "agent-at-" + dynamicTargetClientId);
        agentAttributes.put("idjag.permitted.scopes.at." + generatedClientId, "read:something write:something");
        agentRep.setAttributes(agentAttributes);

        ssodomain.admin().clients().get(agentInternalId).update(agentRep);

        try {
            oauth.realm("ssodomain").client("agent", "password").openLoginForm();
            oauth.fillLoginForm("testuser", "password");
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
            Assertions.assertTrue(tokenResponse.isSuccess());

            AccessTokenResponse exchangeResponse = oauth.tokenExchangeRequest(tokenResponse.getIdToken(), "urn:ietf:params:oauth:token-type:id_token")
                .audience(sharedIdentifier) // 動的登録されたクライアントの識別子を指定
                .requestedTokenType("urn:ietf:params:oauth:token-type:id-jag")
                .scope("read:something")
                .send();

            Assertions.assertFalse(exchangeResponse.isSuccess(), "Security Vulnerability: Dynamically registered client was allowed to use ID-JAG exchange!");
            Assertions.assertEquals(OAuthErrorException.INVALID_TARGET, exchangeResponse.getError());
            Assertions.assertTrue(exchangeResponse.getErrorDescription().contains("disabled for dynamically registered target clients"));
            Assertions.assertTrue(
                exchangeResponse.getErrorDescription().equals("ID-JAG token exchange is disabled for dynamically registered target clients"),
                "Expected 'Dynamically registered clients' error message but got: " + exchangeResponse.getErrorDescription()
            );

        } catch (Exception e) {
            LOG.log(java.util.logging.Level.WARNING, "Failed to execute dynamic target client block test", e);
            Assertions.fail("Exception occurred during dynamic target client block test: " + e.getMessage());
        } finally {
            ssodomain.admin().clients().findByClientId(generatedClientId)
                .forEach(c -> ssodomain.admin().clients().get(c.getId()).remove());
        }
    }

    public static class JWTAuthorizationGrantServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.IDENTITY_ASSERTION_JWT);
        }
    }
}
