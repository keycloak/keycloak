package org.keycloak.testsuite.admin;

import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;

public class AdminApiUtil extends org.keycloak.tests.utils.admin.AdminApiUtil {

    public static void enableRequiredAction(RealmResource realm, UserModel.RequiredAction requiredAction, boolean enabled) {
        AuthenticationManagementResource flows = realm.flows();
        RequiredActionProviderRepresentation action = flows.getRequiredAction(requiredAction.name());
        action.setEnabled(enabled);
        flows.updateRequiredAction(requiredAction.name(), action);
    }
}
