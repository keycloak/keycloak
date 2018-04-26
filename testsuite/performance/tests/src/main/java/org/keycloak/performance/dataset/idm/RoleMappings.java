package org.keycloak.performance.dataset.idm;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.performance.dataset.NestedEntity;
import org.keycloak.performance.dataset.Updatable;

/**
 *
 * @author tkyjovsk
 * @param <RM> role-mapper parent entity (user or group)
 */
public class RoleMappings<RM extends RoleMapper> extends NestedEntity<RM, RoleMappingsRepresentation>
        implements Updatable<RoleMappingsRepresentation> {

    public RoleMappings(RM roleMapper, RoleMappingsRepresentation representation) {
        super(roleMapper);
        setRepresentation(representation);
    }

    @Override
    public RoleMappingsRepresentation newRepresentation() {
        return new RoleMappingsRepresentation();
    }

    public RoleMapper getRoleMapper() {
        return getParentEntity();
    }

    @Override
    public String toString() {
        return String.format("%s/role-mappings/realm", getRoleMapper());
    }

    public RoleScopeResource resource(Keycloak adminClient) {
        return getRoleMapper().roleMappingResource(adminClient).realmLevel();
    }

    @Override
    public void update(Keycloak adminClient) {
        resource(adminClient).add(getRepresentation());
    }

    @Override
    public void delete(Keycloak adminClient) {
        resource(adminClient).remove(getRepresentation());
    }

}
