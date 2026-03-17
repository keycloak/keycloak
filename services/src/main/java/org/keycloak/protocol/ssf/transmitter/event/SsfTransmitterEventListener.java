package org.keycloak.protocol.ssf.transmitter.event;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.ssf.Ssf;
import org.keycloak.protocol.ssf.event.token.SsfSecurityEventToken;
import org.keycloak.protocol.ssf.transmitter.SsfTransmitterProvider;

import org.jboss.logging.Logger;

import org.keycloak.protocol.ssf.transmitter.stream.StreamConfig;
import org.keycloak.protocol.ssf.transmitter.stream.StreamService;

import java.util.List;

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

        StreamService streamService = Ssf.transmitter().streamService();

        List<StreamConfig> streams = streamService.findAllEnabledStreams();

        if (streams.isEmpty()) {
            log.warnf("No streams found. Discarding user event %s", event.getId());
            return;
        }

        for (StreamConfig stream : streams) {
            SsfSecurityEventToken securityEventToken = convertUserEventToSecurityEventToken(event, transmitter, stream);
            if (securityEventToken == null) {
                return;
            }

            try {
                log.debugf("Generated SSF Security Event Token for User Event. jti=%s", securityEventToken.getJti());
                dispatchSecurityEventToken(securityEventToken, transmitter, stream);
            } catch (Exception e) {
                log.warn("Failed to deliver SSF Security Event for User Event", e);
            }
        }
    }

    protected void dispatchSecurityEventToken(SsfSecurityEventToken securityEventToken, SsfTransmitterProvider transmitter, StreamConfig stream) {
        transmitter.securityEventTokenDispatcher().dispatchEvent(securityEventToken);
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

        StreamService streamService = transmitter.streamService();
        List<StreamConfig> streams = streamService.getAvailableStreams();
        if (streams.isEmpty()) {
            log.warnf("No streams found. Discarding admin event %s", adminEvent.getId());
            return;
        }

        for (StreamConfig stream : streams) {

            SsfSecurityEventToken securityEventToken = convertAdminEventToSecurityEventToken(adminEvent, transmitter, stream);
            if (securityEventToken == null) {
                return;
            }

            log.debugf("Generated SSF Security Event Token for Admin user Event. jti=%s", securityEventToken.getJti());
            try {
                dispatchSecurityEventToken(securityEventToken, transmitter, stream);
            } catch (Exception e) {
                log.warn("Failed to deliver SSF Security Event for Admin user Event", e);
            }
        }
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
