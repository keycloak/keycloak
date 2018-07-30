package org.keycloak.performance.dataset.idm;

import javax.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.GroupResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.performance.dataset.Creatable;

/**
 *
 * @author tkyjovsk
 */
public class Group extends RoleMapper<GroupRepresentation> implements Creatable<GroupRepresentation> {

    public Group(Realm realm, int index) {
        super(realm, index);
    }

    @Override
    public GroupRepresentation newRepresentation() {
        return new GroupRepresentation();
    }

    @Override
    public String toString() {
        return getRepresentation().getName();
    }

    @Override
    public RoleMappingResource roleMappingResource(Keycloak adminClient) {
        throw new UnsupportedOperationException();
    }

    public GroupResource resource(Keycloak adminClient) {
        return getRealm().resource(adminClient).groups().group(getIdAndReadIfNull(adminClient));
    }

    @Override
    public GroupRepresentation read(Keycloak adminClient) {
        return getRealm().resource(adminClient).groups().groups(getRepresentation().getName(), 0, 1).get(0);
    }

    @Override
    public Response create(Keycloak adminClient) {
        return getRealm().resource(adminClient).groups().add(getRepresentation());
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
