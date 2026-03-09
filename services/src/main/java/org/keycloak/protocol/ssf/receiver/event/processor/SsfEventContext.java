package org.keycloak.protocol.ssf.receiver.event.processor;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ssf.event.token.SsfSecurityEventToken;
import org.keycloak.protocol.ssf.receiver.SsfReceiver;

/**
 * Context object for SecurityEventToken processing.
 */
public class SsfEventContext {

    protected KeycloakSession session;

    protected SsfReceiver receiver;

    protected SsfSecurityEventToken securityEventToken;

    protected boolean processedSuccessfully;

    public SsfEventContext() {
    }

    public SsfSecurityEventToken getSecurityEventToken() {
        return securityEventToken;
    }

    public void setSecurityEventToken(SsfSecurityEventToken securityEventToken) {
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

    public RealmModel getRealm() {
        return session.getContext().getRealm();
    }
}
