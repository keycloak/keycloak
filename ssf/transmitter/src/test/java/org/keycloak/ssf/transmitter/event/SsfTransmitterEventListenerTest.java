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
 * Unit tests for the {@code ssf.manualOnlyEvents} per-receiver gate
 * implemented by {@link SsfTransmitterEventListener#isManualOnlyForStream}.
 *
 * <p>The gate runs in the native-listener path only. When the resolved
 * SSF event-type URI on a generated token appears in the stream's
 * {@code manualOnlyEvents} set, the listener drops the token for that
 * stream — operators must instead fire the event explicitly through
 * the synthetic-emit endpoint, which deliberately bypasses this gate.
 *
 * <p>Tests construct the listener with a {@code null} session because
 * the predicate doesn't touch it; only its two arguments matter.
 */
class SsfTransmitterEventListenerTest {

    private final SsfTransmitterEventListener listener = new SsfTransmitterEventListener(null);

    @Test
    void manualOnlySetIsNull_doesNotSkip() {
        StreamConfig stream = new StreamConfig();
        // manualOnlyEvents intentionally left null

        assertFalse(listener.isManualOnlyForStream(eventToken(CaepSessionRevoked.TYPE), stream),
                "null manual-only set is the default — every event should be auto-emittable");
    }

    @Test
    void manualOnlySetIsEmpty_doesNotSkip() {
        StreamConfig stream = new StreamConfig();
        stream.setManualOnlyEvents(Set.of());

        assertFalse(listener.isManualOnlyForStream(eventToken(CaepSessionRevoked.TYPE), stream),
                "empty manual-only set behaves the same as the default");
    }

    @Test
    void eventTypeInManualOnlySet_skips() {
        StreamConfig stream = new StreamConfig();
        stream.setManualOnlyEvents(Set.of(CaepSessionRevoked.TYPE));

        assertTrue(listener.isManualOnlyForStream(eventToken(CaepSessionRevoked.TYPE), stream),
                "the ASM use case: session-revoked is supported but must not auto-emit on every Keycloak logout");
    }

    @Test
    void eventTypeNotInManualOnlySet_doesNotSkip() {
        StreamConfig stream = new StreamConfig();
        // manual-only blocks session-revoked but not credential-change
        stream.setManualOnlyEvents(Set.of(CaepSessionRevoked.TYPE));

        assertFalse(listener.isManualOnlyForStream(eventToken(CaepCredentialChange.TYPE), stream),
                "credential-change is not in the manual-only set, so it must still auto-emit");
    }

    @Test
    void tokenWithMultipleEvents_skipsWhenAnyMatches() {
        StreamConfig stream = new StreamConfig();
        stream.setManualOnlyEvents(Set.of(CaepSessionRevoked.TYPE));

        // Tokens almost always carry exactly one event in practice, but the
        // shape of SsfSecurityEventToken.getEvents() is a Map keyed by URI,
        // so guard the iteration: a single match anywhere is enough to skip.
        SsfSecurityEventToken token = eventToken(CaepCredentialChange.TYPE);
        token.getEvents().put(CaepSessionRevoked.TYPE, new CaepSessionRevoked());

        assertTrue(listener.isManualOnlyForStream(token, stream),
                "a token carrying any manual-only event type must be dropped — partial delivery would be confusing");
    }

    @Test
    void tokenWithNoEvents_doesNotSkip() {
        StreamConfig stream = new StreamConfig();
        stream.setManualOnlyEvents(Set.of(CaepSessionRevoked.TYPE));

        SsfSecurityEventToken token = new SsfSecurityEventToken();
        // events map left null — defensive, since real generated tokens
        // always carry at least one event entry

        assertFalse(listener.isManualOnlyForStream(token, stream),
                "a token without any events has nothing to filter on; the predicate should be a no-op");
    }

    /**
     * Builds a minimal token carrying a single SSF event of the given type
     * keyed in the {@code events} map by its URI — the exact shape the
     * dispatcher hands to {@code isManualOnlyForStream}.
     */
    private SsfSecurityEventToken eventToken(String eventTypeUri) {
        SsfSecurityEventToken token = new SsfSecurityEventToken();
        token.setJti("jti-test");
        // The events map is Map<String, Object> on the parent class
        // (Jackson loose-typing) — only the keys are inspected by the
        // manual-only predicate, so the value can be any non-null
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
