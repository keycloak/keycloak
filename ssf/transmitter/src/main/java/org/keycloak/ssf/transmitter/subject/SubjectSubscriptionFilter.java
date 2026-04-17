package org.keycloak.ssf.transmitter.subject;


import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.ssf.event.token.SsfSecurityEventToken;
import org.keycloak.ssf.metadata.DefaultSubjects;
import org.keycloak.ssf.event.stream.SsfStreamVerificationEvent;
import org.keycloak.ssf.event.stream.SsfStreamUpdatedEvent;
import org.keycloak.ssf.subject.ComplexSubjectId;
import org.keycloak.ssf.subject.EmailSubjectId;
import org.keycloak.ssf.subject.IssuerSubjectId;
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

    public SubjectSubscriptionFilter() {}

    /**
     * Returns {@code true} if the event should be delivered to the given
     * stream, {@code false} to silently skip it.
     */
    public boolean shouldDispatch(SsfSecurityEventToken eventToken,
                                         StreamConfig stream,
                                         String receiverClientId,
                                         KeycloakSession session) {

        DefaultSubjects defaultSubjects = stream.getDefaultSubjects();

        // Stream management events (verification, stream-updated) are
        // always delivered — they're about the stream itself, not about
        // a specific user subject.
        if (isSsfStreamEvent(eventToken)) {
            return true;
        }

        RealmModel realm = session.getContext().getRealm();
        UserModel user = resolveUserFromEvent(eventToken, session, realm);

        if (user == null) {
            // Event carries a subject but the user couldn't be resolved
            // (deleted, issuer mismatch, unknown format, etc.).
            // In ALL mode: deliver (benefit of the doubt).
            // In NONE mode: block (can't verify subscription).
            boolean deliver = defaultSubjects == DefaultSubjects.ALL;
            if (!deliver) {
                log.debugf("SSF subject filter: skipping event — user subject unresolvable (default_subjects=NONE). "
                                + "streamId=%s clientId=%s jti=%s",
                        stream.getStreamId(), receiverClientId, eventToken.getJti());
            }
            return deliver;
        }

        if (defaultSubjects == DefaultSubjects.ALL) {
            // Broadcast mode: deliver unless explicitly excluded.
            if (isExcluded(user, receiverClientId, session)) {
                log.debugf("SSF subject filter: skipping event — user is marked as IGNORED (default_subjects=ALL). "
                                + "streamId=%s clientId=%s userId=%s jti=%s",
                        stream.getStreamId(), receiverClientId,
                        user.getId(), eventToken.getJti());
                return false;
            }
            return true;
        }

        // NONE mode: deliver only when explicitly included.
        if (SsfNotifyAttributes.isUserNotified(user, receiverClientId)) {
            return true;
        }

        if (isOrganizationNotified(user, receiverClientId, session)) {
            return true;
        }

        log.debugf("SSF subject filter: skipping event — user has no notification preference (default_subjects=NONE). "
                        + "streamId=%s clientId=%s userId=%s jti=%s",
                stream.getStreamId(), receiverClientId,
                user.getId(), eventToken.getJti());
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
     * Checks if the user (or any of the user's organizations) is
     * explicitly excluded via {@code ssf.notify.<clientId>=false}.
     */
    protected boolean isExcluded(UserModel user, String receiverClientId, KeycloakSession session) {
        if (SsfNotifyAttributes.isUserExcluded(user, receiverClientId)) {
            return true;
        }
        if (Profile.isFeatureEnabled(Profile.Feature.ORGANIZATION)) {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            if (orgProvider != null) {
                return orgProvider.getByMember(user)
                        .anyMatch(org -> SsfNotifyAttributes.isOrganizationExcluded(org, receiverClientId));
            }
        }
        return false;
    }

    /**
     * Checks if any of the user's organizations is explicitly notified.
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
                .anyMatch(org -> SsfNotifyAttributes.isOrganizationNotified(org, receiverClientId));
    }
}
