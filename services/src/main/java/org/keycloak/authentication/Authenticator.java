package org.keycloak.authentication;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;

/**
* @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
* @version $Revision: 1 $
*/
public interface Authenticator extends Provider {
    void authenticate(AuthenticatorContext context);
    boolean requiresUser();
    boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user);

    /**
     * Set actions to configure authenticator
     *
     */
    void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user);

    void action(AuthenticatorContext context);


}
