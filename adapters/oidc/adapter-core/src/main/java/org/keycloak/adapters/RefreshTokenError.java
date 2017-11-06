package org.keycloak.adapters;

public enum RefreshTokenError {

    VERIFICATION_FAILED,
    REFRESH_TOKEN_EXPIRED,
    TOKEN_TTL_INSUFFICIENT,
    UNAVAILABLE,
    UNEXPECTED;

    RefreshTokenError() {
    }

    public static RefreshTokenError convert(final ServerRequest.HttpFailure httpFailure) {

        if (httpFailure.getStatus() == 400) {
            return REFRESH_TOKEN_EXPIRED;
        } else if (httpFailure.getStatus() == 404 || httpFailure.getStatus() == 500) {
            return UNAVAILABLE;
        }

        return UNEXPECTED;

    }
}
