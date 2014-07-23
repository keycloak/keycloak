package org.keycloak.models;

import java.util.Set;

/**
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface FederationProvider extends UserProvider {
    UserModel proxy(UserModel local);
    UserModel addUser(RealmModel realm, UserModel user);
    boolean removeUser(RealmModel realm, UserModel user);
    Set<String> getSupportedCredentialTypes();
    String getAdminPage();
    Class getAdminClass();
}
