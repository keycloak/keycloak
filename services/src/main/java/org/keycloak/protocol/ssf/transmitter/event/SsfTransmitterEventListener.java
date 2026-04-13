package org.keycloak.protocol.ssf.transmitter.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.ssf.Ssf;
import org.keycloak.protocol.ssf.event.token.SsfSecurityEventToken;
import org.keycloak.protocol.ssf.transmitter.SsfTransmitterProvider;
import org.keycloak.protocol.ssf.transmitter.stream.StreamConfig;
import org.keycloak.protocol.ssf.transmitter.stream.StreamService;

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

        if (shouldIgnoreEvent(event) || !isSupportedEvent(event, null)) {
            return;
        }

        SsfTransmitterProvider transmitter = Ssf.transmitter();
        if (transmitter == null) {
            return;
        }

        var streamTokens = generateSecurityTokensForUserEvent(event, transmitter);
        if (streamTokens == null || streamTokens.isEmpty()) {
            return;
        }
        dispatchSecurityEventTokens(streamTokens, transmitter);
    }

    /**
     * Null means does any stream support this event at all.
     * @param event
     * @param stream
     * @return
     */
    public boolean isSupportedEvent(Event event, StreamConfig stream) {

        return switch (event.getType()) {
            case LOGOUT, UPDATE_CREDENTIAL -> true;
            default -> false;
        };
    }

    protected List<Map.Entry<SsfSecurityEventToken, StreamConfig>> generateSecurityTokensForUserEvent(Event event, SsfTransmitterProvider transmitter) {

        StreamService streamService = transmitter.streamService();
        List<StreamConfig> streams = streamService.findStreamsForSsfReceiverClients();
        if (streams.isEmpty()) {
            log.warnf("No streams found. Discarding user event %s", event.getId());
            return List.of();
        }

        var streamTokens = new ArrayList<Map.Entry<SsfSecurityEventToken, StreamConfig>>();

        for (StreamConfig stream : streams) {

            if (!isSupportedEvent(event, stream)) {
                continue;
            }

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
        // HACK ignore calls from user session expiration logic
        return isCalledFromUserSessionExpirationLogic();
    }


    @Override
    public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {

        if (!ResourceType.USER.equals(adminEvent.getResourceType())) {
            return;
        }

        SsfTransmitterProvider transmitter = Ssf.transmitter();
        if (transmitter == null) {
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
        List<StreamConfig> streams = streamService.getAvailableStreams();
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

    protected boolean isCalledFromUserSessionExpirationLogic() {
        for (var ste : Thread.currentThread().getStackTrace()) {
            if (ste.getClassName().contains("UserSessionExpirationLogic")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void close() {
        // No resources to close
    }

}
