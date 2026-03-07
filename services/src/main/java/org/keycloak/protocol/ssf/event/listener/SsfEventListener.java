package org.keycloak.protocol.ssf.event.listener;

import org.keycloak.protocol.ssf.event.processor.SsfEventContext;
import org.keycloak.protocol.ssf.event.types.SsfEvent;

/**
 * Handles events delivered via SSF.
 */
public interface SsfEventListener {

    void onEvent(SsfEventContext eventContext, String eventId, SsfEvent event);

}
