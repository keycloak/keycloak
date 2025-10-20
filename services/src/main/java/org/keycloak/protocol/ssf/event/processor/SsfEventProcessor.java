package org.keycloak.protocol.ssf.event.processor;

public interface SsfEventProcessor {

    void processSecurityEvents(SsfEventContext context);
}
