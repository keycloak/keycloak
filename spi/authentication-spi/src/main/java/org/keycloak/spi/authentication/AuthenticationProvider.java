package org.keycloak.spi.authentication;

import java.util.Map;

import org.keycloak.models.RealmModel;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface AuthenticationProvider {

    String getName();

    /**
     * Standard Authentication flow
     *
     * @param username
     * @param password
     * @return
     */
    AuthResult validatePassword(RealmModel realm, Map<String, String> configuration, String username, String password) throws AuthenticationProviderException;


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
