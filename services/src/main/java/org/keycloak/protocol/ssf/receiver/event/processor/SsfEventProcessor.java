package org.keycloak.protocol.ssf.receiver.event.processor;

import org.keycloak.protocol.ssf.event.token.SsfSecurityEventToken;

/**
 * Processor for the SsfEvents contained in a {@link SsfEventContext}.
 */
public interface SsfEventProcessor {

    void processEvents(SsfSecurityEventToken securityEventToken, SsfEventContext eventContext);
}
