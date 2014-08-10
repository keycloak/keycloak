package org.keycloak.test.tools.jobs;

import org.keycloak.models.ApplicationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.RealmManager;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DeleteUsersJob extends UsersJob {

    private Iterator<UserModel> users;

    @Override
    protected void before(KeycloakSession session) {
        RealmModel realm = new RealmManager(session).getRealmByName(realmName);

        // TODO: pagination
        List<UserModel> users = (prefix==null) ? session.users().getUsers(realm) : session.users().searchForUser(prefix, realm);
        users = users.subList(start, start + count);

        this.users = users.iterator();
    }

    @Override
    protected void runIteration(KeycloakSession session, RealmModel realm, Map<String, ApplicationModel> apps, Set<RoleModel> realmRoles, Map<String, Set<RoleModel>> appRoles, int counter) {
        session.users().removeUser(realm, users.next());
    }
}
