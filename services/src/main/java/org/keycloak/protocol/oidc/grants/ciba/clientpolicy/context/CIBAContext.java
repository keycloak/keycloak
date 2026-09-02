package org.keycloak.protocol.oidc.grants.ciba.clientpolicy.context;

import org.keycloak.models.ClientModel;
import org.keycloak.protocol.oidc.grants.ciba.channel.CIBAAuthenticationRequest;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.context.ClientModelContext;
import org.keycloak.services.clientpolicy.context.ScopeParameterContext;

/**
 * Represents {@link ClientPolicyContext}, which is used in the CIBA requests/responses
 */
public interface CIBAContext extends ScopeParameterContext, ClientModelContext {

    CIBAAuthenticationRequest getParsedRequest();

    @Override
    default ClientModel getClient() {
        if (getParsedRequest() == null) return null;
        return getParsedRequest().getClient();
    }

    @Override
    default String getScopeParameter() {
        if (getParsedRequest() == null) return null;
        return getParsedRequest().getScope();
    }
}
