package org.keycloak.ssf.transmitter.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.ssf.Ssf;
import org.keycloak.ssf.event.token.SsfSecurityEventToken;
import org.keycloak.ssf.metadata.DefaultSubjects;
import org.keycloak.ssf.transmitter.SsfTransmitterProvider;
import org.keycloak.ssf.transmitter.stream.StreamConfig;
import org.keycloak.ssf.transmitter.stream.StreamService;
import org.keycloak.ssf.transmitter.stream.storage.client.ClientStreamStore;
import org.keycloak.ssf.transmitter.subject.SsfNotifyAttributes;
import org.keycloak.ssf.transmitter.support.SsfUtil;

import org.jboss.logging.Logger;

/**
 * Maps Keycloak user and admin events to SSF events for delivery.
 */
public class SsfTransmitterEventListener implements EventListenerProvider {

    protected static final Logger log = Logger.getLogger(SsfTransmitterEventListener.class);

    private final KeycloakSession session;

    public SsfTransmitterEventListener(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void onEvent(Event event) {

        if (shouldIgnoreEvent(event)) {
            return;
        }

        SsfTransmitterProvider transmitter = session.getProvider(SsfTransmitterProvider.class);
        if (transmitter == null) {
            return;
        }

        // Auto-tag users on login for SSF receivers with autoNotifyOnLogin.
        // Runs after the transmitter lookup so we don't write per-user
        // attributes when SSF isn't actually wired in this realm — the
        // realm-attribute gate in shouldIgnoreEvent is a coarser check.
        if (EventType.LOGIN.equals(event.getType())) {
            autoNotifyOnLogin(event, transmitter);
        }

        // Ask the mapper first whether this event could even produce a SET.
        // If not, bail before doing any stream-lookup work — most realm
        // events are not SSF-mappable, and stream lookups hit the client
        // store (plus any cache layer in front of it), whereas this
        // predicate is an in-memory switch on event type.
        if (!transmitter.securityEventTokenMapper().canConvert(event)) {
            return;
        }

        var streamTokens = generateSecurityTokensForUserEvent(event, transmitter);
        if (streamTokens == null || streamTokens.isEmpty()) {
            return;
        }
        dispatchSecurityEventTokens(streamTokens, transmitter);
    }

    protected List<Map.Entry<SsfSecurityEventToken, StreamConfig>> generateSecurityTokensForUserEvent(Event event, SsfTransmitterProvider transmitter) {

        StreamService streamService = transmitter.streamService();
        List<StreamConfig> streams = streamService.findStreamsForSsfReceiverClients();
        if (streams.isEmpty()) {
            log.warnf("No streams found. Discarding user event %s", event.getId());
            return List.of();
        }

        // Resolve the user once for the pre-token subject gate. A null
        // userId or an unresolvable user bypasses the pre-check and
        // lets the dispatcher-side gate decide — it has the ALL/NONE
        // fallback for unresolved subjects and handles complex-subject
        // cases where the token subject differs from event.getUserId().
        UserModel eventUser = resolveEventUser(event);

        // The top-level canConvert() gate already verified the event type is
        // mappable, so every stream below will attempt a real conversion.
        // Per-stream null results are still handled defensively — e.g. if
        // the user was deleted mid-event and the subject lookup fails.
        var streamTokens = new ArrayList<Map.Entry<SsfSecurityEventToken, StreamConfig>>();
        for (StreamConfig stream : streams) {
            if (eventUser != null
                    && !transmitter.securityEventTokenDispatcher().shouldDispatchForUser(eventUser, stream)) {
                // Subject-gate negative — skip the mapper call entirely.
                // Avoids building a token for a stream the dispatcher
                // would just filter out, and keeps logs honest for the
                // multi-receiver case where only some streams deliver.
                continue;
            }
            SsfSecurityEventToken securityEventToken = convertUserEventToSecurityEventToken(event, transmitter, stream);
            if (securityEventToken == null) {
                log.debugf("Could not generate SSF Security Event Token for User Event. id=%s", event.getId());
                continue;
            }
            if (isAnyEventEmitOnlyForReceiver(securityEventToken, stream)) {
                log.debugf("Skipping native auto-emit — event type is in receiver's emitOnlyEvents set. "
                                + "streamId=%s clientId=%s jti=%s",
                        stream.getStreamId(), stream.getClientClientId(), securityEventToken.getJti());
                continue;
            }
            log.debugf("Generated SSF Security Event Token for User Event. "
                            + "realm=%s clientId=%s streamId=%s userId=%s eventType=%s jti=%s",
                    session.getContext().getRealm().getName(),
                    stream.getClientClientId(), stream.getStreamId(),
                    event.getUserId(), event.getType(), securityEventToken.getJti());
            streamTokens.add(Map.entry(securityEventToken, stream));
        }
        return streamTokens;
    }

    protected UserModel resolveEventUser(Event event) {
        String userId = event.getUserId();
        if (userId == null) {
            return null;
        }
        RealmModel realm = session.getContext().getRealm();
        if (realm == null) {
            return null;
        }
        return session.users().getUserById(realm, userId);
    }

    /**
     * Returns {@code true} when the receiver has marked the event type
     * as emit-only ({@code ssf.emitOnlyEvents} contains it). The native
     * event listener honours the gate; the synthetic-emit endpoint
     * deliberately does not — its whole purpose is to fire exactly
     * these events on demand.
     */
    protected boolean isEmitOnlyEventForReceiver(String eventType, StreamConfig stream) {
        var emitOnly = stream.getEmitOnlyEvents();
        return emitOnly != null && emitOnly.contains(eventType);
    }

    /**
     * True when any event entry in the token is emit-only for the
     * receiver. Tokens almost always carry a single event in practice,
     * but {@link SsfSecurityEventToken#getEvents()} is a Map keyed by
     * event-type URI, so guard the iteration: a single match anywhere
     * is enough to skip — partial delivery would be confusing.
     */
    protected boolean isAnyEventEmitOnlyForReceiver(SsfSecurityEventToken token, StreamConfig stream) {
        var events = token.getEvents();
        if (events == null || events.isEmpty()) {
            return false;
        }
        for (String eventType : events.keySet()) {
            if (isEmitOnlyEventForReceiver(eventType, stream)) {
                return true;
            }
        }
        return false;
    }

    protected void dispatchSecurityEventToken(SsfSecurityEventToken securityEventToken, SsfTransmitterProvider transmitter, StreamConfig stream) {
        transmitter.securityEventTokenDispatcher().dispatchEvent(securityEventToken, stream);
    }

    protected SsfSecurityEventToken convertUserEventToSecurityEventToken(Event event, SsfTransmitterProvider transmitter, StreamConfig stream) {
        return transmitter.securityEventTokenMapper().toSecurityEventToken(event, stream);
    }

    /**
     * When a user logs in via a client that is an SSF receiver with
     * {@code ssf.autoNotifyOnLogin=true} and
     * {@code ssf.defaultSubjects=NONE}, automatically sets the
     * {@code ssf.notify.<clientId>} attribute on the user so future
     * events for that user are delivered to the receiver's stream.
     */
    protected void autoNotifyOnLogin(Event event, SsfTransmitterProvider transmitter) {
        String eventClientId = event.getClientId();
        if (eventClientId == null || event.getUserId() == null) {
            return;
        }

        RealmModel realm = session.getContext().getRealm();
        ClientModel client = realm.getClientByClientId(eventClientId);
        if (client == null) {
            return;
        }

        if (!Boolean.parseBoolean(client.getAttribute(ClientStreamStore.SSF_ENABLED_KEY))) {
            return;
        }
        if (!Boolean.parseBoolean(client.getAttribute(ClientStreamStore.SSF_AUTO_NOTIFY_ON_LOGIN_KEY))) {
            return;
        }

        // Only auto-tag when the receiver is NOT in broadcast mode.
        // Absent attribute → falls back to transmitter default (NONE),
        // so we skip only when explicitly set to ALL.
        DefaultSubjects defaultSubjects = DefaultSubjects.parseOrDefault(
                client.getAttribute(ClientStreamStore.SSF_DEFAULT_SUBJECTS_KEY), null);
        if (defaultSubjects == DefaultSubjects.ALL) {
            return;
        }

        UserModel user = session.users().getUserById(realm, event.getUserId());
        if (user == null) {
            return;
        }

        // Read through the pluggable inclusion resolver so an extension
        // that already considers the user notified (e.g. via a group
        // attribute or an external policy) doesn't get redundant
        // attribute writes layered on top. The write side stays on the
        // canonical user attribute via SsfNotifyAttributes.setForUser.
        // Organization membership counts as inclusion too — the
        // dispatcher's subject filter already honors org-level
        // ssf.notify.<clientId>, so a user who's notified via their
        // org doesn't need a redundant per-user attribute.
        if (!isUserNotified(transmitter, user, client)
                && !isAnyOrganizationNotified(transmitter, user, client)) {
            markAsNotified(user, client);
            log.debugf("SSF auto-notify on login: tagged user %s for receiver client %s",
                    user.getId(), client.getClientId());
        }
    }

    protected void markAsNotified(UserModel user, ClientModel client) {
        SsfNotifyAttributes.setForUser(user, client.getClientId());
    }

    protected boolean isUserNotified(SsfTransmitterProvider transmitter, UserModel user, ClientModel client) {
        return transmitter.subjectInclusionResolver().isUserNotified(session, user, client.getClientId());
    }

    /**
     * Mirrors the org-membership leg of the dispatcher's subject filter:
     * a user is effectively subscribed when any of their organizations
     * carries the {@code ssf.notify.<clientId>} attribute. We use the
     * same {@link SsfTransmitterProvider#subjectInclusionResolver()}
     * pluggable resolver as the dispatcher, so an extension that
     * defines org-inclusion differently stays the single source of
     * truth for both the auto-notify guard and the dispatch gate.
     */
    protected boolean isAnyOrganizationNotified(SsfTransmitterProvider transmitter, UserModel user, ClientModel client) {
        if (!Profile.isFeatureEnabled(Profile.Feature.ORGANIZATION)) {
            return false;
        }
        OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
        if (orgProvider == null) {
            return false;
        }
        String receiverClientId = client.getClientId();
        return orgProvider.getByMember(user)
                .anyMatch(org -> transmitter.subjectInclusionResolver()
                        .isOrganizationNotified(session, org, receiverClientId));
    }

    protected boolean shouldIgnoreEvent(Event event) {

        if (!Ssf.isTransmitterEnabled(session.getContext().getRealm())) {
            return true;
        }

        // ignore calls from user session expiration logic, as we only want to deliver user events
        return isUserSessionExpiration(event);
    }


    @Override
    public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {

        if (shouldIgnoreEvent(adminEvent)) {
            return;
        }

        SsfTransmitterProvider transmitter = session.getProvider(SsfTransmitterProvider.class);
        if (transmitter == null) {
            return;
        }

        // Ask the mapper first whether this admin event could even
        // produce a SET (currently only the "log out all user sessions"
        // path maps). If not, bail before hitting getAvailableStreams()
        // — every other admin user-resource operation (profile update,
        // group change, role assignment, etc.) would previously have
        // triggered a stream lookup that the mapper would then ignore.
        // The canConvert predicate also handles the ResourceType=USER guard
        // the old code had inline.
        if (!transmitter.securityEventTokenMapper().canConvert(adminEvent)) {
            return;
        }

        var streamTokens = generateSecurityEventTokensForAdminEvent(adminEvent, transmitter);
        if (streamTokens == null || streamTokens.isEmpty()) {
            return;
        }
        dispatchSecurityEventTokens(streamTokens, transmitter);
    }

    protected boolean shouldIgnoreEvent(AdminEvent adminEvent) {

        if (!Ssf.isTransmitterEnabled(session.getContext().getRealm())) {
            return true;
        }

        return false;
    }

    protected void dispatchSecurityEventTokens(List<Map.Entry<SsfSecurityEventToken, StreamConfig>> streamTokens, SsfTransmitterProvider transmitter) {
        for (var streamToken : streamTokens) {
            dispatchSecurityEventToken(streamToken.getKey(), transmitter, streamToken.getValue());
        }
    }

    protected List<Map.Entry<SsfSecurityEventToken, StreamConfig>> generateSecurityEventTokensForAdminEvent(AdminEvent adminEvent, SsfTransmitterProvider transmitter) {

        if (adminEvent.getResourceType() != ResourceType.USER) {
            return List.of();
        }

        StreamService streamService = transmitter.streamService();
        List<StreamConfig> streams = streamService.findStreamsForSsfReceiverClients();
        if (streams.isEmpty()) {
            log.warnf("No streams found. Discarding admin event %s", adminEvent.getId());
            return List.of();
        }

        RealmModel realm = session.realms().getRealm(adminEvent.getRealmId());
        UserModel eventUser = session.users().getUserById(realm, SsfUtil.userIdFromAdminEventPath(adminEvent));
        if (eventUser == null) {
            return List.of();
        }

        var streamTokens = new ArrayList<Map.Entry<SsfSecurityEventToken, StreamConfig>>();
        for (StreamConfig stream : streams) {

            if (!transmitter.securityEventTokenDispatcher().shouldDispatchForUser(eventUser, stream)) {
                // Subject-gate negative — skip the mapper call entirely.
                // Avoids building a token for a stream the dispatcher
                // would just filter out, and keeps logs honest for the
                // multi-receiver case where only some streams deliver.
                continue;
            }

            SsfSecurityEventToken securityEventToken = convertAdminEventToSecurityEventToken(adminEvent, transmitter, stream);
            if (securityEventToken == null) {
                log.debugf("Could not generate SSF Security Event Token for Admin Event. id=%s", adminEvent.getId());
                continue;
            }
            log.debugf("Generated SSF Security Event Token for Admin Event. "
                            + "realm=%s clientId=%s streamId=%s operationType=%s resourceType=%s resourcePath=%s jti=%s",
                    session.getContext().getRealm().getName(),
                    stream.getClientClientId(), stream.getStreamId(),
                    adminEvent.getOperationType(), adminEvent.getResourceType(),
                    adminEvent.getResourcePath(), securityEventToken.getJti());
            streamTokens.add(Map.entry(securityEventToken, stream));
        }
        return streamTokens;
    }

    protected SsfSecurityEventToken convertAdminEventToSecurityEventToken(AdminEvent adminEvent, SsfTransmitterProvider transmitter, StreamConfig stream) {
        return transmitter.securityEventTokenMapper().toSecurityEventToken(adminEvent, stream);
    }

    protected boolean isUserSessionExpiration(Event event) {
        return EventType.USER_SESSION_DELETED.equals(event.getType()) &&
               (Details.INVALID_USER_SESSION_REMEMBER_ME_REASON.equals(event.getDetails().get(Details.REASON))
               || Details.USER_SESSION_EXPIRED_REASON.equals(event.getDetails().get(Details.REASON)));
    }

    @Override
    public void close() {
        // No resources to close
    }

}
