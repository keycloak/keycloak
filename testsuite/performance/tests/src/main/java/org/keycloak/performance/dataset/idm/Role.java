package org.keycloak.performance.dataset.idm;

import javax.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RoleByIdResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.performance.dataset.Entity;
import org.keycloak.performance.dataset.NestedEntity;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.performance.dataset.Creatable;

/**
 *
 * @author tkyjovsk
 * @param <PE>
 */
public abstract class Role<PE extends Entity> extends NestedEntity<PE, RoleRepresentation>
        implements Creatable<RoleRepresentation> {

    public Role(PE parentEntity, int index) {
        super(parentEntity, index);
    }

    @Override
    public RoleRepresentation newRepresentation() {
        return new RoleRepresentation();
    }

    @Override
    public String toString() {
        return getRepresentation().getName();
    }

    public abstract RolesResource rolesResource(Keycloak adminClient);

    public abstract RoleByIdResource roleByIdResource(Keycloak adminClient);

    public RoleResource resource(Keycloak adminClient) {
        return rolesResource(adminClient).get(getRepresentation().getName());
    }

    @Override
    public RoleRepresentation read(Keycloak adminClient) {
        return resource(adminClient).toRepresentation();
    }

    @Override
    public Response create(Keycloak adminClient) { // FIXME
        rolesResource(adminClient).create(getRepresentation());
        return null;
    }

    @Override
    public void update(Keycloak adminClient) {
        roleByIdResource(adminClient).updateRole(getId(), getRepresentation());
    }

    @Override
    public void delete(Keycloak adminClient) {
        roleByIdResource(adminClient).deleteRole(getId());
    }
}
