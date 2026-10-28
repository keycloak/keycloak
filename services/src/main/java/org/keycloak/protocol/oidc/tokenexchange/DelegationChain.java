package org.keycloak.protocol.oidc.tokenexchange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Helpers to read the nested "act" claim of RFC 8693 section 4.1, where the outermost level is the most recent actor.
 */
public final class DelegationChain {

    /**
     * Hard limit on the number of actors in the "act" claim of a delegated token. Not configurable, so every hop
     * applies the same bound no matter which client performs it.
     */
    public static final int MAX_CHAIN_DEPTH = 8;

    private DelegationChain() {
    }

    /**
     * Returns every actor of a nested "act" claim, most recent first. Empty if the claim is absent or malformed.
     */
    @SuppressWarnings("unchecked")
    public static List<DelegationActor> actors(Object actClaim) {
        if (!(actClaim instanceof Map)) {
            return Collections.emptyList();
        }

        List<DelegationActor> actors = new ArrayList<>();
        Object current = actClaim;
        while (current instanceof Map) {
            DelegationActor actor = new DelegationActor((Map<String, Object>) current);
            actors.add(actor);
            current = actor.getNestedAct();
        }
        return actors;
    }

    /**
     * True when every actor was produced by client delegation, which is the only delegation type that can be chained.
     * Admin delegation and impersonation write an actor without a "client_id".
     */
    public static boolean allActorsHaveClientId(List<DelegationActor> actors) {
        return actors.stream().allMatch(actor -> actor.getClientId() != null);
    }

    public static boolean containsActor(List<DelegationActor> actors, String actorUserId) {
        return actors.stream().anyMatch(actor -> actorUserId.equals(actor.getSubject()));
    }
}
