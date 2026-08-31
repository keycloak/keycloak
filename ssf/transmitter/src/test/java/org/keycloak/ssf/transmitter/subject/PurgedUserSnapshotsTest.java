package org.keycloak.ssf.transmitter.subject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.models.KeycloakSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the request-scoped snapshot set: its two indexes, the retention
 * bound, and the warn-once latch.
 *
 * <p>The bound and the latch used to be one hand-maintained counter attribute, which
 * meant the "already warned" marker was written by pushing the count one past the
 * maximum. That made the count stop matching the number actually held, and the
 * regressions it caused are pinned here: a freed slot has to become reusable, and the
 * warning has to stay fired exactly once however the set churns.
 */
class PurgedUserSnapshotsTest {

    PurgedUserSnapshots snapshots;

    @BeforeEach
    void setUp() {
        snapshots = new PurgedUserSnapshots();
    }

    @Test
    void size_tracksPutsAndRemoves() {
        snapshots.put("realm.user-1", "realm.one@local.test", snapshot("user-1"));
        snapshots.put("realm.user-2", "realm.two@local.test", snapshot("user-2"));
        assertEquals(2, snapshots.size());

        snapshots.remove("realm.user-1", "realm.one@local.test");
        assertEquals(1, snapshots.size());
    }

    @Test
    void bothIndexesResolveTheSameSnapshot() {
        PurgedUserSnapshot captured = snapshot("user-1");
        snapshots.put("realm.user-1", "realm.one@local.test", captured);

        assertSame(captured, snapshots.byId("realm.user-1"));
        assertSame(captured, snapshots.byEmail("realm.one@local.test"));
    }

    @Test
    void removeClearsBothIndexes() {
        snapshots.put("realm.user-1", "realm.one@local.test", snapshot("user-1"));

        snapshots.remove("realm.user-1", "realm.one@local.test");

        assertNull(snapshots.byId("realm.user-1"));
        assertNull(snapshots.byEmail("realm.one@local.test"));
    }

    @Test
    void userWithoutEmail_isIndexedByIdOnly() {
        // capture() passes a null email key for a user with no email rather than
        // indexing under a null, so the secondary index stays addressable.
        PurgedUserSnapshot captured = snapshot("user-1");
        snapshots.put("realm.user-1", null, captured);

        assertSame(captured, snapshots.byId("realm.user-1"));
        assertEquals(1, snapshots.size());
        assertNull(snapshots.byEmail(null));

        snapshots.remove("realm.user-1", null);
        assertEquals(0, snapshots.size());
    }

    @Test
    void isFull_onlyAtTheBound() {
        for (int i = 0; i < PurgedUserSnapshots.MAX_SNAPSHOTS_PER_SESSION - 1; i++) {
            snapshots.put("realm.user-" + i, null, snapshot("user-" + i));
            assertFalse(snapshots.isFull(), "not full below the bound");
        }

        snapshots.put("realm.user-last", null, snapshot("user-last"));

        assertTrue(snapshots.isFull());
        assertEquals(PurgedUserSnapshots.MAX_SNAPSHOTS_PER_SESSION, snapshots.size());
    }

    @Test
    void discardingFreesASlotForTheNextCapture() {
        // The regression. A request that hits the bound and then discards a snapshot
        // it has finished emitting for must be able to capture again — the count is
        // the set's real size, so it drops back below the bound. Backed by a counter
        // that had been pushed past the maximum to double as a warned-flag, the gate
        // stayed shut for the rest of the request instead.
        fillToBound();
        assertTrue(snapshots.isFull());

        snapshots.remove("realm.user-0", null);

        assertFalse(snapshots.isFull(), "a freed slot must be reusable");
        snapshots.put("realm.user-new", null, snapshot("user-new"));
        assertNotNull(snapshots.byId("realm.user-new"), "capture must succeed after a discard");
    }

    @Test
    void boundWarning_firesExactlyOncePerSession() {
        assertTrue(snapshots.markBoundWarned(), "first caller flips the latch");
        assertFalse(snapshots.markBoundWarned());
        assertFalse(snapshots.markBoundWarned());
    }

    @Test
    void boundWarning_staysLatchedAcrossChurn() {
        // Same regression seen from the logging side: with the latch inferred from the
        // count, every discard dropped it back to the warn-again value, so a bulk
        // request emitted a WARN per deletion rather than the one line intended.
        fillToBound();
        assertTrue(snapshots.markBoundWarned());

        snapshots.remove("realm.user-0", null);
        snapshots.put("realm.user-0", null, snapshot("user-0"));

        assertFalse(snapshots.markBoundWarned(), "churn must not re-arm the warning");
    }

    @Test
    void find_returnsNullBeforeAnythingIsCaptured() {
        assertNull(PurgedUserSnapshots.find(sessionWithAttributes()));
    }

    @Test
    void of_createsOnceAndStashesOnTheSession() {
        KeycloakSession session = sessionWithAttributes();

        PurgedUserSnapshots first = PurgedUserSnapshots.of(session);
        PurgedUserSnapshots second = PurgedUserSnapshots.of(session);

        assertSame(first, second, "the set is created once per session");
        assertSame(first, PurgedUserSnapshots.find(session));
    }

    private void fillToBound() {
        for (int i = 0; i < PurgedUserSnapshots.MAX_SNAPSHOTS_PER_SESSION; i++) {
            snapshots.put("realm.user-" + i, null, snapshot("user-" + i));
        }
    }

    private PurgedUserSnapshot snapshot(String userId) {
        return new PurgedUserSnapshot(null, null, userId, null, List.of());
    }

    /**
     * A session whose attribute map behaves like the real one — {@code setAttribute}
     * stores and the typed {@code getAttribute} reads back — so {@code of} / {@code find}
     * are exercised against get-or-create semantics rather than a stubbed return.
     */
    private KeycloakSession sessionWithAttributes() {
        KeycloakSession session = mock(KeycloakSession.class);
        Map<String, Object> attributes = new HashMap<>();

        doAnswer(invocation -> {
            attributes.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(session).setAttribute(anyString(), any());

        when(session.getAttribute(anyString(), any(Class.class))).thenAnswer(invocation -> {
            Object value = attributes.get(invocation.<String>getArgument(0));
            return value == null ? null : invocation.<Class<?>>getArgument(1).cast(value);
        });

        return session;
    }
}
