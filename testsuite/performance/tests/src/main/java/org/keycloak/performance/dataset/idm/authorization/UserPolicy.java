package org.keycloak.performance.dataset.idm.authorization;

import java.util.List;
import javax.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserPoliciesResource;
import org.keycloak.admin.client.resource.UserPolicyResource;
import org.keycloak.performance.dataset.idm.User;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;

/**
 *
 * @author tkyjovsk
 */
public class UserPolicy extends Policy<UserPolicyRepresentation> {

    private List<User> users;

    public UserPolicy(ResourceServer resourceServer, int index) {
        super(resourceServer, index);
    }

    @Override
    public UserPolicyRepresentation newRepresentation() {
        return new UserPolicyRepresentation();
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public UserPoliciesResource userPoliciesResource(Keycloak adminClient) {
        return policies(adminClient).user();
    }

    public UserPolicyResource resource(Keycloak adminClient) {
        return userPoliciesResource(adminClient).findById(getIdAndReadIfNull(adminClient));
    }

    @Override
    public UserPolicyRepresentation read(Keycloak adminClient) {
        return userPoliciesResource(adminClient).findByName(getRepresentation().getName());
    }

    @Override
    public Response create(Keycloak adminClient) {
        return userPoliciesResource(adminClient).create(getRepresentation());
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
