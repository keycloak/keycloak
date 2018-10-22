package org.keycloak.performance.dataset.idm.authorization;

import java.util.List;
import javax.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ScopePermissionResource;
import org.keycloak.admin.client.resource.ScopePermissionsResource;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;

/**
 *
 * @author tkyjovsk
 */
public class ScopePermission extends Policy<ScopePermissionRepresentation> {

    private List<Scope> scopes;
    private List<Policy> policies;

    public ScopePermission(ResourceServer resourceServer, int index) {
        super(resourceServer, index);
    }

    @Override
    public ScopePermissionRepresentation newRepresentation() {
        return new ScopePermissionRepresentation();
    }

    public ScopePermissionsResource scopePermissionsResource(Keycloak adminClient) {
        return getResourceServer().resource(adminClient).permissions().scope();
    }

    public ScopePermissionResource resource(Keycloak adminClient) {
        return scopePermissionsResource(adminClient).findById(getIdAndReadIfNull(adminClient));
    }

    @Override
    public ScopePermissionRepresentation read(Keycloak adminClient) {
        return scopePermissionsResource(adminClient).findByName(getRepresentation().getName());
    }

    @Override
    public Response create(Keycloak adminClient) {
        return scopePermissionsResource(adminClient).create(getRepresentation());
    }

    @Override
    public void update(Keycloak adminClient) {
        resource(adminClient).update(getRepresentation());
    }

    @Override
    public void delete(Keycloak adminClient) {
        resource(adminClient).remove();
    }

    public List<Policy> getPolicies() {
        return policies;
    }

    public void setPolicies(List<Policy> policies) {
        this.policies = policies;
    }

    public List<Scope> getScopes() {
        return scopes;
    }

    public void setScopes(List<Scope> scopes) {
        this.scopes = scopes;
    }

}
