package org.keycloak.testsuite.util.oauth;

import org.keycloak.util.Strings;

public class AuthorizationRequestResponse extends AuthorizationEndpointResponse {

    public AuthorizationRequestResponse(AbstractOAuthClient<?> client) {
        super(client);
    }

    @Override
    public String getCode() {
        String authCode = super.getCode();
        if (Strings.isEmpty(authCode) && !Strings.isEmpty(getErrorDescription())) {
            throw new IllegalStateException(String.format("[%s] %s", getError(), getErrorDescription()));
        }
        return authCode;
    }
}
