package org.keycloak.protocol.ssf.event.processor;

import org.keycloak.protocol.ssf.event.SecurityEventToken;

/**
 * Processor for the SsfEvents contained in a {@link SsfEventContext}.
 */
public interface SsfEventProcessor {

    void processEvents(SecurityEventToken securityEventToken, SsfEventContext eventContext);
}
