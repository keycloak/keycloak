package org.keycloak.ssf.transmitter.event;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.keycloak.events.Event;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.ssf.event.caep.CaepCredentialChange;
import org.keycloak.ssf.event.caep.CaepSessionRevoked;
import org.keycloak.ssf.event.token.SsfSecurityEventToken;
import org.keycloak.ssf.transmitter.SsfTransmitterProvider;
import org.keycloak.ssf.transmitter.stream.StreamConfig;
import org.keycloak.ssf.transmitter.stream.storage.client.ClientStreamStore;
import org.keycloak.storage.ReadOnlyException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@code ssf.emitOnlyEvents} per-receiver gate
 * implemented by {@link SsfTransmitterEventListener#isEmitOnlyEventForReceiver}
 * and the token-level {@link SsfTransmitterEventListener#isAnyEventEmitOnlyForReceiver}
 * helper that the listener uses at the call site.
 *
 * <p>The gate runs in the native-listener path only. When the resolved
 * SSF event-type URI on a generated token appears in the stream's
 * {@code emitOnlyEvents} set, the listener drops the token for that
 * stream — operators must instead fire the event explicitly through
 * the synthetic-emit endpoint, which deliberately bypasses this gate.
 *
 * <p>Tests construct the listener with a {@code null} session because
 * the predicate doesn't touch it; only its arguments matter.
 */
class SsfTransmitterEventListenerTest {

    private final SsfTransmitterEventListener listener = new SsfTransmitterEventListener(null);

    @Test
    void emitOnlySetIsNull_doesNotSkip() {
        StreamConfig stream = new StreamConfig();
        // emitOnlyEvents intentionally left null

        assertFalse(listener.isEmitOnlyEventForReceiver(CaepSessionRevoked.TYPE, stream),
                "null emit-only set is the default — every event should be auto-emittable");
    }

    @Test
    void emitOnlySetIsEmpty_doesNotSkip() {
        StreamConfig stream = new StreamConfig();
        stream.setEmitOnlyEvents(Set.of());

        assertFalse(listener.isEmitOnlyEventForReceiver(CaepSessionRevoked.TYPE, stream),
                "empty emit-only set behaves the same as the default");
    }

    @Test
    void eventTypeInEmitOnlySet_skips() {
        StreamConfig stream = new StreamConfig();
        stream.setEmitOnlyEvents(Set.of(CaepSessionRevoked.TYPE));

        assertTrue(listener.isEmitOnlyEventForReceiver(CaepSessionRevoked.TYPE, stream),
                "the ASM use case: session-revoked is supported but must not auto-emit on every Keycloak logout");
    }

    @Test
    void eventTypeNotInEmitOnlySet_doesNotSkip() {
        StreamConfig stream = new StreamConfig();
        // emit-only blocks session-revoked but not credential-change
        stream.setEmitOnlyEvents(Set.of(CaepSessionRevoked.TYPE));

        assertFalse(listener.isEmitOnlyEventForReceiver(CaepCredentialChange.TYPE, stream),
                "credential-change is not in the emit-only set, so it must still auto-emit");
    }

    @Test
    void tokenWithMultipleEvents_skipsWhenAnyMatches() {
        StreamConfig stream = new StreamConfig();
        stream.setEmitOnlyEvents(Set.of(CaepSessionRevoked.TYPE));

        // Tokens almost always carry exactly one event in practice, but the
        // shape of SsfSecurityEventToken.getEvents() is a Map keyed by URI,
        // so guard the iteration: a single match anywhere is enough to skip.
        SsfSecurityEventToken token = eventToken(CaepCredentialChange.TYPE);
        token.getEvents().put(CaepSessionRevoked.TYPE, new CaepSessionRevoked());

        assertTrue(listener.isAnyEventEmitOnlyForReceiver(token, stream),
                "a token carrying any emit-only event type must be dropped — partial delivery would be confusing");
    }

    @Test
    void tokenWithNoEvents_doesNotSkip() {
        StreamConfig stream = new StreamConfig();
        stream.setEmitOnlyEvents(Set.of(CaepSessionRevoked.TYPE));

        SsfSecurityEventToken token = new SsfSecurityEventToken();
        // events map left null — defensive, since real generated tokens
        // always carry at least one event entry

        assertFalse(listener.isAnyEventEmitOnlyForReceiver(token, stream),
                "a token without any events has nothing to filter on; the predicate should be a no-op");
    }

    /**
     * Builds a minimal token carrying a single SSF event of the given type
     * keyed in the {@code events} map by its URI — the exact shape the
     * dispatcher hands to {@code isAnyEventEmitOnlyForReceiver}.
     */
    private SsfSecurityEventToken eventToken(String eventTypeUri) {
        SsfSecurityEventToken token = new SsfSecurityEventToken();
        token.setJti("jti-test");
        // The events map is Map<String, Object> on the parent class
        // (Jackson loose-typing) — only the keys are inspected by the
        // emit-only predicate, so the value can be any non-null
        // marker. We pick the typed event class for readability.
        Map<String, Object> events = new LinkedHashMap<>();
        events.put(eventTypeUri, eventForType(eventTypeUri));
        token.setEvents(events);
        return token;
    }

    private Object eventForType(String eventTypeUri) {
        if (CaepSessionRevoked.TYPE.equals(eventTypeUri)) {
            return new CaepSessionRevoked();
        }
        if (CaepCredentialChange.TYPE.equals(eventTypeUri)) {
            return new CaepCredentialChange();
        }
        throw new IllegalArgumentException("Test fixture only covers CAEP session-revoked / credential-change. Add a branch when extending.");
    }

    // ----- auto-notify-on-login read-only handling -----

    private static final String RECEIVER_CLIENT_ID = "receiver";
    private static final String LOGIN_USER_ID = "user-1";

    @Test
    void autoNotifyOnLogin_swallowsReadOnlyException() {
        KeycloakSession session = mockLoginSession();
        SsfTransmitterProvider transmitter = mock(SsfTransmitterProvider.class);
        AtomicBoolean attempted = new AtomicBoolean(false);

        // A user backed by a read-only store (e.g. LDAP edit mode READ_ONLY,
        // or import disabled): markAsNotified throws ReadOnlyException. The
        // listener must swallow it so login isn't disrupted.
        SsfTransmitterEventListener readOnlyListener = new SsfTransmitterEventListener(session) {
            @Override
            protected boolean isUserNotified(SsfTransmitterProvider t, UserModel u, ClientModel c) {
                return false;
            }
            @Override
            protected boolean isAnyOrganizationNotified(SsfTransmitterProvider t, UserModel u, ClientModel c) {
                return false;
            }
            @Override
            protected void markAsNotified(UserModel u, ClientModel c) {
                attempted.set(true);
                throw new ReadOnlyException("user is read-only");
            }
        };

        assertDoesNotThrow(() -> readOnlyListener.autoNotifyOnLogin(loginEvent(), transmitter),
                "a read-only user store must not break login — the write is skipped, not propagated");
        assertTrue(attempted.get(),
                "the listener should have attempted the write before swallowing the exception");
    }

    @Test
    void autoNotifyOnLogin_propagatesNonReadOnlyExceptions() {
        KeycloakSession session = mockLoginSession();
        SsfTransmitterProvider transmitter = mock(SsfTransmitterProvider.class);

        // The catch is narrow: only ReadOnlyException is swallowed. Any
        // other failure must still surface so genuine bugs aren't hidden.
        SsfTransmitterEventListener faultyListener = new SsfTransmitterEventListener(session) {
            @Override
            protected boolean isUserNotified(SsfTransmitterProvider t, UserModel u, ClientModel c) {
                return false;
            }
            @Override
            protected boolean isAnyOrganizationNotified(SsfTransmitterProvider t, UserModel u, ClientModel c) {
                return false;
            }
            @Override
            protected void markAsNotified(UserModel u, ClientModel c) {
                throw new IllegalStateException("boom");
            }
        };

        assertThrows(IllegalStateException.class,
                () -> faultyListener.autoNotifyOnLogin(loginEvent(), transmitter),
                "only ReadOnlyException is swallowed; other failures must surface");
    }

    /**
     * Wires a session whose realm hosts an SSF receiver client with
     * {@code ssf.enabled=true} and {@code ssf.autoNotifyOnLogin=true},
     * resolving {@link #LOGIN_USER_ID} to a (mock) user — enough for
     * {@code autoNotifyOnLogin} to reach the {@code markAsNotified} write.
     */
    private KeycloakSession mockLoginSession() {
        KeycloakSession session = mock(KeycloakSession.class);
        KeycloakContext context = mock(KeycloakContext.class);
        RealmModel realm = mock(RealmModel.class);
        ClientModel client = mock(ClientModel.class);
        UserModel user = mock(UserModel.class);
        UserProvider users = mock(UserProvider.class);

        when(session.getContext()).thenReturn(context);
        when(context.getRealm()).thenReturn(realm);
        when(realm.getClientByClientId(RECEIVER_CLIENT_ID)).thenReturn(client);
        when(client.getAttribute(ClientStreamStore.SSF_ENABLED_KEY)).thenReturn("true");
        when(client.getAttribute(ClientStreamStore.SSF_AUTO_NOTIFY_ON_LOGIN_KEY)).thenReturn("true");
        // SSF_DEFAULT_SUBJECTS_KEY left unstubbed (null) → not broadcast (ALL).
        when(session.users()).thenReturn(users);
        when(users.getUserById(realm, LOGIN_USER_ID)).thenReturn(user);
        return session;
    }

    private Event loginEvent() {
        Event event = new Event();
        event.setClientId(RECEIVER_CLIENT_ID);
        event.setUserId(LOGIN_USER_ID);
        return event;
    }
}
