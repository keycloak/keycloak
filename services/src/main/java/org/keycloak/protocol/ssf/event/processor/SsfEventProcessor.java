package org.keycloak.protocol.ssf.event.processor;

public interface SsfEventProcessor {

    void processSecurityEvents(SsfSecurityEventContext context);
}
