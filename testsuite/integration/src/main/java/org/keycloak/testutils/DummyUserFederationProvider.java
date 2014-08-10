package org.keycloak.testutils;

import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserModel;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class DummyUserFederationProvider implements UserFederationProvider {

    private static Map<String, UserModel> users = new HashMap<String, UserModel>();

    @Override
    public UserModel proxy(UserModel local) {
        return local;
    }

    @Override
    public boolean synchronizeRegistrations() {
        return true;
    }

    @Override
    public UserModel register(RealmModel realm, UserModel user) {
        users.put(user.getUsername(), user);
        return user;
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        return users.remove(user.getUsername()) != null;
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        return users.get(username);
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        return null;
    }

    @Override
    public List<UserModel> searchByAttributes(Map<String, String> attributes, RealmModel realm, int maxResults) {
        return Collections.emptyList();
    }

    @Override
    public void preRemove(RealmModel realm) {

    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {

    }

    @Override
    public boolean isValid(UserModel local) {
        return false;
    }

    @Override
    public Set<String> getSupportedCredentialTypes(UserModel user) {
        return Collections.emptySet();
    }

    @Override
    public boolean validCredentials(RealmModel realm, UserModel user, List<UserCredentialModel> input) {
        return false;
    }

    @Override
    public boolean validCredentials(RealmModel realm, UserModel user, UserCredentialModel... input) {
        return false;
    }

    @Override
    public void close() {

    }
}
