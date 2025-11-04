package org.keycloak.protocol.ssf.event;

import org.keycloak.protocol.ssf.support.SsfSetPushDeliveryFailureResponse;

public class ErrorSecurityEventToken extends SecurityEventToken {

    protected final SsfSetPushDeliveryFailureResponse failureResponse;

    public ErrorSecurityEventToken(String errorCode, String message) {
        this.failureResponse = new SsfSetPushDeliveryFailureResponse(errorCode, message);
    }

    public SsfSetPushDeliveryFailureResponse getFailureResponse() {
        return failureResponse;
    }

    @Override
    public String toString() {
        return "ErrorSecurityEventToken{" +
               "failureResponse=" + failureResponse +
               '}';
    }
}
