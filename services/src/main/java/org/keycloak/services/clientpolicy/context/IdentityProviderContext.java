package org.keycloak.services.clientpolicy.context;

import org.keycloak.services.clientpolicy.ClientPolicyContext;

/**
 * Represents {@link org.keycloak.services.clientpolicy.ClientPolicyContext} with access to the identity provider, which can be used in underlying conditions/executors
 */
public interface IdentityProviderContext extends ClientPolicyContext {

    String getIdentityProviderAlias();
}
