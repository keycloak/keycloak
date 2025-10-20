package org.keycloak.protocol.ssf.event.listener;

import org.keycloak.protocol.ssf.event.types.SsfEvent;
import org.keycloak.protocol.ssf.event.processor.SsfEventContext;

public interface SsfEventListener {

    void onEvent(SsfEventContext eventContext, String eventId, SsfEvent event);

}
