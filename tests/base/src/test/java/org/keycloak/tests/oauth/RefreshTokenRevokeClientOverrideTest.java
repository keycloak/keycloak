package org.keycloak.tests.oauth;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.BadRequestException;

import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.tests.common.TestRealmUserConfig;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.tests.oauth.RefreshTokenTest.enableRefreshTokenEvents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the client-level override of the realm settings "Revoke Refresh Token" and "Refresh Token Max Reuse"
 * (client attributes {@link OIDCConfigAttributes#REVOKE_REFRESH_TOKEN} and {@link OIDCConfigAttributes#REFRESH_TOKEN_MAX_REUSE}).
 */
@KeycloakIntegrationTest
public class RefreshTokenRevokeClientOverrideTest {

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectEvents
    Events events;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @InjectRealm(config = RefreshTokenTest.RefreshTokenTestRealmConfig.class)
    ManagedRealm realm;

    @InjectUser(config = TestRealmUserConfig.class)
    ManagedUser user;

    @BeforeEach
    public void before() {
        enableRefreshTokenEvents(realm);
        AccountHelper.logout(realm.admin(), user.getUsername());
    }

    @AfterEach
    public void after() {
        setClientOverride(null, null);
    }

    @Test
    public void clientOverrideEnablesRevocationWhenRealmDisabled() {
        // realm default: revokeRefreshToken=false
        setClientOverride(true, null);

        String sessionId = login();
        AccessTokenResponse response1 = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        RefreshToken refreshToken1 = oauth.parseRefreshToken(response1.getRefreshToken());
        EventAssertion.assertSuccess(events.poll()).type(EventType.CODE_TO_TOKEN);

        // reuse_id claim is only present when revocation is effective for the client
        assertNotNull(refreshToken1.getOtherClaims().get(Constants.REUSE_ID));

        AccessTokenResponse response2 = oauth.doRefreshTokenRequest(response1.getRefreshToken());
        assertEquals(200, response2.getStatusCode());
        RefreshToken refreshToken2 = oauth.parseRefreshToken(response2.getRefreshToken());
        assertEquals(refreshToken1.getOtherClaims().get(Constants.REUSE_ID), refreshToken2.getOtherClaims().get(Constants.REUSE_ID));
        EventAssertion.assertSuccess(events.poll())
                .sessionId(sessionId)
                .details(Details.REFRESH_TOKEN_ID, refreshToken1.getId())
                .type(EventType.REFRESH_TOKEN);

        // Reuse of the old refresh token is rejected
        AccessTokenResponse response3 = oauth.doRefreshTokenRequest(response1.getRefreshToken());
        assertEquals(400, response3.getStatusCode());
        EventAssertion.assertError(events.poll())
                .sessionId(sessionId)
                .details(Details.REFRESH_TOKEN_ID, refreshToken1.getId())
                .type(EventType.REFRESH_TOKEN_ERROR)
                .error("invalid_token");

        // Client session was invalidated, hence the newer refresh token is not valid anymore either
        AccessTokenResponse response4 = oauth.doRefreshTokenRequest(response2.getRefreshToken());
        assertEquals(400, response4.getStatusCode());
        EventAssertion.assertError(events.poll())
                .sessionId(sessionId)
                .details(Details.REFRESH_TOKEN_ID, refreshToken2.getId())
                .type(EventType.REFRESH_TOKEN_ERROR)
                .error("invalid_token");
    }

    @Test
    public void clientOverrideDisablesRevocationWhenRealmEnabled() {
        realm.updateWithCleanup(r -> r.revokeRefreshToken(true));
        setClientOverride(false, null);

        String sessionId = login();
        AccessTokenResponse response1 = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        RefreshToken refreshToken1 = oauth.parseRefreshToken(response1.getRefreshToken());
        EventAssertion.assertSuccess(events.poll()).type(EventType.CODE_TO_TOKEN);

        assertNull(refreshToken1.getOtherClaims().get(Constants.REUSE_ID));

        // Unlimited reuse of the same refresh token is allowed for this client
        for (int i = 0; i < 3; i++) {
            AccessTokenResponse response = oauth.doRefreshTokenRequest(response1.getRefreshToken());
            assertEquals(200, response.getStatusCode());
            EventAssertion.assertSuccess(events.poll())
                    .sessionId(sessionId)
                    .details(Details.REFRESH_TOKEN_ID, refreshToken1.getId())
                    .type(EventType.REFRESH_TOKEN);
        }
    }

    @Test
    public void clientMaxReuseOverridesRealmMaxReuse() {
        // realm: revocation enabled, no reuse allowed
        realm.updateWithCleanup(r -> r.revokeRefreshToken(true).refreshTokenMaxReuse(0));
        // client: inherit revocation, but allow a single reuse
        setClientOverride(null, 1);

        assertSingleReuseAllowed();
    }

    @Test
    public void clientMaxReuseWithClientEnabledRevocation() {
        // realm default: revokeRefreshToken=false, refreshTokenMaxReuse=0
        setClientOverride(true, 1);

        assertSingleReuseAllowed();
    }

    @Test
    public void clientMaxReuseIgnoredWhenRevocationDisabledForClient() {
        realm.updateWithCleanup(r -> r.revokeRefreshToken(true).refreshTokenMaxReuse(0));
        setClientOverride(false, 1);

        String sessionId = login();
        AccessTokenResponse response1 = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        RefreshToken refreshToken1 = oauth.parseRefreshToken(response1.getRefreshToken());
        EventAssertion.assertSuccess(events.poll()).type(EventType.CODE_TO_TOKEN);

        // Max reuse override is irrelevant, as revocation is disabled for the client -> unlimited reuse
        for (int i = 0; i < 3; i++) {
            AccessTokenResponse response = oauth.doRefreshTokenRequest(response1.getRefreshToken());
            assertEquals(200, response.getStatusCode());
            EventAssertion.assertSuccess(events.poll())
                    .sessionId(sessionId)
                    .details(Details.REFRESH_TOKEN_ID, refreshToken1.getId())
                    .type(EventType.REFRESH_TOKEN);
        }
    }

    @Test
    public void introspectionHonoursClientOverride() throws IOException {
        // realm default: revokeRefreshToken=false
        setClientOverride(true, null);

        login();
        AccessTokenResponse response1 = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        events.poll();

        timeOffSet.set(1);

        AccessTokenResponse response2 = oauth.doRefreshTokenRequest(response1.getRefreshToken());
        assertEquals(200, response2.getStatusCode());
        events.poll();

        // The rotated refresh token is active, the used one is not
        assertTrue(oauth.doIntrospectionRefreshTokenRequest(response2.getRefreshToken()).asTokenMetadata().isActive());
        assertFalse(oauth.doIntrospectionRefreshTokenRequest(response1.getRefreshToken()).asTokenMetadata().isActive());
    }

    @Test
    public void introspectionIgnoresReuseWhenRevocationDisabledForClient() throws IOException {
        realm.updateWithCleanup(r -> r.revokeRefreshToken(true));
        setClientOverride(false, null);

        login();
        AccessTokenResponse response1 = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        events.poll();

        timeOffSet.set(1);

        AccessTokenResponse response2 = oauth.doRefreshTokenRequest(response1.getRefreshToken());
        assertEquals(200, response2.getStatusCode());
        events.poll();

        // Both refresh tokens remain active for this client
        assertTrue(oauth.doIntrospectionRefreshTokenRequest(response2.getRefreshToken()).asTokenMetadata().isActive());
        assertTrue(oauth.doIntrospectionRefreshTokenRequest(response1.getRefreshToken()).asTokenMetadata().isActive());
    }

    @Test
    public void invalidOverridesAreRejectedByClientValidation() {
        ClientRepresentation rep = oauth.clientResource().toRepresentation();
        Map<String, String> attributes = rep.getAttributes() != null ? new HashMap<>(rep.getAttributes()) : new HashMap<>();
        rep.setAttributes(attributes);

        attributes.put(OIDCConfigAttributes.REFRESH_TOKEN_MAX_REUSE, "-1");
        assertThrows(BadRequestException.class, () -> oauth.clientResource().update(rep));

        attributes.put(OIDCConfigAttributes.REFRESH_TOKEN_MAX_REUSE, "abc");
        assertThrows(BadRequestException.class, () -> oauth.clientResource().update(rep));

        // Malformed boolean must not be silently interpreted as "false"
        attributes.remove(OIDCConfigAttributes.REFRESH_TOKEN_MAX_REUSE);
        attributes.put(OIDCConfigAttributes.REVOKE_REFRESH_TOKEN, "tru");
        assertThrows(BadRequestException.class, () -> oauth.clientResource().update(rep));

        // Valid values (case-insensitive) are accepted
        attributes.put(OIDCConfigAttributes.REVOKE_REFRESH_TOKEN, "TRUE");
        attributes.put(OIDCConfigAttributes.REFRESH_TOKEN_MAX_REUSE, "0");
        oauth.clientResource().update(rep);
    }

    private void assertSingleReuseAllowed() {
        String sessionId = login();
        AccessTokenResponse initialResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        RefreshToken initialRefreshToken = oauth.parseRefreshToken(initialResponse.getRefreshToken());
        EventAssertion.assertSuccess(events.poll()).type(EventType.CODE_TO_TOKEN);

        assertNotNull(initialRefreshToken.getOtherClaims().get(Constants.REUSE_ID));

        // Initial use
        AccessTokenResponse responseFirstUse = oauth.doRefreshTokenRequest(initialResponse.getRefreshToken());
        assertEquals(200, responseFirstUse.getStatusCode());
        EventAssertion.assertSuccess(events.poll())
                .sessionId(sessionId)
                .details(Details.REFRESH_TOKEN_ID, initialRefreshToken.getId())
                .type(EventType.REFRESH_TOKEN);

        // Single reuse allowed
        AccessTokenResponse responseFirstReuse = oauth.doRefreshTokenRequest(initialResponse.getRefreshToken());
        assertEquals(200, responseFirstReuse.getStatusCode());
        EventAssertion.assertSuccess(events.poll())
                .sessionId(sessionId)
                .details(Details.REFRESH_TOKEN_ID, initialRefreshToken.getId())
                .type(EventType.REFRESH_TOKEN);

        // Second reuse exceeds the limit
        AccessTokenResponse responseSecondReuse = oauth.doRefreshTokenRequest(initialResponse.getRefreshToken());
        assertEquals(400, responseSecondReuse.getStatusCode());
        EventAssertion.assertError(events.poll())
                .sessionId(sessionId)
                .details(Details.REFRESH_TOKEN_ID, initialRefreshToken.getId())
                .type(EventType.REFRESH_TOKEN_ERROR)
                .error("invalid_token");
    }

    private String login() {
        oauth.doLogin("test-user@localhost", "password");
        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent).userId(user.getId());
        return loginEvent.getSessionId();
    }

    /**
     * Sets (or removes, when {@code null}) the client-level overrides on the OAuth test client.
     */
    private void setClientOverride(Boolean revokeRefreshToken, Integer refreshTokenMaxReuse) {
        ClientRepresentation rep = oauth.clientResource().toRepresentation();
        Map<String, String> attributes = rep.getAttributes() != null ? new HashMap<>(rep.getAttributes()) : new HashMap<>();
        // An empty value means "inherit from realm" (and removes the attribute)
        attributes.put(OIDCConfigAttributes.REVOKE_REFRESH_TOKEN, revokeRefreshToken == null ? "" : String.valueOf(revokeRefreshToken));
        attributes.put(OIDCConfigAttributes.REFRESH_TOKEN_MAX_REUSE, refreshTokenMaxReuse == null ? "" : String.valueOf(refreshTokenMaxReuse));
        rep.setAttributes(attributes);
        oauth.clientResource().update(rep);
    }
}
