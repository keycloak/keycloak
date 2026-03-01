package org.keycloak.protocol.oidc;

public enum LogoutTokenValidationCode {

    VALIDATION_SUCCESS(""),
    DECODE_TOKEN_FAILED("The decode of the logoutToken failed"),
    COULD_NOT_FIND_IDP("No Identity Provider has been found"),
    TOKEN_VERIFICATION_WITH_IDP_FAILED("LogoutToken verification with identity provider failed"),
    MISSING_SID_OR_SUBJECT("Missing sid or sub claim"),
    BACKCHANNEL_LOGOUT_EVENT_MISSING("The LogoutToken event claim is not as expected"),
    NONCE_CLAIM_IN_TOKEN("The LogoutToken contains a nonce claim which is not allowed"),
    MISSING_IAT_CLAIM("The LogoutToken doesn't contain an iat claim"),
    LOGOUT_TOKEN_ID_MISSING("The logoutToken jti is missing");

    private String errorMessage;

    LogoutTokenValidationCode(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    LogoutTokenValidationContext toCtx() {
        return new LogoutTokenValidationContext(this);
    }
}
