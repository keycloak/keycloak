package org.keycloak.performance.dataset.idm;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.performance.dataset.NestedEntity;

/**
 *
 * @author tkyjovsk
 */
public abstract class RoleMapper<R> extends NestedEntity<Realm, R> {

    public RoleMapper(Realm realm, int index) {
        super(realm, index);
    }

    public Realm getRealm() {
        return getParentEntity();
    }

    public abstract RoleMappingResource roleMappingResource(Keycloak adminClient);

}
