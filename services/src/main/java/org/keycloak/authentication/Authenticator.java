package org.keycloak.authentication;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;

/**
 * This interface is for users that want to add custom authenticators to an authentication flow.
 * You must implement this interface as well as an AuthenticatorFactory.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface Authenticator extends Provider {

    /**
     * Initial call for the authenticator.  If this is a form, a challenge with a Response rendering the form is usually sent
     *
     * @param context
     */
    void authenticate(AuthenticatorContext context);

    /**
     * Does this authenticator require that the user has already been identified?  That AuthenticatorContext.getUser() is not null?
     *
     * @return
     */
    boolean requiresUser();

    /**
     * Is this authenticator configured for this user.
     *
     * @param session
     * @param realm
     * @param user
     * @return
     */
    boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user);

    /**
     * Set actions to configure authenticator
     *
     */
    void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user);

    /**
     * Usually implements a form action.
     *
     * @param context
     */
    void action(AuthenticatorContext context);


}
