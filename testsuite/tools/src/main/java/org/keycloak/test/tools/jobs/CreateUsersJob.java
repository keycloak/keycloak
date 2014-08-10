package org.keycloak.test.tools.jobs;

import org.keycloak.models.ApplicationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;

import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class CreateUsersJob extends UsersJob {

    private String[] roles;

    public CreateUsersJob(String[] roles) {
        this.roles = roles;
    }

    @Override
    protected void before(KeycloakSession session) {
    }

    @Override
    protected void runIteration(KeycloakSession session, RealmModel realm, Map<String, ApplicationModel> apps, Set<RoleModel> realmRoles, Map<String, Set<RoleModel>> appRoles, int counter) {
        String username = prefix + "-" + counter;
        UserModel user = session.users().addUser(realm, username);
        user.setEnabled(true);
        user.setFirstName("First");
        user.setLastName("Last");
        user.setEmail(username + "@localhost");

        UserCredentialModel password = new UserCredentialModel();
        password.setType(UserCredentialModel.PASSWORD);
        password.setValue("password");

        user.updateCredential(password);

        for (String r : roles) {
            grantRole(user, r, realmRoles, appRoles);
        }
    }

}
