package org.keycloak.authentication;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;
import sun.security.krb5.internal.AuthContext;

/**
* @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
* @version $Revision: 1 $
*/
public interface Authenticator2 extends Authenticator {
    void putAction(AuthContext context);
    void postAction(AuthContext context);
    void deleteAction(AuthContext context);
    void getAction(AuthContext context);
}
