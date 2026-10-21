package org.keycloak.protocol.oidc.endpoints;

import org.keycloak.protocol.oidc.endpoints.AuthorizationEndpointChecker.AuthorizationCheckException;
import org.keycloak.provider.Provider;

public interface AuthorizationEndpointCheckProvider extends Provider {

    void check(AuthorizationEndpointChecker context) throws AuthorizationCheckException;
}
