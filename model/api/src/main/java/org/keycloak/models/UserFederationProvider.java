package org.keycloak.models;

import org.keycloak.provider.Provider;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserFederationProvider extends Provider {

    public static final String USERNAME = UserModel.USERNAME;
    public static final String EMAIL = UserModel.EMAIL;
    public static final String FIRST_NAME = UserModel.EMAIL;
    public static final String LAST_NAME = UserModel.LAST_NAME;

    UserModel proxy(UserModel local);
    boolean isRegistrationSupported();
    UserModel register(RealmModel realm, UserModel user);
    boolean removeUser(RealmModel realm, UserModel user);

    /**
     * Required to import into local storage any user found.
     *
     * @param realm
     * @param username
     * @return
     */
    UserModel getUserByUsername(RealmModel realm, String username);

    /**
     * Required to import into local storage any user found.
     *
     * @param realm
     * @param email
     * @return
     */
    UserModel getUserByEmail(RealmModel realm, String email);

    /**
     * Required to import into local storage any user found.  Must not import if user already exists in KeycloakSession.userStorage()!
     * Currently only attributes USERNAME, EMAIL, FIRST_NAME and LAST_NAME will be used.
     *
     * @param attributes
     * @param realm
     * @return
     */
    List<UserModel> searchByAttributes(Map<String, String> attributes, RealmModel realm);

    void preRemove(RealmModel realm);
    void preRemove(RealmModel realm, RoleModel role);

    boolean isValid(UserModel local);
    Set<String> getSupportedCredentialTypes();
    boolean validCredentials(RealmModel realm, UserModel user, List<UserCredentialModel> input);
    boolean validCredentials(RealmModel realm, UserModel user, UserCredentialModel... input);
    void close();}
