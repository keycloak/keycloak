package org.keycloak.protocol.ssf.transmitter.event;

import org.jboss.logging.Logger;

import org.keycloak.Config;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import org.keycloak.protocol.ssf.event.token.SsfSecurityEventToken;
import org.keycloak.protocol.ssf.transmitter.SsfTransmitterProvider;

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

        SsfSecurityEventToken securityEventToken = convertUserEventToSecurityEventToken(event, transmitter);
        if (securityEventToken == null) {
            return;
        }

        try {
            log.debugf("Generated SSF Security Event Token for User Event. jti=%s", securityEventToken.getJti());
            dispatchSecurityEventToken(securityEventToken, transmitter);
        } catch (Exception e) {
            log.warn("Failed to deliver SSF Security Event for User Event", e);
        }
    }

    protected void dispatchSecurityEventToken(SsfSecurityEventToken securityEventToken, SsfTransmitterProvider transmitter) {
        transmitter.securityEventTokenDispatcher().dispatchEvent(securityEventToken);
    }

    protected SsfSecurityEventToken convertUserEventToSecurityEventToken(Event event, SsfTransmitterProvider transmitter) {
        return transmitter.securityEventTokenMapper().toSecurityEvent(event);
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

        SsfTransmitterProvider transmitter = session.getProvider(SsfTransmitterProvider.class);
        if (transmitter == null) {
            return;
        }

        SsfSecurityEventToken securityEventToken = convertAdminEventToSecurityEventToken(adminEvent, transmitter);
        if (securityEventToken == null) {
            return;
        }

        log.debugf("Generated SSF Security Event Token for Admin user Event. jti=%s", securityEventToken.getJti());
        try {
            dispatchSecurityEventToken(securityEventToken, transmitter);
        } catch (Exception e) {
            log.warn("Failed to deliver SSF Security Event for Admin user Event", e);
        }
    }

    protected SsfSecurityEventToken convertAdminEventToSecurityEventToken(AdminEvent adminEvent, SsfTransmitterProvider transmitter) {
        return transmitter.securityEventTokenMapper().toSecurityEvent(adminEvent);
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

    public static class Factory implements EventListenerProviderFactory {

        private static final String ID = "ssf-events";

        @Override
        public EventListenerProvider create(KeycloakSession session) {
           // Create and return the event mapper
            return new SsfTransmitterEventListener(session);
        }


        @Override
        public void init(Config.Scope config) {
            // No initialization needed
        }

        @Override
        public void postInit(KeycloakSessionFactory factory) {
            // NOOP
        }

        @Override
        public void close() {
            // No resources to close
        }

        @Override
        public String getId() {
            return ID;
        }
    }

}
