package org.keycloak.performance.templates.idm;

import org.keycloak.performance.dataset.idm.Client;
import org.keycloak.performance.dataset.idm.ClientRole;
import org.keycloak.performance.templates.NestedEntityTemplate;
import org.keycloak.performance.util.ValidateNumber;
import org.keycloak.representations.idm.RoleRepresentation;

/**
 *
 * @author tkyjovsk
 */
public class ClientRoleTemplate extends NestedEntityTemplate<Client, ClientRole, RoleRepresentation> {

    public static final String CLIENT_ROLES_PER_CLIENT = "clientRolesPerClient";

    public final int clientRolesPerClient;
    public final int clientRolesTotal;

    public ClientRoleTemplate(ClientTemplate clientTemplate) {
        super(clientTemplate);
        this.clientRolesPerClient = getConfiguration().getInt(CLIENT_ROLES_PER_CLIENT, 0);
        this.clientRolesTotal = clientRolesPerClient * clientTemplate.clientsTotal;
    }

    public ClientTemplate clientTemplate() {
        return (ClientTemplate) getParentEntityTemplate();
    }

    @Override
    public int getEntityCountPerParent() {
        return clientRolesPerClient;
    }

    @Override
    public void validateConfiguration() {
        logger().info(String.format("%s: %s, total: %s", CLIENT_ROLES_PER_CLIENT, clientRolesPerClient, clientRolesTotal));
        ValidateNumber.minValue(clientRolesPerClient, 0);
    }

    @Override
    public ClientRole newEntity(Client parentEntity, int index) {
        return new ClientRole(parentEntity, index);
    }

    @Override
    public void processMappings(ClientRole entity) {
    }

}
