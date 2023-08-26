package org.keycloak.admin.ui.rest.model;

import java.util.stream.Stream;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RoleModel;

public class RoleMapper {

    public static ClientRole convertToModel(RoleModel roleModel, Stream<ClientModel> clients) {
        ClientRole clientRole = new ClientRole(roleModel.getId(), roleModel.getName(), roleModel.getDescription());
        ClientModel clientModel = clients.filter(c -> roleModel.getContainerId().equals(c.getId())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Could not find referenced client"));
        clientRole.setClientId(clientModel.getId());
        clientRole.setClient(clientModel.getClientId());
        return clientRole;
    }
}
