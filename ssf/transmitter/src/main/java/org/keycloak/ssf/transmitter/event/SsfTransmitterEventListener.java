package org.keycloak.ssf.transmitter.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.keycloak.events.Details;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.ssf.Ssf;
import org.keycloak.ssf.event.token.SsfSecurityEventToken;
import org.keycloak.ssf.transmitter.SsfTransmitterProvider;
import org.keycloak.ssf.transmitter.stream.StreamConfig;
import org.keycloak.ssf.transmitter.stream.StreamService;

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

        // The top-level canConvert() gate already verified the event type is
        // mappable, so every stream below will attempt a real conversion.
        // Per-stream null results are still handled defensively — e.g. if
        // the user was deleted mid-event and the subject lookup fails.
        var streamTokens = new ArrayList<Map.Entry<SsfSecurityEventToken, StreamConfig>>();
        for (StreamConfig stream : streams) {
            SsfSecurityEventToken securityEventToken = convertUserEventToSecurityEventToken(event, transmitter, stream);
            if (securityEventToken == null) {
                log.debugf("Could not generate SSF Security Event Token for User Event. id=%s", event.getId());
                continue;
            }
            log.debugf("Generated SSF Security Event Token for User Event. jti=%s", securityEventToken.getJti());
            streamTokens.add(Map.entry(securityEventToken, stream));
        }
        return streamTokens;
    }

    protected void dispatchSecurityEventToken(SsfSecurityEventToken securityEventToken, SsfTransmitterProvider transmitter, StreamConfig stream) {
        transmitter.securityEventTokenDispatcher().dispatchEvent(securityEventToken, stream);
    }

    protected SsfSecurityEventToken convertUserEventToSecurityEventToken(Event event, SsfTransmitterProvider transmitter, StreamConfig stream) {
        return transmitter.securityEventTokenMapper().toSecurityEvent(event, stream);
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

    protected void dispatchSecurityEventTokens(List<Map.Entry<SsfSecurityEventToken, StreamConfig>> streamTokens, SsfTransmitterProvider transmitter) {
        for (var streamToken : streamTokens) {
            dispatchSecurityEventToken(streamToken.getKey(), transmitter, streamToken.getValue());
        }
    }

    protected List<Map.Entry<SsfSecurityEventToken, StreamConfig>> generateSecurityEventTokensForAdminEvent(AdminEvent adminEvent, SsfTransmitterProvider transmitter) {

        StreamService streamService = transmitter.streamService();
        List<StreamConfig> streams = streamService.findStreamsForSsfReceiverClients();
        if (streams.isEmpty()) {
            log.warnf("No streams found. Discarding admin event %s", adminEvent.getId());
            return List.of();
        }

        var streamTokens = new ArrayList<Map.Entry<SsfSecurityEventToken, StreamConfig>>();
        for (StreamConfig stream : streams) {
            SsfSecurityEventToken securityEventToken = convertAdminEventToSecurityEventToken(adminEvent, transmitter, stream);
            if (securityEventToken == null) {
                log.debugf("Could not generate SSF Security Event Token for Admin Event. id=%s", adminEvent.getId());
                continue;
            }
            log.debugf("Generated SSF Security Event Token for Admin Event. jti=%s", securityEventToken.getJti());
            streamTokens.add(Map.entry(securityEventToken, stream));
        }
        return streamTokens;
    }

    protected SsfSecurityEventToken convertAdminEventToSecurityEventToken(AdminEvent adminEvent, SsfTransmitterProvider transmitter, StreamConfig stream) {
        return transmitter.securityEventTokenMapper().toSecurityEvent(adminEvent, stream);
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
