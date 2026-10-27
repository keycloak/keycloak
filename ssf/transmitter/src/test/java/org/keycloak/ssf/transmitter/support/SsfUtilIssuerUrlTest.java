package org.keycloak.ssf.transmitter.support;

import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SsfUtil#getIssuerUrl(KeycloakSession)} covering the
 * {@code frontendUrl} realm attribute branch (keycloak/keycloak#53107).
 *
 * <p>The other branches (KC_HOSTNAME_URL env var, hostname SPI config, and
 * request base URI) are not exercised here because they rely on
 * {@link System#getenv} and {@link org.keycloak.Config} which are not
 * practical to override in a plain unit test. The key invariant being
 * verified is that the {@code frontendUrl} branch — previously the only one
 * that did <em>not</em> append the realm path — now produces an issuer URL
 * that is consistent with the other branches and with the realm's OIDC issuer.
 */
class SsfUtilIssuerUrlTest {

    private static KeycloakSession sessionWithFrontendUrl(String realmName, String frontendUrl) {
        RealmModel realm = mock(RealmModel.class);
        when(realm.getName()).thenReturn(realmName);
        when(realm.getAttribute("frontendUrl")).thenReturn(frontendUrl);

        KeycloakContext context = mock(KeycloakContext.class);
        when(context.getRealm()).thenReturn(realm);

        KeycloakSession session = mock(KeycloakSession.class);
        when(session.getContext()).thenReturn(context);

        return session;
    }

    @Test
    void frontendUrl_withoutTrailingSlash_appendsRealmPath() {
        KeycloakSession session = sessionWithFrontendUrl("myrealm", "https://example.com");
        assertEquals("https://example.com/realms/myrealm", SsfUtil.getIssuerUrl(session),
                "frontendUrl without trailing slash should have /realms/<realm> appended");
    }

    @Test
    void frontendUrl_withTrailingSlash_appendsRealmPath() {
        KeycloakSession session = sessionWithFrontendUrl("myrealm", "https://example.com/");
        assertEquals("https://example.com/realms/myrealm", SsfUtil.getIssuerUrl(session),
                "frontendUrl with trailing slash should have realms/<realm> appended (no double slash)");
    }

    @Test
    void frontendUrl_withSubPath_appendsRealmPath() {
        KeycloakSession session = sessionWithFrontendUrl("testrealm", "https://example.com/auth");
        assertEquals("https://example.com/auth/realms/testrealm", SsfUtil.getIssuerUrl(session),
                "frontendUrl with a sub-path should still have /realms/<realm> appended");
    }

    @Test
    void frontendUrl_realmNameWithSpaces_encodesRealmPath() {
        KeycloakSession session = sessionWithFrontendUrl("my realm", "https://example.com");
        assertEquals("https://example.com/realms/my%20realm", SsfUtil.getIssuerUrl(session),
                "realm names with spaces must be percent-encoded in the issuer URL");
    }

    @Test
    void frontendUrl_blank_fallsThrough() {
        // A blank frontendUrl must not be used — the method should fall through
        // to the next branch. In a plain unit test the env/config fallbacks are
        // absent too, so the method throws IllegalStateException. We only verify
        // it does NOT return the blank value as the issuer.
        RealmModel realm = mock(RealmModel.class);
        when(realm.getName()).thenReturn("myrealm");
        when(realm.getAttribute("frontendUrl")).thenReturn("   ");

        KeycloakContext context = mock(KeycloakContext.class);
        when(context.getRealm()).thenReturn(realm);
        // context.getUri() is null → getBaseUri() throws RuntimeException,
        // which is caught, so the method reaches the final IllegalStateException.

        KeycloakSession session = mock(KeycloakSession.class);
        when(session.getContext()).thenReturn(context);

        try {
            String result = SsfUtil.getIssuerUrl(session);
            // If somehow a result is returned it must not be the blank string
            org.junit.jupiter.api.Assertions.assertFalse(result.isBlank(),
                    "a blank frontendUrl must not be returned as the issuer");
        } catch (IllegalStateException expected) {
            // Expected: no hostname configured and no HTTP request context
        }
    }
}
