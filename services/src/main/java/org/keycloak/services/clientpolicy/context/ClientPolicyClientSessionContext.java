package org.keycloak.services.clientpolicy.context;

import org.keycloak.OAuth2Constants;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.services.clientpolicy.ClientPolicyContext;

/**
 * Represents {@link ClientPolicyContext} with access to the {@link AuthenticatedClientSessionModel}, which can be used in underlying conditions/executors
 */
public interface ClientPolicyClientSessionContext extends ClientModelContext, ScopeParameterContext {

    AuthenticatedClientSessionModel getClientSession();

    @Override
    default ClientModel getClient() {
        return getClientSession().getClient();
    }

    @Override
    default String getScopeParameter() {
        return getClientSession().getNote(OAuth2Constants.SCOPE);
    }

}
