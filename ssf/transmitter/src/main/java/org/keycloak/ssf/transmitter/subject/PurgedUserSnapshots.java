package org.keycloak.ssf.transmitter.subject;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.models.KeycloakSession;

/**
 * The set of {@link PurgedUserSnapshot}s captured during one request, held under a
 * single {@link KeycloakSession} attribute.
 *
 * <p>Snapshots were originally stashed one session attribute per user, which read
 * back typed with no cast and matched how Keycloak stashes other request-scoped
 * per-id state ({@code AuthenticationSessionAdapter} keys on
 * {@code "authSession.user." + parentSessionId}). That stopped paying once the set
 * grew a retention bound and a discard hook: enforcing those across N independent
 * attributes meant three writes that had to stay in step — the id index, the email
 * index, and a hand-maintained count — and a count maintained by hand is a count
 * that can drift from what is actually held.
 *
 * <p>Collecting them here makes the size exact and free, keeps the SSF footprint in
 * the session's shared attribute namespace to one entry rather than two per user
 * plus a counter, and puts the bound next to the collection it bounds. The holder is
 * what keeps the reads typed: {@code getAttribute(KEY, PurgedUserSnapshots.class)}
 * needs no unchecked cast, where a bare {@code Map} would.
 *
 * <p>Purely request-scoped bookkeeping — it lives and dies with the session, is never
 * persisted, and is not an extension point. {@link PurgedUserSnapshot} owns the key
 * format and is the only caller.
 */
class PurgedUserSnapshots {

    private static final String SESSION_ATTRIBUTE_KEY = "ssf.purgedUsers";

    /**
     * Upper bound on snapshots retained per session, complementing
     * {@link PurgedUserSnapshot#isRequestBound}.
     *
     * <p>The request gate excludes deletions with no request behind them, but bulk
     * deletion still happens <em>inside</em> a request: a partial import under the
     * {@code OVERWRITE} policy replaces many users in one call, and deleting an
     * organization removes each of its managed members. Neither emits a purge event,
     * so their snapshots are never read — and without a bound, one such request would
     * retain a copied attribute map per account until it finishes.
     *
     * <p>An emitting request releases each snapshot as soon as it has emitted for it
     * (see {@link PurgedUserSnapshot#discard}), so it holds one at a time and cannot
     * approach this bound however many users it deletes.
     */
    static final int MAX_SNAPSHOTS_PER_SESSION = 64;

    /** Primary index, keyed by realm-scoped user id. Its size is the retained count. */
    private final Map<String, PurgedUserSnapshot> byId = new HashMap<>();

    /** Secondary index, keyed by realm-scoped email. See {@link PurgedUserSnapshot}. */
    private final Map<String, PurgedUserSnapshot> byEmail = new HashMap<>();

    /**
     * Whether the bound has already been reported for this session. Deliberately not
     * derived from {@link #size()}: the two answer different questions, and folding
     * the "already warned" latch into the count is what let the count drift above the
     * number actually held — which in turn wedged the gate closed even after a
     * {@code discard} had freed a slot.
     */
    private boolean boundWarned;

    /**
     * Returns the set for this session, creating and stashing it on first use. Only
     * the capture path calls this; readers use {@link #find} so a lookup never
     * allocates.
     */
    static PurgedUserSnapshots of(KeycloakSession session) {
        PurgedUserSnapshots snapshots = find(session);
        if (snapshots == null) {
            snapshots = new PurgedUserSnapshots();
            session.setAttribute(SESSION_ATTRIBUTE_KEY, snapshots);
        }
        return snapshots;
    }

    /** Returns the set for this session, or {@code null} if nothing was captured. */
    static PurgedUserSnapshots find(KeycloakSession session) {
        return session.getAttribute(SESSION_ATTRIBUTE_KEY, PurgedUserSnapshots.class);
    }

    PurgedUserSnapshot byId(String idKey) {
        return byId.get(idKey);
    }

    PurgedUserSnapshot byEmail(String emailKey) {
        return byEmail.get(emailKey);
    }

    /** Indexes {@code snapshot} under both keys. {@code emailKey} is null when the user had no email. */
    void put(String idKey, String emailKey, PurgedUserSnapshot snapshot) {
        byId.put(idKey, snapshot);
        if (emailKey != null) {
            byEmail.put(emailKey, snapshot);
        }
    }

    /** Drops both index entries, freeing a slot against {@link #MAX_SNAPSHOTS_PER_SESSION}. */
    void remove(String idKey, String emailKey) {
        byId.remove(idKey);
        if (emailKey != null) {
            byEmail.remove(emailKey);
        }
    }

    int size() {
        return byId.size();
    }

    boolean isFull() {
        return byId.size() >= MAX_SNAPSHOTS_PER_SESSION;
    }

    /**
     * Latches the bound warning, returning {@code true} only for the caller that
     * flips it — so a bulk request leaves one log line rather than one per deletion.
     */
    boolean markBoundWarned() {
        if (boundWarned) {
            return false;
        }
        boundWarned = true;
        return true;
    }
}
