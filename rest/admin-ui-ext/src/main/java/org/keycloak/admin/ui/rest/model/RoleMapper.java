package org.keycloak.admin.ui.rest.model;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;

public class RoleMapper {
    public static ClientRole convertToModel(RoleModel roleModel, RealmModel realm) {
        ClientModel clientModel = realm.getClientById(roleModel.getContainerId());
        if (clientModel==null) {
            throw new IllegalArgumentException("Could not find referenced client");
        }
        ClientRole clientRole = new ClientRole(roleModel.getId(), roleModel.getName(), roleModel.getDescription());
        clientRole.setClientId(clientModel.getId());
        clientRole.setClient(clientModel.getClientId());
        return clientRole;
    }
}
