package org.keycloak.models.users;

import org.keycloak.models.KeycloakTransaction;
import org.keycloak.provider.Provider;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface UserProvider extends Provider {

    KeycloakTransaction getTransaction();

    User addUser(String id, String username, Set<String> roles, String realm);

    boolean removeUser(String name, String realm);

    User getUserById(String id, String realm);
    User getUserByUsername(String username, String realm);
    User getUserByEmail(String email, String realm);
    User getUserByAttribute(String name, String value, String realm);

    List<User> getUsers(String realm);
    List<User> searchForUser(String search, String realm);
    List<User> searchForUserByAttributes(Map<String, String> attributes, String realm);

    /**
     * Returns features supported by the provider. A provider is required to at least support one of verifying credentials
     * or reading credentials.
     *
     * @param feature
     * @return
     */
    boolean supports(Feature feature);

    boolean verifyCredentials(User user, Credentials... credentials);

    void onRealmRemoved(String realm);
    void onRoleRemoved(String role);

    void close();

}
