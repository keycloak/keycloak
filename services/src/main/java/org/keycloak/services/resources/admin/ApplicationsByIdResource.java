package org.keycloak.services.resources.admin;

import org.keycloak.models.ApplicationModel;
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
    protected ApplicationModel getApplicationByPathParam(String id) {
        return realm.getApplicationById(id);
    }

    @Override
    protected String getApplicationPath(ApplicationModel applicationModel) {
        return applicationModel.getId();
    }

}
