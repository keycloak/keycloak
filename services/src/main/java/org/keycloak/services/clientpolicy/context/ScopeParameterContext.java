package org.keycloak.services.clientpolicy.context;

import org.keycloak.services.clientpolicy.ClientPolicyContext;

/**
 * Represents {@link ClientPolicyContext} with access to the scope parameter, which can be used in underlying conditions/executors to check what
 * scope was used in the particular request
 */
public interface ScopeParameterContext extends ClientPolicyContext {

    String getScopeParameter();
}
