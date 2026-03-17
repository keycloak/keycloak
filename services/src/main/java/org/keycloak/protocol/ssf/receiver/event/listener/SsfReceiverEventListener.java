package org.keycloak.protocol.ssf.receiver.event.listener;

import org.keycloak.protocol.ssf.event.SsfEvent;
import org.keycloak.protocol.ssf.receiver.event.processor.SsfEventContext;

/**
 * Handles events received via SSF.
 */
public interface SsfReceiverEventListener {

    void onEvent(SsfEventContext eventContext, String eventId, SsfEvent event);

}
