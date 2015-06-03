package org.keycloak.authentication;

import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;

/**
* @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
* @version $Revision: 1 $
*/
public interface Authenticator extends Provider {
    boolean requiresUser();
    void authenticate(AuthenticatorContext context);
    boolean configuredFor(UserModel user);
    String getRequiredAction();


}
