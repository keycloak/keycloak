package org.keycloak.tests.oauth.tokenexchange;


import java.util.logging.Logger;

import org.keycloak.OAuthErrorException;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.testframework.annotations.InjectRealm;
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
import org.junit.jupiter.api.BeforeEach;
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

    @BeforeEach
    public void configureAgentRedirectUri() {
        String redirectUri = oauth.getRedirectUri();
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName("ssodomain");
            ClientModel requestingClient = realm.getClientByClientId("agent");
            requestingClient.setRedirectUris(java.util.Collections.singleton(redirectUri));
        });
    }

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
                    .password("password")
                    .realmRoles(org.keycloak.models.Constants.OFFLINE_ACCESS_ROLE));

            return realm;

        }
    }

    @Test
    public void testTokenExchangeSucceedsWithValidIDJAG() throws Exception {

        try {
            ssodomain.admin().logoutAll();
        } catch (Exception e) {
            LOG.log(java.util.logging.Level.WARNING, "Failed to logout all sessions via Admin API", e);
        }

        configureIdJagClientScope();

        // do a login in ssodomain to obtain an access token with agent
        oauth.realm("ssodomain").client("agent", "password")/* .redirectUri(exactRedirectUri)*/.openLoginForm();
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
        Assertions.assertTrue(tokenResponse.getScope().contains("read:something"));
        Assertions.assertTrue(tokenResponse.getScope().contains("idjag-saas-scope"));

        JsonWebToken idjag = oauth.parseToken(tokenResponse.getAccessToken(), JsonWebToken.class);
        Assertions.assertEquals("agent-at-todo", idjag.getOtherClaims().get("client_id"));

        // The sub of the ID-JAG comes from the protocol mappers applied in the exchange, not from the ID token.
        // Here the pairwise mapper is in a default client scope, so the same sector is used at login and in the
        // exchange, and both subjects are equal. Keeping sub consistent with the Resource AS (no pairwise, or the
        // same sector for all clients that take part in ID-JAG) is the administrator's responsibility.
        Assertions.assertNotEquals(subject_of_idtoken, ssodomain.admin().users().search("testuser").get(0).getId(),
            "The pairwise mapper must have been applied to the ID token subject");
        String exchangedSub = idjag.getSubject();
        Assertions.assertNotNull(exchangedSub);
        Assertions.assertEquals(subject_of_idtoken, exchangedSub,
            "The ID-JAG subject must be produced by the pairwise mapper of the default client scope");
        Assertions.assertFalse(exchangedSub.isBlank());

        Assertions.assertEquals("https://login.saas-tool.example/", idjag.getAudience()[0]);
        Assertions.assertTrue(idjag.getOtherClaims().get("scope").toString().contains("read:something"));
        Assertions.assertTrue(idjag.getOtherClaims().get("scope").toString().contains("idjag-saas-scope"));
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
        AccessTokenResponse tokenResponseForWrongAgent = oauth2.doAccessTokenRequest(oauth2.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponseForWrongAgent.isSuccess());

        oauth.realm("ssodomain").client("agent", "password").openLoginForm();
        oauth.fillLoginForm("testuser", "password");
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse.isSuccess());

        tokenResponse = oauth.tokenExchangeRequest(tokenResponseForWrongAgent.getIdToken(),"urn:ietf:params:oauth:token-type:id_token")
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
        AccessTokenResponse tokenResponseForWrongAgent = oauth2.doAccessTokenRequest(oauth2.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponseForWrongAgent.isSuccess());

        oauth.realm("ssodomain").client("agent", "password").openLoginForm();
        oauth.fillLoginForm("testuser", "password");
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse.isSuccess());

        String sessionId = tokenResponseForWrongAgent.getSessionState();

        runOnServer.run(session -> {
            org.keycloak.models.RealmModel realm = session.realms().getRealmByName("ssodomain");
            org.keycloak.models.UserSessionModel userSession = session.sessions().getUserSession(realm, sessionId);
            org.keycloak.models.ClientModel client = realm.getClientByClientId("wrong_agent");

            if (userSession != null && client != null) {
                userSession.removeAuthenticatedClientSessions(java.util.Collections.singleton(client.getId()));
            }
        });

        tokenResponse = oauth.tokenExchangeRequest(tokenResponseForWrongAgent.getIdToken(),"urn:ietf:params:oauth:token-type:id_token")
                .audience("https://login.saas-tool.example/")
                .requestedTokenType("urn:ietf:params:oauth:token-type:id-jag")
                .scope("read:something")
                .send();

        Assertions.assertFalse(tokenResponse.isSuccess());
        Assertions.assertEquals(400, tokenResponse.getStatusCode());
        Assertions.assertNull(tokenResponse.getAccessToken());
        Assertions.assertEquals("invalid_request", tokenResponse.getError());
        Assertions.assertEquals("Invalid token", tokenResponse.getErrorDescription());

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
        Assertions.assertEquals("Invalid subject token type", tokenResponse.getErrorDescription());
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
        Assertions.assertEquals("invalid_request", tokenResponse.getError());
        Assertions.assertEquals("Invalid token", tokenResponse.getErrorDescription());
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
        Assertions.assertEquals("invalid_client", tokenResponse.getError());
        Assertions.assertEquals("Audience not found", tokenResponse.getErrorDescription());
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
        Assertions.assertEquals("invalid_request", tokenResponse.getError());
        Assertions.assertEquals("Invalid token", tokenResponse.getErrorDescription());
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

    }

    @Test
    public void testTokenExchangeFailsForLoggingOutUserSession() throws Exception {
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

        // Simulate a frontchannel logout in progress: only the user session state is changed,
        // the session timestamps are left untouched so that the session is still within its lifespan.
        runOnServer.run(session -> {
            RealmModel realmModel = session.realms().getRealmByName("ssodomain");
            UserSessionModel userSession = session.sessions().getUserSession(realmModel, sessionState);
            Assertions.assertNotNull(userSession);
            userSession.setState(UserSessionModel.State.LOGGING_OUT);
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
    public void testTokenExchangeFailsForLoggingOutClientSession() throws Exception {
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

        // Simulate a frontchannel logout in progress for the token holder client: only the client session
        // action is changed, the session timestamps are left untouched so that the session is still within its lifespan.
        runOnServer.run(session -> {
            RealmModel realmModel = session.realms().getRealmByName("ssodomain");
            ClientModel client = realmModel.getClientByClientId("agent");
            UserSessionModel userSession = session.sessions().getUserSession(realmModel, sessionState);
            Assertions.assertNotNull(userSession);
            AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());
            Assertions.assertNotNull(clientSession);
            clientSession.setAction(AuthenticatedClientSessionModel.Action.LOGGING_OUT.name());
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
    public void testTokenExchangeFailsForLoggedOutUnconfirmedUserSession() throws Exception {
        try {
            ssodomain.admin().logoutAll();
        } catch (Exception e) {
            LOG.log(java.util.logging.Level.WARNING, "Failed to logout all sessions via Admin API", e);
        }

        // Configure the ID-JAG issuance, so that without the state check the exchange would succeed
        configureIdJagClientScope();

        oauth.realm("ssodomain").client("agent", "password").openLoginForm();
        oauth.fillLoginForm("testuser", "password");
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse.isSuccess());

        String sessionState = tokenResponse.getSessionState();

        // Simulate a browser logout waiting for the confirmation of another client: the user session is kept
        // in LOGGED_OUT_UNCONFIRMED, while the client session of the requesting client is unchanged.
        // The session timestamps are left untouched, so that the session is still within its lifespan.
        runOnServer.run(session -> {
            RealmModel realmModel = session.realms().getRealmByName("ssodomain");
            UserSessionModel userSession = session.sessions().getUserSession(realmModel, sessionState);
            Assertions.assertNotNull(userSession);
            userSession.setState(UserSessionModel.State.LOGGED_OUT_UNCONFIRMED);
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
    public void testTokenExchangeFailsForSymmetricSigningAlgorithm() throws Exception {
        try {
            ssodomain.admin().logoutAll();
        } catch (Exception e) {
            LOG.log(java.util.logging.Level.WARNING, "Failed to logout all sessions via Admin API", e);
        }

        configureIdJagClientScope();

        // Log in first, so that the subject ID token itself is still signed with the default asymmetric algorithm
        oauth.realm("ssodomain").client("agent", "password").openLoginForm();
        oauth.fillLoginForm("testuser", "password");
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse.isSuccess());

        // Select a symmetric ID token signing algorithm for the requesting client.
        // Its key is not published in the realm's JWKS, so it must not be used for the ID-JAG.
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName("ssodomain");
            ClientModel requestingClient = realm.getClientByClientId("agent");
            requestingClient.setAttribute(org.keycloak.protocol.oidc.OIDCConfigAttributes.ID_TOKEN_SIGNED_RESPONSE_ALG,
                    org.keycloak.crypto.Algorithm.HS256);
        });

        try {
            tokenResponse = oauth.tokenExchangeRequest(tokenResponse.getIdToken(), "urn:ietf:params:oauth:token-type:id_token")
                .audience("https://login.saas-tool.example/")
                .requestedTokenType("urn:ietf:params:oauth:token-type:id-jag")
                .scope("read:something")
                .send();

            Assertions.assertFalse(tokenResponse.isSuccess());
            Assertions.assertEquals(400, tokenResponse.getStatusCode());
            Assertions.assertNull(tokenResponse.getAccessToken());
            Assertions.assertEquals("invalid_request", tokenResponse.getError());
            Assertions.assertEquals("ID-JAG requires an asymmetric signing algorithm, but 'HS256' is configured",
                tokenResponse.getErrorDescription());
        } finally {
            // Restore the default, so that the other tests are not affected
            runOnServer.run(session -> {
                RealmModel realm = session.realms().getRealmByName("ssodomain");
                ClientModel requestingClient = realm.getClientByClientId("agent");
                requestingClient.removeAttribute(org.keycloak.protocol.oidc.OIDCConfigAttributes.ID_TOKEN_SIGNED_RESPONSE_ALG);
            });
        }
    }

    @Test
    public void testTokenExchangeFailsForOfflineSessionAfterLogout() throws Exception {
        try {
            ssodomain.admin().logoutAll();
        } catch (Exception e) {
            LOG.log(java.util.logging.Level.WARNING, "Failed to logout all sessions via Admin API", e);
        }

        try {
            configureIdJagClientScope();
            // Log in with offline_access, so that both an online and an offline user session are created
            oauth.realm("ssodomain").client("agent", "password").scope("openid offline_access").openLoginForm();
            oauth.fillLoginForm("testuser", "password");
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
            Assertions.assertTrue(tokenResponse.isSuccess());

            String idToken = tokenResponse.getIdToken();
            String sessionState = tokenResponse.getSessionState();

            // Precondition: after the offline_access login only the offline session is found for this id,
            // so the exchange can only resolve the offline session.
            runOnServer.run(session -> {
                RealmModel realm = session.realms().getRealmByName("ssodomain");
                Assertions.assertNull(session.sessions().getUserSession(realm, sessionState));
                Assertions.assertNotNull(session.sessions().getOfflineUserSession(realm, sessionState));
            });

            tokenResponse = oauth.tokenExchangeRequest(idToken, "urn:ietf:params:oauth:token-type:id_token")
                .audience("https://login.saas-tool.example/")
                .requestedTokenType("urn:ietf:params:oauth:token-type:id-jag")
                .scope("read:something")
                .send();

            Assertions.assertFalse(tokenResponse.isSuccess());
            Assertions.assertEquals(400, tokenResponse.getStatusCode());
            Assertions.assertNull(tokenResponse.getAccessToken());
            Assertions.assertEquals("invalid_token", tokenResponse.getError());
            Assertions.assertEquals("ID-JAG requires an online user session; an ID token bound only to an offline session cannot be used", tokenResponse.getErrorDescription());
        } finally {
            oauth.scope(null);
        }
    }

    @Test
    public void testTokenExchangeFailsForMultipleAudiences() throws Exception {
        try {
            ssodomain.admin().logoutAll();
        } catch (Exception e) {
            LOG.log(java.util.logging.Level.WARNING, "Failed to logout all sessions via Admin API", e);
        }

        oauth.realm("ssodomain").client("agent", "password").openLoginForm();
        oauth.fillLoginForm("testuser", "password");
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse.isSuccess());

        // The ID-JAG is issued for a single Resource Authorization Server
        tokenResponse = oauth.tokenExchangeRequest(tokenResponse.getIdToken(), "urn:ietf:params:oauth:token-type:id_token")
            .audience("https://login.saas-tool.example/", "https://other.saas-tool.example/")
            .requestedTokenType("urn:ietf:params:oauth:token-type:id-jag")
            .scope("read:something")
            .send();

        Assertions.assertFalse(tokenResponse.isSuccess());
        Assertions.assertEquals(400, tokenResponse.getStatusCode());
        Assertions.assertNull(tokenResponse.getAccessToken());
        Assertions.assertEquals("invalid_request", tokenResponse.getError());
        Assertions.assertEquals("The ID-JAG token exchange requires exactly one audience", tokenResponse.getErrorDescription());
    }

    @Test
    public void testTokenExchangeFailsForResourceParameter() throws Exception {
        try {
            ssodomain.admin().logoutAll();
        } catch (Exception e) {
            LOG.log(java.util.logging.Level.WARNING, "Failed to logout all sessions via Admin API", e);
        }

        oauth.realm("ssodomain").client("agent", "password").openLoginForm();
        oauth.fillLoginForm("testuser", "password");
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse.isSuccess());

        // Resource indicators are not supported yet, so the request must be rejected
        // instead of issuing an ID-JAG without the requested resource restriction
        tokenResponse = oauth.tokenExchangeRequest(tokenResponse.getIdToken(), "urn:ietf:params:oauth:token-type:id_token")
            .audience("https://login.saas-tool.example/")
            .resource("https://api.saas-tool.example/")
            .requestedTokenType("urn:ietf:params:oauth:token-type:id-jag")
            .scope("read:something")
            .send();

        Assertions.assertFalse(tokenResponse.isSuccess());
        Assertions.assertEquals(400, tokenResponse.getStatusCode());
        Assertions.assertNull(tokenResponse.getAccessToken());
        Assertions.assertEquals("invalid_request", tokenResponse.getError());
        Assertions.assertEquals("Resource parameters are not supported for ID-JAG token exchange", tokenResponse.getErrorDescription());
    }

    @Test
    public void testTokenExchangeFailsForMissingClientIdClaim() throws Exception {
        try {
            ssodomain.admin().logoutAll();
        } catch (Exception e) {
            LOG.log(java.util.logging.Level.WARNING, "Failed to logout all sessions via Admin API", e);
    }

        configureIdJagClientScope();

        oauth.realm("ssodomain").client("agent", "password").openLoginForm();
        oauth.fillLoginForm("testuser", "password");
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse.isSuccess());

        // Stop adding client_id to the access token, so that the evaluated token has no client_id
        updateClientIdMapper(java.util.Map.of("access.token.claim", "false"));
        try {
            tokenResponse = oauth.tokenExchangeRequest(tokenResponse.getIdToken(), "urn:ietf:params:oauth:token-type:id_token")
                .audience("https://login.saas-tool.example/")
                .requestedTokenType("urn:ietf:params:oauth:token-type:id-jag")
                .scope("read:something")
                .send();

            Assertions.assertFalse(tokenResponse.isSuccess());
            Assertions.assertEquals(400, tokenResponse.getStatusCode());
            Assertions.assertNull(tokenResponse.getAccessToken());
            Assertions.assertEquals("invalid_request", tokenResponse.getError());
            Assertions.assertTrue(tokenResponse.getErrorDescription().startsWith("Claim 'client_id' must be a nonblank string in ID-JAG"));
        } finally {
            updateClientIdMapper(java.util.Map.of("access.token.claim", "true"));
        }
    }

    @Test
    public void testTokenExchangeFailsForNonStringClientIdClaim() throws Exception {
        try {
            ssodomain.admin().logoutAll();
        } catch (Exception e) {
            LOG.log(java.util.logging.Level.WARNING, "Failed to logout all sessions via Admin API", e);
        }

        configureIdJagClientScope();

        oauth.realm("ssodomain").client("agent", "password").openLoginForm();
        oauth.fillLoginForm("testuser", "password");
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse.isSuccess());

        // Produce a numeric client_id
        updateClientIdMapper(java.util.Map.of("jsonType.label", "long", "claim.value", "123"));
        try {
            tokenResponse = oauth.tokenExchangeRequest(tokenResponse.getIdToken(), "urn:ietf:params:oauth:token-type:id_token")
                .audience("https://login.saas-tool.example/")
                .requestedTokenType("urn:ietf:params:oauth:token-type:id-jag")
                .scope("read:something")
                .send();

            Assertions.assertFalse(tokenResponse.isSuccess());
            Assertions.assertEquals(400, tokenResponse.getStatusCode());
            Assertions.assertNull(tokenResponse.getAccessToken());
            Assertions.assertEquals("invalid_request", tokenResponse.getError());
            Assertions.assertTrue(tokenResponse.getErrorDescription().startsWith("Claim 'client_id' must be a nonblank string in ID-JAG"));
        } finally {
            updateClientIdMapper(java.util.Map.of("jsonType.label", "String", "claim.value", "agent-at-todo"));
        }
    }

    @Test
    public void testTokenExchangeFailsForMissingSubjectClaim() throws Exception {
        try {
            ssodomain.admin().logoutAll();
        } catch (Exception e) {
            LOG.log(java.util.logging.Level.WARNING, "Failed to logout all sessions via Admin API", e);
        }

        configureIdJagClientScope();

        // Log in first, so that the subject ID token itself still has a sub
        oauth.realm("ssodomain").client("agent", "password").openLoginForm();
        oauth.fillLoginForm("testuser", "password");
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse.isSuccess());

        // Remove every mapper that produces sub for the requesting client: the pairwise mapper of the
        // ID-JAG scope (which always sets sub) and the sub mapper of the basic scope
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName("ssodomain");
            ClientModel agent = realm.getClientByClientId("agent");
            org.keycloak.models.ClientScopeModel idjagScope = org.keycloak.models.utils.KeycloakModelUtils.getClientScopeByName(realm, "idjag-saas-scope");
            idjagScope.removeProtocolMapper(idjagScope.getProtocolMapperByName(
                org.keycloak.protocol.oidc.OIDCLoginProtocol.LOGIN_PROTOCOL, "pairwise-sub-mapper"));
            agent.removeClientScope(org.keycloak.models.utils.KeycloakModelUtils.getClientScopeByName(realm, "basic"));
        });

        try {
            tokenResponse = oauth.tokenExchangeRequest(tokenResponse.getIdToken(), "urn:ietf:params:oauth:token-type:id_token")
                .audience("https://login.saas-tool.example/")
                .requestedTokenType("urn:ietf:params:oauth:token-type:id-jag")
                .scope("read:something")
                .send();

            Assertions.assertFalse(tokenResponse.isSuccess());
            Assertions.assertEquals(400, tokenResponse.getStatusCode());
            Assertions.assertNull(tokenResponse.getAccessToken());
            Assertions.assertEquals("invalid_request", tokenResponse.getError());
            Assertions.assertTrue(tokenResponse.getErrorDescription().startsWith("Claim 'sub' is missing in ID-JAG"));
        } finally {
            // Restore the basic scope; configureIdJagClientScope() recreates the pairwise mapper with the same salt
            runOnServer.run(session -> {
                RealmModel realm = session.realms().getRealmByName("ssodomain");
                ClientModel agent = realm.getClientByClientId("agent");
                agent.addClientScope(org.keycloak.models.utils.KeycloakModelUtils.getClientScopeByName(realm, "basic"), true);
            });
            configureIdJagClientScope();
        }
    }

    @Test
    public void testTokenExchangeFailsWhenRejectedByClientPolicy() throws Exception {
        try {
            ssodomain.admin().logoutAll();
        } catch (Exception e) {
            LOG.log(java.util.logging.Level.WARNING, "Failed to logout all sessions via Admin API", e);
        }

        configureIdJagClientScope();

        // A client policy that rejects only the token exchange response. The ID-JAG exchange must trigger
        // the client policies when the response is built, like the standard token exchange.
        ssodomain.updateWithCleanup(r -> r
            .resetClientProfiles()
            .clientProfile(org.keycloak.testframework.realm.ClientProfileBuilder.create()
                .name("reject-token-exchange-response")
                .executor(org.keycloak.tests.providers.client.policies.RejectTokenExchangeResponseExecutor.PROVIDER_ID, null)
                .build())
            .resetClientPolicies()
            .clientPolicy(org.keycloak.testframework.realm.ClientPolicyBuilder.create()
                .name("reject-token-exchange-response")
                .condition(org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory.PROVIDER_ID,
                    new org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation())
                .profile("reject-token-exchange-response")
                .build()));

        oauth.realm("ssodomain").client("agent", "password").openLoginForm();
        oauth.fillLoginForm("testuser", "password");
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse.isSuccess());

        tokenResponse = oauth.tokenExchangeRequest(tokenResponse.getIdToken(), "urn:ietf:params:oauth:token-type:id_token")
            .audience("https://login.saas-tool.example/")
            .requestedTokenType("urn:ietf:params:oauth:token-type:id-jag")
            .scope("read:something")
            .send();

        Assertions.assertFalse(tokenResponse.isSuccess());
        Assertions.assertEquals(400, tokenResponse.getStatusCode());
        Assertions.assertNull(tokenResponse.getAccessToken());
        Assertions.assertEquals("invalid_request", tokenResponse.getError());
        Assertions.assertEquals(org.keycloak.tests.providers.client.policies.RejectTokenExchangeResponseExecutor.ERROR_DESCRIPTION,
            tokenResponse.getErrorDescription());
    }

    public static class JWTAuthorizationGrantServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.IDENTITY_ASSERTION_JWT)
                .dependency("org.keycloak.tests", "keycloak-tests-custom-providers");
        }
    }

    /**
     * Configures the client scope, mappers and roles that are required to issue an ID-JAG for the "agent" client.
     * The configuration is idempotent, so it can be called from every test that needs to reach the ID-JAG issuance.
     */
    private void configureIdJagClientScope() {
        final String targetAudience = "https://login.saas-tool.example/";
        final String expectedClientId = "agent-at-todo";
        final String scopeName = "idjag-saas-scope";
        final String oidcProtocol = org.keycloak.protocol.oidc.OIDCLoginProtocol.LOGIN_PROTOCOL;

        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName("ssodomain");
            ClientModel requestingClient = realm.getClientByClientId("agent");
            ClientModel targetClient = realm.getClientByClientId(targetAudience);

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
                config.put(org.keycloak.protocol.oidc.mappers.PairwiseSubMapperHelper.PAIRWISE_SUB_ALGORITHM_SALT,
                    "agent-salt");
                config.put("access.token.claim", "true");
                config.put("id.token.claim", "true");
                pairwiseMapper.setConfig(config);

                clientScope.addProtocolMapper(pairwiseMapper);
            }

            // assigned to Requesting Client ("agent") as Default Client Scope
            requestingClient.addClientScope(clientScope, true);

            org.keycloak.models.ClientScopeModel readScope = org.keycloak.models.utils.KeycloakModelUtils.getClientScopeByName(realm, "read:something");
            if (readScope == null) {
                readScope = realm.addClientScope("read:something");
                readScope.setProtocol(oidcProtocol);
            }
            requestingClient.addClientScope(readScope, false);
        });
    }

    /**
     * Updates the config of the hardcoded client_id mapper of the ID-JAG client scope.
     */
    private void updateClientIdMapper(java.util.Map<String, String> changes) {
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName("ssodomain");
            org.keycloak.models.ClientScopeModel idjagScope = org.keycloak.models.utils.KeycloakModelUtils.getClientScopeByName(realm, "idjag-saas-scope");
            org.keycloak.models.ProtocolMapperModel mapper = idjagScope.getProtocolMapperByName(
                org.keycloak.protocol.oidc.OIDCLoginProtocol.LOGIN_PROTOCOL, "hardcoded-client-id");
            mapper.getConfig().putAll(changes);
            idjagScope.updateProtocolMapper(mapper);
        });
    }
}
