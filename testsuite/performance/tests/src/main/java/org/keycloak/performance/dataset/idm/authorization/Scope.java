package org.keycloak.performance.dataset.idm.authorization;

import javax.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ResourceScopeResource;
import org.keycloak.admin.client.resource.ResourceScopesResource;
import org.keycloak.performance.dataset.NestedEntity;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.performance.dataset.Creatable;

/**
 *
 * @author tkyjovsk
 */
public class Scope extends NestedEntity<ResourceServer, ScopeRepresentation>
        implements Creatable<ScopeRepresentation> {

    public Scope(ResourceServer resourceServer, int index) {
        super(resourceServer, index);
    }

    @Override
    public ScopeRepresentation newRepresentation() {
        return new ScopeRepresentation();
    }

    @Override
    public String toString() {
        return getRepresentation().getName();
    }

    public ResourceServer getResourceServer() {
        return getParentEntity();
    }

    public ResourceScopesResource scopesResource(Keycloak adminClient) {
        return getResourceServer().resource(adminClient).scopes();
    }

    public ResourceScopeResource resource(Keycloak adminClient) {
        return scopesResource(adminClient).scope(getIdAndReadIfNull(adminClient));
    }

    @Override
    public ScopeRepresentation read(Keycloak adminClient) {
        return scopesResource(adminClient).findByName(getRepresentation().getName());
    }

    @Override
    public Response create(Keycloak adminClient) {
        return scopesResource(adminClient).create(getRepresentation());
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
