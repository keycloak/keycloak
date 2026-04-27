package org.keycloak.ssf.transmitter.subject;


import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.ssf.event.stream.SsfStreamUpdatedEvent;
import org.keycloak.ssf.event.stream.SsfStreamVerificationEvent;
import org.keycloak.ssf.event.token.SsfSecurityEventToken;
import org.keycloak.ssf.metadata.DefaultSubjects;
import org.keycloak.ssf.subject.ComplexSubjectId;
import org.keycloak.ssf.subject.SubjectId;
import org.keycloak.ssf.subject.SubjectUserLookup;
import org.keycloak.ssf.transmitter.stream.StreamConfig;

import org.jboss.logging.Logger;

/**
 * Dispatch-time check that controls per-subject event delivery based
 * on the stream's {@code default_subjects} setting and the
 * {@code ssf.notify.<clientId>} attribute on the event's subject.
 *
 * <p>Semantics:
 * <ul>
 *     <li>{@link DefaultSubjects#ALL}: deliver to everyone unless the
 *         user/org is explicitly excluded ({@code ssf.notify.<clientId>=false}).</li>
 *     <li>{@link DefaultSubjects#NONE}: deliver only when the user/org
 *         is explicitly included ({@code ssf.notify.<clientId>=true}).</li>
 * </ul>
 *
 * <p>Called from
 * {@link org.keycloak.ssf.transmitter.delivery.SecurityEventTokenDispatcher}
 * after the event-type filter and before encoding/pushing.
 */
public class SubjectSubscriptionFilter {

    private static final Logger log = Logger.getLogger(SubjectSubscriptionFilter.class);

    /**
     * Grace window (seconds) during which a recently-removed subject
     * still receives events on a {@link DefaultSubjects#NONE} stream.
     * Defends against the SSF 1.0 §9.3 "Malicious Subject Removal"
     * scenario. {@code 0} disables the grace check (current behavior
     * preserved when the SPI knob is unset).
     */
    protected final long subjectRemovalGraceSeconds;

    /**
     * Pluggable include / exclude predicate. Default delegates to
     * {@link SsfNotifyAttributes}; extensions plug in custom logic
     * (group attribute lookups, role-based opt-ins) via the
     * transmitter provider's
     * {@link org.keycloak.ssf.transmitter.SsfTransmitterProvider#subjectInclusionResolver}.
     */
    protected final SsfSubjectInclusionResolver subjectInclusionResolver;

    public SubjectSubscriptionFilter() {
        this(0L, new DefaultSsfSubjectInclusionResolver());
    }

    public SubjectSubscriptionFilter(long subjectRemovalGraceSeconds) {
        this(subjectRemovalGraceSeconds, new DefaultSsfSubjectInclusionResolver());
    }

    public SubjectSubscriptionFilter(long subjectRemovalGraceSeconds,
                                     SsfSubjectInclusionResolver subjectInclusionResolver) {
        this.subjectRemovalGraceSeconds = Math.max(0L, subjectRemovalGraceSeconds);
        this.subjectInclusionResolver = subjectInclusionResolver != null
                ? subjectInclusionResolver
                : new DefaultSsfSubjectInclusionResolver();
    }

    /**
     * Returns {@code true} if the event should be delivered to the given
     * stream, {@code false} to silently skip it.
     */
    public boolean shouldDispatch(SsfSecurityEventToken eventToken,
                                         StreamConfig stream,
                                         String receiverClientId,
                                         KeycloakSession session) {

        // Stream management events (verification, stream-updated) are
        // always delivered — they're about the stream itself, not about
        // a specific user subject.
        if (isSsfStreamEvent(eventToken)) {
            return true;
        }

        RealmModel realm = session.getContext().getRealm();
        UserModel user = resolveUserFromEvent(eventToken, session, realm);

        return evaluateSubjectSubscription(user, stream, receiverClientId, session, eventToken.getJti());
    }

    /**
     * Pre-token gate used by the native event listener to skip streams
     * before running the mapper. Takes a pre-resolved user rather than
     * a token subject — callers that need the token-subject semantics
     * (complex subjects, stream management events) should use
     * {@link #shouldDispatch(SsfSecurityEventToken, StreamConfig, String, KeycloakSession)}
     * instead. The dispatcher-side gate still runs, so a mismatch
     * between {@code event.getUserId()} and the final token subject
     * (impersonation, actor-on-behalf) stays safe.
     */
    public boolean shouldDispatchForUser(UserModel user,
                                         StreamConfig stream,
                                         String receiverClientId,
                                         KeycloakSession session) {
        return evaluateSubjectSubscription(user, stream, receiverClientId, session, null);
    }

    protected boolean evaluateSubjectSubscription(UserModel user,
                                                  StreamConfig stream,
                                                  String receiverClientId,
                                                  KeycloakSession session,
                                                  String jti) {

        DefaultSubjects defaultSubjects = stream.getDefaultSubjects();

        if (user == null) {
            // Event carries a subject but the user couldn't be resolved
            // (deleted, issuer mismatch, unknown format, etc.).
            // In ALL mode: deliver (benefit of the doubt).
            // In NONE mode: block (can't verify subscription).
            boolean deliver = defaultSubjects == DefaultSubjects.ALL;
            if (!deliver) {
                log.debugf("SSF subject filter: skipping event — user subject unresolvable (default_subjects=NONE). "
                                + "streamId=%s clientId=%s jti=%s",
                        stream.getStreamId(), receiverClientId, jti);
            }
            return deliver;
        }

        // Per-user explicit settings always win over org inheritance
        // and default_subjects fallback. An admin who clicked "Include"
        // or "Ignore" on a specific user expects that decision to stick
        // regardless of any org-membership-driven defaults.
        if (subjectInclusionResolver.isUserNotified(session, user, receiverClientId)) {
            return true;
        }
        if (subjectInclusionResolver.isUserExcluded(session, user, receiverClientId)) {
            log.debugf("SSF subject filter: skipping event — user is explicitly excluded. "
                            + "streamId=%s clientId=%s userId=%s jti=%s",
                    stream.getStreamId(), receiverClientId,
                    user.getId(), jti);
            return false;
        }

        if (defaultSubjects == DefaultSubjects.ALL) {
            // Broadcast mode: deliver unless any org excludes the user.
            if (isOrganizationExcluded(user, receiverClientId, session)) {
                log.debugf("SSF subject filter: skipping event — user is excluded via organization (default_subjects=ALL). "
                                + "streamId=%s clientId=%s userId=%s jti=%s",
                        stream.getStreamId(), receiverClientId,
                        user.getId(), jti);
                return false;
            }
            return true;
        }

        // NONE mode: deliver only when explicitly included.
        if (isOrganizationNotified(user, receiverClientId, session)) {
            return true;
        }

        // SSF §9.3 grace window — receiver-driven removes leave a
        // tombstone; while we're inside the configured grace, keep
        // delivering events for the subject so a compromised receiver
        // can't silently silence events for a target. Effective grace
        // is the per-receiver override (ssf.subjectRemovalGraceSeconds
        // client attribute) when set, otherwise the transmitter-wide
        // default this filter was constructed with. Zero disables.
        long effectiveGrace = effectiveGraceSeconds(stream);
        if (effectiveGrace > 0
                && isWithinRemovalGrace(user, receiverClientId, session, effectiveGrace)) {
            log.debugf("SSF subject filter: delivering inside removal grace window (default_subjects=NONE). "
                            + "streamId=%s clientId=%s userId=%s jti=%s graceSeconds=%d",
                    stream.getStreamId(), receiverClientId,
                    user.getId(), jti, effectiveGrace);
            return true;
        }

        log.debugf("SSF subject filter: skipping event — user has no notification preference (default_subjects=NONE). "
                        + "streamId=%s clientId=%s userId=%s jti=%s",
                stream.getStreamId(), receiverClientId,
                user.getId(), jti);
        return false;
    }

    /**
     * Resolves the effective grace window for a stream — prefers the
     * per-receiver override
     * ({@code ssf.subjectRemovalGraceSeconds} client attribute,
     * surfaced on {@link StreamConfig#getSubjectRemovalGraceSeconds()})
     * when set, otherwise falls back to the transmitter-wide default
     * this filter was constructed with.
     */
    protected long effectiveGraceSeconds(StreamConfig stream) {
        Integer perReceiver = stream != null ? stream.getSubjectRemovalGraceSeconds() : null;
        if (perReceiver != null) {
            return Math.max(0L, perReceiver.longValue());
        }
        return subjectRemovalGraceSeconds;
    }

    /**
     * Returns {@code true} when the user (or any of the user's
     * organizations) was removed via a receiver-driven {@code
     * /subjects/remove} call within the last
     * {@code graceSeconds} seconds.
     */
    protected boolean isWithinRemovalGrace(UserModel user, String receiverClientId, KeycloakSession session, long graceSeconds) {
        long now = Time.currentTime();
        Long userTombstone = SsfNotifyAttributes.getRemovedAtForUser(user, receiverClientId);
        if (userTombstone != null && now - userTombstone < graceSeconds) {
            return true;
        }
        if (Profile.isFeatureEnabled(Profile.Feature.ORGANIZATION)) {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            if (orgProvider != null) {
                return orgProvider.getByMember(user)
                        .anyMatch(org -> {
                            Long orgTombstone = SsfNotifyAttributes.getRemovedAtForOrganization(org, receiverClientId);
                            return orgTombstone != null
                                    && now - orgTombstone < graceSeconds;
                        });
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if the event token carries an SSF stream
     * management event (verification, stream-updated). These events
     * are about the stream itself, not about a specific user, and
     * must always be delivered regardless of subject filtering.
     */
    protected boolean isSsfStreamEvent(SsfSecurityEventToken eventToken) {
        var events = eventToken.getEvents();
        if (events == null || events.isEmpty()) {
            return false;
        }
        return events.containsKey(SsfStreamVerificationEvent.TYPE)
                || events.containsKey(SsfStreamUpdatedEvent.TYPE);
    }

    /**
     * Extracts and resolves the user from an event token's subject id.
     * Handles both simple subjects (email, iss_sub, opaque) and complex
     * subjects by drilling into {@link ComplexSubjectId#getUser()}.
     * Returns {@code null} when no user can be resolved.
     */
    protected UserModel resolveUserFromEvent(SsfSecurityEventToken eventToken,
                                                  KeycloakSession session,
                                                  RealmModel realm) {
        SubjectId subjectId = eventToken.getSubjectId();
        if (subjectId == null) {
            return null;
        }

        if (subjectId instanceof ComplexSubjectId complex) {
            SubjectId userSubject = complex.getUser();
            if (userSubject == null) {
                return null;
            }
            return lookupUserBySubject(session, realm, userSubject);
        }

        return lookupUserBySubject(session, realm, subjectId);
    }

    protected UserModel lookupUserBySubject(KeycloakSession session, RealmModel realm, SubjectId userSubject) {
        return SubjectUserLookup.lookupUser(session, realm, userSubject);
    }

    /**
     * Checks if any of the user's organizations is explicitly excluded
     * per the {@link #subjectInclusionResolver}. The per-user exclude
     * is intentionally NOT checked here — user-explicit settings are
     * resolved earlier in {@link #evaluateSubjectSubscription} and
     * always override org-level state, so this helper only answers the
     * "is the user excluded *via* one of their orgs" question.
     */
    protected boolean isOrganizationExcluded(UserModel user, String receiverClientId, KeycloakSession session) {
        if (!Profile.isFeatureEnabled(Profile.Feature.ORGANIZATION)) {
            return false;
        }
        OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
        if (orgProvider == null) {
            return false;
        }
        return orgProvider.getByMember(user)
                .anyMatch(org -> subjectInclusionResolver.isOrganizationExcluded(session, org, receiverClientId));
    }

    /**
     * Checks if any of the user's organizations is explicitly notified
     * per the {@link #subjectInclusionResolver}.
     */
    protected boolean isOrganizationNotified(UserModel user, String receiverClientId, KeycloakSession session) {
        if (!Profile.isFeatureEnabled(Profile.Feature.ORGANIZATION)) {
            return false;
        }
        OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
        if (orgProvider == null) {
            return false;
        }
        return orgProvider.getByMember(user)
                .anyMatch(org -> subjectInclusionResolver.isOrganizationNotified(session, org, receiverClientId));
    }
}
