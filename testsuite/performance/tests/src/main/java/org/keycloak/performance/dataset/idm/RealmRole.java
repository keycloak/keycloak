package org.keycloak.performance.dataset.idm;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RoleByIdResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.representations.idm.RoleRepresentation;

/**
 *
 * @author tkyjovsk
 */
public class RealmRole extends Role<Realm> {

    public RealmRole(Realm realm, int index) {
        super(realm, index);
    }

    public Realm getRealm() {
        return getParentEntity();
    }

    @Override
    public RolesResource rolesResource(Keycloak adminClient) {
        return getRealm().resource(adminClient).roles();
    }

    @Override
    public RoleByIdResource roleByIdResource(Keycloak adminClient) {
        return getRealm().resource(adminClient).rolesById();
    }

    @Override
    public RoleRepresentation newRepresentation() {
        return new RoleRepresentation();
    }

}
