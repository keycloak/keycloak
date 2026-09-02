package org.keycloak.ssf.transmitter;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.ssf.SsfException;
import org.keycloak.ssf.transmitter.support.SsfUtil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SsfTransmitter#getReceiverClient}, which resolves a
 * receiver client for programmatic emit callers and throws a descriptive
 * {@link SsfException} for every rejection path.
 *
 * <p>Order matters: the receiver-capability check runs before the client
 * on/off check, so a plain (non-receiver) client — even a disabled one —
 * gets the accurate "not an SSF Receiver" diagnostic instead of being
 * steered toward enabling a client that would still be rejected. Only an
 * actual receiver that happens to be disabled gets the "is disabled"
 * message.
 */
class SsfTransmitterGetReceiverClientTest {

    private static final String CLIENT_ID = "my-receiver";
    private static final String REALM_NAME = "test-realm";

    private static KeycloakSession sessionWithClient(ClientModel client) {
        RealmModel realm = mock(RealmModel.class);
        when(realm.getName()).thenReturn(REALM_NAME);
        when(realm.getClientByClientId(CLIENT_ID)).thenReturn(client);

        KeycloakContext context = mock(KeycloakContext.class);
        when(context.getRealm()).thenReturn(realm);

        KeycloakSession session = mock(KeycloakSession.class);
        when(session.getContext()).thenReturn(context);
        return session;
    }

    private static ClientModel client(boolean ssfEnabled, boolean clientEnabled) {
        ClientModel client = mock(ClientModel.class);
        when(client.getClientId()).thenReturn(CLIENT_ID);
        when(client.getAttribute(SsfUtil.SSF_ENABLED_KEY)).thenReturn(Boolean.toString(ssfEnabled));
        when(client.isEnabled()).thenReturn(clientEnabled);
        return client;
    }

    @Test
    void noSuchClient_throwsNotFoundMessage() {
        KeycloakSession session = sessionWithClient(null);

        SsfException ex = assertThrows(SsfException.class,
                () -> SsfTransmitter.getReceiverClient(session, CLIENT_ID));
        assertTrue(ex.getMessage().contains("No client with clientId"),
                () -> "unexpected message: " + ex.getMessage());
    }

    @Test
    void notAReceiver_throwsNotAReceiverMessage() {
        KeycloakSession session = sessionWithClient(client(false, true));

        SsfException ex = assertThrows(SsfException.class,
                () -> SsfTransmitter.getReceiverClient(session, CLIENT_ID));
        assertTrue(ex.getMessage().contains("is not an SSF Receiver"),
                () -> "unexpected message: " + ex.getMessage());
    }

    @Test
    void disabledNonReceiver_reportsNotAReceiverNotDisabled() {
        // The key ordering case: a disabled client that is ALSO not a
        // receiver must report "not a receiver" — steering the caller to
        // enable it would be misleading, since it would still be rejected.
        KeycloakSession session = sessionWithClient(client(false, false));

        SsfException ex = assertThrows(SsfException.class,
                () -> SsfTransmitter.getReceiverClient(session, CLIENT_ID));
        assertTrue(ex.getMessage().contains("is not an SSF Receiver"),
                () -> "a disabled non-receiver should report not-a-receiver, not disabled: " + ex.getMessage());
    }

    @Test
    void disabledReceiver_throwsDisabledMessage() {
        KeycloakSession session = sessionWithClient(client(true, false));

        SsfException ex = assertThrows(SsfException.class,
                () -> SsfTransmitter.getReceiverClient(session, CLIENT_ID));
        assertTrue(ex.getMessage().contains("is disabled"),
                () -> "an actual receiver that is disabled should report disabled: " + ex.getMessage());
    }

    @Test
    void enabledReceiver_returnsClient() {
        ClientModel client = client(true, true);
        KeycloakSession session = sessionWithClient(client);

        assertSame(client, SsfTransmitter.getReceiverClient(session, CLIENT_ID),
                "an enabled, configured receiver should be returned");
    }

    @Test
    void noSuchClient_messageNamesTheRealm() {
        KeycloakSession session = sessionWithClient(null);

        SsfException ex = assertThrows(SsfException.class,
                () -> SsfTransmitter.getReceiverClient(session, CLIENT_ID));
        assertEquals("No client with clientId '" + CLIENT_ID + "' in realm '" + REALM_NAME + "'",
                ex.getMessage());
    }
}
