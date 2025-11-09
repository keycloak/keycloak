package org.keycloak.protocol.ssf.event.processor;

/**
 * Processor for the SecurityEvents contained in a {@link SsfSecurityEventContext}.
 */
public interface SsfSecurityEventProcessor {

    void processSecurityEvents(SsfSecurityEventContext context);
}
