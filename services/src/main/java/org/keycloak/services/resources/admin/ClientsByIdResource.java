package org.keycloak.services.resources.admin;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientsByIdResource extends ClientsResource {
    public ClientsByIdResource(RealmModel realm, RealmAuth auth, AdminEventBuilder adminEvent) {
        super(realm, auth, adminEvent);
    }

    @Override
    protected ClientModel getClientByPathParam(String id) {
        return realm.getClientById(id);
    }

    @Override
    protected String getClientPath(ClientModel clientModel) {
        return clientModel.getId();
    }

}
