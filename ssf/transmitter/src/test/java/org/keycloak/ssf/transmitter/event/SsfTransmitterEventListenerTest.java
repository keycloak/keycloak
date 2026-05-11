package org.keycloak.ssf.transmitter.event;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.keycloak.ssf.event.caep.CaepCredentialChange;
import org.keycloak.ssf.event.caep.CaepSessionRevoked;
import org.keycloak.ssf.event.token.SsfSecurityEventToken;
import org.keycloak.ssf.transmitter.stream.StreamConfig;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
