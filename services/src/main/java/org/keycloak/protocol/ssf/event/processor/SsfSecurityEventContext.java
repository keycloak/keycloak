package org.keycloak.protocol.ssf.event.processor;

import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.ssf.event.SecurityEventToken;
import org.keycloak.protocol.ssf.receiver.SsfReceiver;

/**
 * Context object for SecurityEventToken processing.
 */
public class SsfSecurityEventContext {

    protected KeycloakSession session;

    protected SsfReceiver receiver;

    protected SecurityEventToken securityEventToken;

    protected boolean processedSuccessfully;

    public SecurityEventToken getSecurityEventToken() {
        return securityEventToken;
    }

    public void setSecurityEventToken(SecurityEventToken securityEventToken) {
        this.securityEventToken = securityEventToken;
    }

    protected void setProcessedSuccessfully(boolean processedSuccessfully) {
        this.processedSuccessfully = processedSuccessfully;
    }

    public boolean isProcessedSuccessfully() {
        return processedSuccessfully;
    }

    public KeycloakSession getSession() {
        return session;
    }

    public void setSession(KeycloakSession session) {
        this.session = session;
    }

    public SsfReceiver getReceiver() {
        return receiver;
    }

    public void setReceiver(SsfReceiver receiver) {
        this.receiver = receiver;
    }
}
