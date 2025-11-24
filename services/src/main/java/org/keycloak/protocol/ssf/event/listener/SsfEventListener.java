package org.keycloak.protocol.ssf.event.listener;

import org.keycloak.protocol.ssf.event.processor.SsfSecurityEventContext;
import org.keycloak.protocol.ssf.event.types.SsfEvent;

/**
 * Handles events delivered via SSF.
 */
public interface SsfEventListener {

    void onEvent(SsfSecurityEventContext eventContext, String eventId, SsfEvent event);

}
