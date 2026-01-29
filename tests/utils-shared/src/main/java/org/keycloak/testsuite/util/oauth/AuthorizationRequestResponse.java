package org.keycloak.testsuite.util.oauth;

import java.util.Map;
import java.util.Optional;

public class AuthorizationRequestResponse extends AuthorizationEndpointResponse {

    public AuthorizationRequestResponse(AbstractOAuthClient<?> client) {
        super(client);
    }

    public AuthorizationRequestResponse(Map<String, String> params) {
        super(params);
    }

    public String assertCode() {
        return Optional.ofNullable(getCode()).orElseThrow(() ->
                new IllegalStateException(String.format("[%s] %s", getError(), getErrorDescription())));
    }
}
