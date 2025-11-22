package org.keycloak.protocol.ssf.event.listener;

import org.keycloak.protocol.ssf.event.types.SsfEvent;
import org.keycloak.protocol.ssf.event.processor.SsfSecurityEventContext;

/**
 * Handles events delivered via SSF.
 */
public interface SsfEventListener {

    void onEvent(SsfSecurityEventContext eventContext, String eventId, SsfEvent event);

}
