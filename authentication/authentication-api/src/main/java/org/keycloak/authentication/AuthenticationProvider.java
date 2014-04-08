package org.keycloak.authentication;

import java.util.List;
import java.util.Map;

import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface AuthenticationProvider extends Provider {

    String getName();

    /**
     * Get names of all available configuration options of current provider
     *
     * @return options or empty list if no options available
     */
    List<String> getAvailableOptions();

    /**
     * Get user by given username or email. Return user instance or null if user doesn't exists in this authentication provider
     *
     * @param realm
     * @param configuration
     * @param username or email
     * @return found user or null if user with given username doesn't exists
     * @throws AuthenticationProviderException
     */
    AuthUser getUser(RealmModel realm, Map<String, String> configuration, String username) throws AuthenticationProviderException;

    /**
     * Try to register user with this authentication provider
     *
     * @param realm
     * @param configuration
     * @param username
     * @return ID of newly created user (For example ID from LDAP)
     * @throws AuthenticationProviderException if user creation couldn't happen
     */
    String registerUser(RealmModel realm, Map<String, String> configuration, String username) throws AuthenticationProviderException;

    /**
     * Standard Authentication flow
     *
     * @param username
     * @param password
     * @return result of authentication, which might eventually encapsulate info about authenticated user and provider which successfully authenticated
     */
    AuthProviderStatus validatePassword(RealmModel realm, Map<String, String> configuration, String username, String password) throws AuthenticationProviderException;


    /**
     * Update credential
     *
     * @param realm
     * @param configuration
     * @param username
     * @param password
     * @return true if credential has been successfully updated
     * @throws AuthenticationProviderException
     */
    boolean updateCredential(RealmModel realm, Map<String, String> configuration, String username, String password) throws AuthenticationProviderException;


}
