package org.keycloak.tests.oauth.tokenexchange;

import java.util.Map;

import org.keycloak.common.Profile;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.oidc.TokenMetadataRepresentation;
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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Legacy token exchange (V1) of a subject token bound to a transient copy of an online user session
 * ({@code ONLINE_TRANSIENT_CLIENT}, for example an RPT or a token issued by the standard token exchange) must keep the
 * session id, otherwise the exchanged token is rejected at introspection.
 * <p>
 * See https://github.com/keycloak/keycloak/issues/55617 and the same check in V2 (#42565).
 */
@KeycloakIntegrationTest(config = LegacyTokenExchangeTransientClientSessionTest.ServerConfig.class)
public class LegacyTokenExchangeTransientClientSessionTest {

    private static final String SUBJECT_CLIENT = "subject-client";
    private static final String STANDARD_CLIENT = "standard-client";
    private static final String LEGACY_CLIENT = "legacy-client";
    private static final String SECRET = "secret";

    @InjectRealm(config = RealmConf.class)
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauth;

    @Test
    public void legacyExchangeOfTransientClientSessionTokenKeepsSessionId() throws Exception {
        // user login: online user session
        oauth.client(SUBJECT_CLIENT, SECRET);
        AccessTokenResponse login = oauth.doPasswordGrantRequest("john", "password");
        assertEquals(200, login.getStatusCode());
        String sessionId = oauth.parseToken(login.getAccessToken(), AccessToken.class).getSessionId();
        assertNotNull(sessionId);

        // standard token exchange by a client without a client session: ONLINE_TRANSIENT_CLIENT token with the session id
        AccessTokenResponse standard = oauth.tokenExchangeRequest(login.getAccessToken())
                .client(STANDARD_CLIENT, SECRET)
                .send();
        assertEquals(200, standard.getStatusCode());
        AccessToken standardToken = oauth.parseToken(standard.getAccessToken(), AccessToken.class);
        assertTrue(standardToken.getId().startsWith("nt"), "expected an ONLINE_TRANSIENT_CLIENT token: " + standardToken.getId());
        assertEquals(sessionId, standardToken.getSessionId());

        // legacy token exchange of that token: it must keep the session id and be active
        AccessTokenResponse legacy = oauth.tokenExchangeRequest(standard.getAccessToken())
                .client(LEGACY_CLIENT, SECRET)
                .audience(LEGACY_CLIENT)
                .requestedTokenType("urn:ietf:params:oauth:token-type:access_token")
                .send();
        assertEquals(200, legacy.getStatusCode());
        AccessToken legacyToken = oauth.parseToken(legacy.getAccessToken(), AccessToken.class);
        assertEquals(sessionId, legacyToken.getSessionId(), "the legacy exchange removed the session id");

        oauth.client(LEGACY_CLIENT, SECRET);
        TokenMetadataRepresentation introspection = oauth.doIntrospectionAccessTokenRequest(legacy.getAccessToken()).asTokenMetadata();
        assertTrue(introspection.isActive(), "the token issued by the legacy exchange is inactive at introspection");
    }

    public static class ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.TOKEN_EXCHANGE, Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ);
        }
    }

    public static class RealmConf implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            // the login token has the standard client in its audience, so that the standard client can exchange it
            realm.clients(ClientBuilder.create().clientId(SUBJECT_CLIENT)
                    .secret(SECRET)
                    .directAccessGrantsEnabled(true)
                    .protocolMappers(audienceMapper(STANDARD_CLIENT)));
            // standard token exchange enabled; its tokens have the legacy client in their audience
            realm.clients(ClientBuilder.create().clientId(STANDARD_CLIENT)
                    .secret(SECRET)
                    .attribute("standard.token.exchange.enabled", "true")
                    .protocolMappers(audienceMapper(LEGACY_CLIENT)));
            // standard token exchange not enabled: its exchanges are served by the legacy token exchange; its tokens
            // have itself in their audience, so that it can introspect them
            realm.clients(ClientBuilder.create().clientId(LEGACY_CLIENT)
                    .secret(SECRET)
                    .protocolMappers(audienceMapper(LEGACY_CLIENT)));
            realm.users(UserBuilder.create("john")
                    .name("John", "Doe")
                    .email("john@localhost")
                    .emailVerified(true)
                    .password("password"));
            return realm;
        }

        private static ProtocolMapperRepresentation audienceMapper(String audience) {
            ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
            mapper.setName("audience-" + audience);
            mapper.setProtocol("openid-connect");
            mapper.setProtocolMapper("oidc-audience-mapper");
            mapper.setConfig(Map.of("included.client.audience", audience, "access.token.claim", "true",
                    "introspection.token.claim", "true"));
            return mapper;
        }
    }
}
