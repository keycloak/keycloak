package org.keycloak.services.resources.admin;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ApplicationsByIdResource extends ApplicationsResource {
    public ApplicationsByIdResource(RealmModel realm, RealmAuth auth) {
        super(realm, auth);
    }

    @Override
    protected ClientModel getApplicationByPathParam(String id) {
        return realm.getClientById(id);
    }

    @Override
    protected String getApplicationPath(ClientModel clientModel) {
        return clientModel.getId();
    }

}
