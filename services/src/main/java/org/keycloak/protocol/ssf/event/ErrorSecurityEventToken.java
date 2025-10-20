package org.keycloak.protocol.ssf.event;

import org.keycloak.protocol.ssf.support.SsfFailureResponse;

public class ErrorSecurityEventToken extends SecurityEventToken {

    protected final SsfFailureResponse failureResponse;

    public ErrorSecurityEventToken(String errorCode, String message) {
        this.failureResponse = new SsfFailureResponse(errorCode, message);
    }

    public SsfFailureResponse getFailureResponse() {
        return failureResponse;
    }

    @Override
    public String toString() {
        return "ErrorSecurityEventToken{" +
               "failureResponse=" + failureResponse +
               '}';
    }
}
