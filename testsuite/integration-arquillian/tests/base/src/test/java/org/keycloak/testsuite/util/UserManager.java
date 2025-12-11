package org.keycloak.testsuite.util;

import java.util.ArrayList;
import java.util.Arrays;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;

import static org.keycloak.testsuite.admin.ApiUtil.findUserByUsernameId;

/**
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>.
 */
public class UserManager {

    private static RealmResource realm;

    private UserManager() {
    }

    public static UserManager realm(RealmResource realm) {
        UserManager.realm = realm;
        return new UserManager();
    }

    public UserManagerBuilder username(String username) {
        return new UserManagerBuilder(findUserByUsernameId(realm, username));
    }

    public UserManagerBuilder user(UserResource user) {
        return new UserManagerBuilder(user);
    }

    public class UserManagerBuilder {

        private final UserResource userResource;

        public UserManagerBuilder(UserResource userResource) {
            this.userResource = userResource;
        }

        public void removeRequiredAction(String action) {
            UserRepresentation user = initializeRequiredActions();
            user.getRequiredActions().remove(action);
            userResource.update(user);
        }

        public void addRequiredAction(String... actions) {
            UserRepresentation user = initializeRequiredActions();
            user.setRequiredActions(Arrays.asList(actions));
            userResource.update(user);
        }

        public void assignRoles(String... roles) {
            UserRepresentation user = userResource.toRepresentation();
            if (user != null && user.getRealmRoles() == null) {
                user.setRealmRoles(new ArrayList<String>());
            }
            user.setRealmRoles(Arrays.asList(roles));
            userResource.update(user);
        }

        public void enabled(Boolean enabled) {
            UserRepresentation user = userResource.toRepresentation();
            user.setEnabled(enabled);
            userResource.update(user);
        }


        private UserRepresentation initializeRequiredActions() {
            UserRepresentation user = userResource.toRepresentation();
            if (user != null && user.getRequiredActions() == null) {
                user.setRequiredActions(new ArrayList<String>());
            }
            return user;
        }

    }
}