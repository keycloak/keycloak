package org.keycloak.performance.dataset.idm;

import org.keycloak.admin.client.Keycloak;

/**
 *
 * @author tkyjovsk
 */
public class ClientRoleMappings<RM extends RoleMapper> extends RoleMappings<RM> {

    private final Client client;

    public ClientRoleMappings(RM roleMapper, Client client, RoleMappingsRepresentation representation) {
        super(roleMapper, representation);
        this.client = client;
    }

    @Override
    public String toString() {
        return String.format("%s/role-mappings/%s", getRoleMapper(), getClient());
    }

    @Override
    public RoleMapper getRoleMapper() {
        return getParentEntity();
    }

    public Client getClient() {
        return client;
    }

    @Override
    public void update(Keycloak adminClient) {
        getRoleMapper()
                .roleMappingResource(adminClient)
                .clientLevel(getClient().getId())
                .add(getRepresentation());
    }

}
