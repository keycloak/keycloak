package org.keycloak.models.utils;

import org.keycloak.models.UserCredentialModel;

/**
 * User: jpkroehling
 * Date: 12/5/13
 * Time: 2:24 PM
 */
public interface PasswordEncoder {
    public String encode(UserCredentialModel credentialModel);
    public boolean verify(UserCredentialModel real, UserCredentialModel provided);
}
