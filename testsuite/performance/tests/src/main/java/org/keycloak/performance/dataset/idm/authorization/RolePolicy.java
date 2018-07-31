package org.keycloak.performance.dataset.idm.authorization;

import java.util.List;
import javax.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RolePoliciesResource;
import org.keycloak.admin.client.resource.RolePolicyResource;
import org.keycloak.performance.dataset.idm.Role;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;

/**
 *
 * @author tkyjovsk
 */
public class RolePolicy extends Policy<RolePolicyRepresentation> {

    private List<Role> roles;

    public RolePolicy(ResourceServer resourceServer, int index) {
        super(resourceServer, index);
    }

    @Override
    public RolePolicyRepresentation newRepresentation() {
        return new RolePolicyRepresentation();
    }

    public List<Role> getRoles() {
        return roles;
    }

    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }

    public RolePoliciesResource rolePoliciesResource(Keycloak adminClient) {
        return getResourceServer().resource(adminClient).policies().role();
    }

    public RolePolicyResource resource(Keycloak adminClient) {
        return rolePoliciesResource(adminClient).findById(getIdAndReadIfNull(adminClient));
    }

    @Override
    public RolePolicyRepresentation read(Keycloak adminClient) {
        return rolePoliciesResource(adminClient).findByName(getRepresentation().getName());
    }

    @Override
    public Response create(Keycloak adminClient) {
        return rolePoliciesResource(adminClient).create(getRepresentation());
    }

    @Override
    public void update(Keycloak adminClient) {
        resource(adminClient).update(getRepresentation());
    }

    @Override
    public void delete(Keycloak adminClient) {
        resource(adminClient).remove();
    }

}
