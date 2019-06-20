package org.keycloak.testsuite.updaters;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.UserRepresentation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

/**
 * Updater for user attributes. See {@link ServerResourceUpdater} for further details.
 * @author hmlnarik
 */
public class UserAttributeUpdater extends ServerResourceUpdater<UserAttributeUpdater, UserResource, UserRepresentation> {

    /**
     * Creates a {@UserAttributeUpdater} for the given user. The user must exist.
     * @param adminClient
     * @param realm
     * @param clientId
     * @return
     */
    public static UserAttributeUpdater forUserByUsername(Keycloak adminClient, String realm, String userName) {
        UsersResource users = adminClient.realm(realm).users();
        List<UserRepresentation> foundUsers = users.search(userName).stream()
          .filter(ur -> userName.equalsIgnoreCase(ur.getUsername()))
          .collect(Collectors.toList());
        assertThat(foundUsers, hasSize(1));
        UserResource userRes = users.get(foundUsers.get(0).getId());

        return new UserAttributeUpdater(userRes);
    }

    public UserAttributeUpdater(UserResource resource) {
        super(resource, resource::toRepresentation, resource::update);
        if (this.rep.getAttributes() == null) {
            this.rep.setAttributes(new HashMap<>());
        }
    }

    public UserAttributeUpdater setAttribute(String name, List<String> value) {
        this.rep.getAttributes().put(name, value);
        return this;
    }

    public UserAttributeUpdater setAttribute(String name, String... values) {
        this.rep.getAttributes().put(name, Arrays.asList(values));
        return this;
    }

    public UserAttributeUpdater removeAttribute(String name) {
        this.rep.getAttributes().put(name, null);
        return this;
    }

    public UserAttributeUpdater setEmailVerified(Boolean emailVerified) {
        rep.setEmailVerified(emailVerified);
        return this;
    }

    public UserAttributeUpdater setRequiredActions(UserModel.RequiredAction... requiredAction) {
        rep.setRequiredActions(Arrays.stream(requiredAction)
                .map(action -> action.name())
                .collect(Collectors.toList())
        );
        return this;
    }

    public RoleScopeUpdater realmRoleScope() {
        return new RoleScopeUpdater(resource.roles().realmLevel());
    }

    public RoleScopeUpdater clientRoleScope(String clientUUID) {
        return new RoleScopeUpdater(resource.roles().clientLevel(clientUUID));
    }
}
