package org.keycloak.services.clientpolicy.context;

import org.keycloak.models.ClientModel;

/**
 * Context for client CRUD in cases when client model is already available (registered/read/update/updated/deleted)
 */
public interface ClientCRUDClientAvailableContext extends ClientCRUDContext, ClientModelContext {

    @Override
    default ClientModel getClient() {
        return getTargetClient();
    }
}
