package org.keycloak.performance.dataset.idm;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RoleByIdResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.representations.idm.RoleRepresentation;

/**
 *
 * @author tkyjovsk
 */
public class ClientRole extends Role<Client> {

    public ClientRole(Client client, int index) {
        super(client, index);
    }

    public Client getClient() {
        return getParentEntity();
    }

    @Override
    public RolesResource rolesResource(Keycloak adminClient) {
        return getClient().resource(adminClient).roles();
    }

    @Override
    public RoleByIdResource roleByIdResource(Keycloak adminClient) {
        return getClient().getRealm().resource(adminClient).rolesById();
    }

    @Override
    public RoleRepresentation newRepresentation() {
        return new RoleRepresentation();
    }

}
