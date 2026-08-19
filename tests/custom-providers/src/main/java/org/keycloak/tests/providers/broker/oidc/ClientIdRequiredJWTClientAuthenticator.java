package org.keycloak.tests.providers.broker.oidc;

import java.util.Collections;
import java.util.Set;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.ClientAuthenticationFlowContext;
import org.keycloak.authentication.authenticators.client.ClientAuthUtil;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;

/**
 * A {@link JWTClientAuthenticator} that requires the optional client_id parameter.
 */
public class ClientIdRequiredJWTClientAuthenticator extends JWTClientAuthenticator {

    public static final String PROVIDER_ID = "testsuite-client-id-required";

    @Override
    public void authenticateClient(ClientAuthenticationFlowContext context) {
        MultivaluedMap<String, String> params = context.getHttpRequest().getDecodedFormParameters();

        String clientId = params.getFirst(OAuth2Constants.CLIENT_ID);
        if (clientId == null) {
            Response challengeResponse = ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(),
                    "invalid_client", "Missing client_id parameter");
            context.challenge(challengeResponse);
            return;
        }

        super.authenticateClient(context);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Set<String> getProtocolAuthenticatorMethods(String loginProtocol) {
        // Do not add as it will affect the well known provider test
        return Collections.emptySet();
    }
}
