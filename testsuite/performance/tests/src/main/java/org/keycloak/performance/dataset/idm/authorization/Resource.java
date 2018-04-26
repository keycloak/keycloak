package org.keycloak.performance.dataset.idm.authorization;

import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.commons.lang.Validate;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ResourceResource;
import org.keycloak.admin.client.resource.ResourcesResource;
import org.keycloak.performance.dataset.NestedEntity;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.performance.dataset.Creatable;

/**
 *
 * @author tkyjovsk
 */
public class Resource extends NestedEntity<ResourceServer, ResourceRepresentation>
        implements Creatable<ResourceRepresentation> {

    private List<Scope> scopes;

    public Resource(ResourceServer resourceServer, int index) {
        super(resourceServer, index);
    }

    @Override
    public ResourceRepresentation newRepresentation() {
        return new ResourceRepresentation();
    }

    @Override
    public String toString() {
        return getRepresentation().getName();
    }

    public ResourceServer getResourceServer() {
        return getParentEntity();
    }

    public ResourcesResource resourcesResource(Keycloak adminClient) {
        return getResourceServer().resource(adminClient).resources();
    }

    public ResourceResource resource(Keycloak adminClient) {
        return resourcesResource(adminClient).resource(getIdAndReadIfNull(adminClient));
    }

    @Override
    public ResourceRepresentation read(Keycloak adminClient) {
        return resourcesResource(adminClient).findByName(getRepresentation().getName()).get(0);
    }

    @Override
    public Response create(Keycloak adminClient) {
        Validate.notNull(getResourceServer());
        Validate.notNull(getResourceServer().getClient());
        Validate.notNull(getResourceServer().getClient().getRepresentation().getBaseUrl());
        return resourcesResource(adminClient).create(getRepresentation());
    }

    @Override
    public void update(Keycloak adminClient) {
        resource(adminClient).update(getRepresentation());
    }

    @Override
    public void delete(Keycloak adminClient) {
        resource(adminClient).remove();
    }

    public List<Scope> getScopes() {
        return scopes;
    }

    public void setScopes(List<Scope> scopes) {
        this.scopes = scopes;
    }

}
