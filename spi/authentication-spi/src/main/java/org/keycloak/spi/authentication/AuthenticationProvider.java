package org.keycloak.spi.authentication;

import java.util.Map;

import org.keycloak.models.RealmModel;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface AuthenticationProvider {

    String getName();

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
