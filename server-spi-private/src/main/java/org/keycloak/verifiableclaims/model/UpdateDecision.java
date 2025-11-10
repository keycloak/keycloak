package org.keycloak.verifiableclaims.model;

import java.util.*;

/**
 * Result from the VerifiableClaimProvider before UserProfile writes attributes.
 * - toPersistNow: the attributes (as a Map) that should be persisted immediately.
 * - statuses: optional per-attribute status/evidence projection for the caller.
 * - delayPersistFor: attributes whose new values are staged/pending (not persisted now).
 */
public class UpdateDecision {
    private final Map<String, List<String>> toPersistNow;  // <--- Map, not Attributes
    private final Map<String, ClaimProjection> statuses;
    private final Set<String> delayPersistFor;

    private UpdateDecision(Map<String, List<String>> toPersistNow,
                           Map<String, ClaimProjection> statuses,
                           Set<String> delayPersistFor) {
        this.toPersistNow = toPersistNow;
        this.statuses = statuses;
        this.delayPersistFor = delayPersistFor;
    }

    /** Attributes to persist NOW (caller wraps this into DefaultAttributes). */
    public Map<String, List<String>> getToPersistNow() { return toPersistNow; }
    public Map<String, ClaimProjection> getStatuses() { return statuses; }
    public Set<String> getDelayPersistFor() { return delayPersistFor; }

    /** Build from any existing attribute map (usually base.toMap() or base.getWritable()). */
    public static Builder builder(Map<String, List<String>> base) { return new Builder(base); }

    public static final class Builder {
        private final Map<String, List<String>> effective;
        private final Map<String, ClaimProjection> statuses = new HashMap<>();
        private final Set<String> delay = new HashSet<>();

        public Builder(Map<String, List<String>> base) {
            this.effective = new HashMap<>();
            if (base != null) {
                for (Map.Entry<String, List<String>> e : base.entrySet()) {
                    this.effective.put(e.getKey(),
                            e.getValue() == null ? null : new ArrayList<>(e.getValue()));
                }
            }
        }

        /** Persist this value immediately */
        public Builder setNow(String attr, List<String> values) {
            this.effective.put(attr, values == null ? null : new ArrayList<>(values));
            return this;
        }

        /** Stage (do *not* persist now) */
        public Builder delay(String attr) {
            this.delay.add(attr);
            return this;
        }

        /** Set verification/attestation output */
        public Builder status(String attr, ClaimProjection projection) {
            this.statuses.put(attr, projection);
            return this;
        }

        public UpdateDecision build() {
            return new UpdateDecision(
                    Collections.unmodifiableMap(new HashMap<>(this.effective)),
                    Collections.unmodifiableMap(new HashMap<>(this.statuses)),
                    Collections.unmodifiableSet(new HashSet<>(this.delay))
            );
        }
    }
}
