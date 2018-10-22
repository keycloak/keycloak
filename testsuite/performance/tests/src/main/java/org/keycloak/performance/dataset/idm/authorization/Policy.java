package org.keycloak.performance.dataset.idm.authorization;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.PoliciesResource;
import org.keycloak.performance.dataset.NestedEntity;
import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;
import org.keycloak.performance.dataset.Creatable;

/**
 *
 * @author tkyjovsk
 */
public abstract class Policy<PR extends AbstractPolicyRepresentation>
        extends NestedEntity<ResourceServer, PR>
        implements Creatable<PR> {

    public Policy(ResourceServer resourceServer, int index) {
        super(resourceServer, index);
    }

    @Override
    public String toString() {
        return getRepresentation().getName();
    }

    public ResourceServer getResourceServer() {
        return getParentEntity();
    }

    public PoliciesResource policies(Keycloak adminClient) {
        return getResourceServer().resource(adminClient).policies();
    }

}
