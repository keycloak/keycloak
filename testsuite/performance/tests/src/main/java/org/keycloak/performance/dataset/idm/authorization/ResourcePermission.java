package org.keycloak.performance.dataset.idm.authorization;

import java.util.List;
import javax.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ResourcePermissionResource;
import org.keycloak.admin.client.resource.ResourcePermissionsResource;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;

/**
 *
 * @author tkyjovsk
 */
public class ResourcePermission extends Policy<ResourcePermissionRepresentation> {

    private List<Resource> resources;
    private List<Policy> policies;

    public ResourcePermission(ResourceServer resourceServer, int index) {
        super(resourceServer, index);
    }

    @Override
    public ResourcePermissionRepresentation newRepresentation() {
        return new ResourcePermissionRepresentation();
    }

    public List<Resource> getResources() {
        return resources;
    }

    public void setResources(List<Resource> resources) {
        this.resources = resources;
    }

    public List<Policy> getPolicies() {
        return policies;
    }

    public void setPolicies(List<Policy> policies) {
        this.policies = policies;
    }

    public ResourcePermissionsResource resourcePermissionsResource(Keycloak adminClient) {
        return getResourceServer().resource(adminClient).permissions().resource();
    }

    public ResourcePermissionResource resource(Keycloak adminClient) {
        return resourcePermissionsResource(adminClient).findById(getIdAndReadIfNull(adminClient));
    }

    @Override
    public ResourcePermissionRepresentation read(Keycloak adminClient) {
        return resourcePermissionsResource(adminClient).findByName(getRepresentation().getName());
    }

    @Override
    public Response create(Keycloak adminClient) {
        return resourcePermissionsResource(adminClient).create(getRepresentation());
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
