package org.keycloak.protocol.ssf.event.listener;

import org.keycloak.protocol.ssf.event.types.SsfEvent;
import org.keycloak.protocol.ssf.event.processor.SsfSecurityEventContext;

public interface SsfEventListener {

    void onEvent(SsfSecurityEventContext eventContext, String eventId, SsfEvent event);

}
