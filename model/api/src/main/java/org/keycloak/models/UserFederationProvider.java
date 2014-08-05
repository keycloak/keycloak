package org.keycloak.models;

import org.keycloak.provider.Provider;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SPI for plugging in federation storage.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserFederationProvider extends Provider {

    public static final String USERNAME = UserModel.USERNAME;
    public static final String EMAIL = UserModel.EMAIL;
    public static final String FIRST_NAME = UserModel.FIRST_NAME;
    public static final String LAST_NAME = UserModel.LAST_NAME;

    /**
     * Optional type that can be by implementations to describe edit mode of federation storage
     *
     */
    enum EditMode {
        /**
         * federation storage is read-only
         */
        READ_ONLY,
        /**
         * federation storage is writable
         *
         */
        WRITABLE,
        /**
         * updates to user are stored locally and not synced with federation storage.
         *
         */
        UNSYNCED
    }


    /**
     * Gives the provider an option to proxy UserModels loaded from local storage.
     * This method is called whenever a UserModel is pulled from local storage.
     * For example, the LDAP provider proxies the UserModel and does on-demand synchronization with
     * LDAP whenever UserModel update methods are invoked.  It also overrides UserModel.updateCredential for the
     * credential types it supports
     *
     * @param local
     * @return
     */
    UserModel proxy(UserModel local);

    /**
     * Should user registrations be synchronized with this provider?
     * FYI, only one provider will be chosen (by priority) to have this synchronization
     *
     * @return
     */
    boolean synchronizeRegistrations();

    /**
     * Called if this federation provider has priority and supports synchronized registrations.
     *
     * @param realm
     * @param user
     * @return
     */
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
    List<UserModel> searchByAttributes(Map<String, String> attributes, RealmModel realm, int maxResults);

    /**
     * called whenever a Realm is removed
     *
     * @param realm
     */
    void preRemove(RealmModel realm);

    /**
     * called before a role is removed.
     *
     * @param realm
     * @param role
     */
    void preRemove(RealmModel realm, RoleModel role);

    /**
     * Is the Keycloak UserModel still valid and/or existing in federated storage?
     *
     * @param local
     * @return
     */
    boolean isValid(UserModel local);

    /**
     * What UserCredentialModel types should be handled by this provider for this user?  Keycloak will only call
     * validCredentials() with the credential types specified in this method.
     *
     * @return
     */
    Set<String> getSupportedCredentialTypes(UserModel user);

    /**
     * Validate credentials for this user.  This method will only be called with credential parameters supported
     * by this provider
     *
     * @param realm
     * @param user
     * @param input
     * @return
     */
    boolean validCredentials(RealmModel realm, UserModel user, List<UserCredentialModel> input);
    boolean validCredentials(RealmModel realm, UserModel user, UserCredentialModel... input);
    void close();

}
