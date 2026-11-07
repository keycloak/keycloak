package org.keycloak.protocol.oidc.grants.ciba.channel;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HttpAuthenticationChannelProviderTest {

    private static final String SERVER_URI = "https://server.example.com/ciba";
    private static final String REALM_URI = "https://realm.example.com/ciba";

    @Test
    public void usesServerUriIfRealmUriNotSet() {
        assertEquals(SERVER_URI, HttpAuthenticationChannelProvider.resolveHttpAuthenticationChannelUri(null, SERVER_URI, true));
        assertEquals(SERVER_URI, HttpAuthenticationChannelProvider.resolveHttpAuthenticationChannelUri("  ", SERVER_URI, true));
    }

    @Test
    public void usesRealmUriIfOverrideAllowed() {
        assertEquals(REALM_URI, HttpAuthenticationChannelProvider.resolveHttpAuthenticationChannelUri(REALM_URI, SERVER_URI, true));
        assertEquals(REALM_URI, HttpAuthenticationChannelProvider.resolveHttpAuthenticationChannelUri(REALM_URI, null, true));
    }

    @Test
    public void ignoresRealmUriIfOverrideNotAllowed() {
        assertEquals(SERVER_URI, HttpAuthenticationChannelProvider.resolveHttpAuthenticationChannelUri(REALM_URI, SERVER_URI, false));
    }

    @Test
    public void validatesUri() {
        assertTrue(HttpAuthenticationChannelProvider.isValidHttpAuthenticationChannelUri("https://realm.example.com/ciba"));
        assertTrue(HttpAuthenticationChannelProvider.isValidHttpAuthenticationChannelUri("http://localhost:8500/ciba/request-authentication-channel"));

        assertFalse(HttpAuthenticationChannelProvider.isValidHttpAuthenticationChannelUri(null));
        assertFalse(HttpAuthenticationChannelProvider.isValidHttpAuthenticationChannelUri(""));
        assertFalse(HttpAuthenticationChannelProvider.isValidHttpAuthenticationChannelUri("/relative/path"));
        assertFalse(HttpAuthenticationChannelProvider.isValidHttpAuthenticationChannelUri("ftp://realm.example.com/ciba"));
        assertFalse(HttpAuthenticationChannelProvider.isValidHttpAuthenticationChannelUri("file:///etc/passwd"));
        assertFalse(HttpAuthenticationChannelProvider.isValidHttpAuthenticationChannelUri("https:///no-host"));
        assertFalse(HttpAuthenticationChannelProvider.isValidHttpAuthenticationChannelUri("https://exa mple.com"));
    }
}
