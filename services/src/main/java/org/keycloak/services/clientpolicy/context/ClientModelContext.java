package org.keycloak.services.clientpolicy.context;

import org.keycloak.models.ClientModel;
import org.keycloak.services.clientpolicy.ClientPolicyContext;

/**
 * Represents {@link ClientPolicyContext} with access to the client, which can be used in underlying conditions/executors
 */
public interface ClientModelContext extends ClientPolicyContext {

    ClientModel getClient();
}
