package org.keycloak.authentication;

import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;

/**
 * Fine grain processing of a form.  Allows you to split up the processing of a form into smaller parts so that you can
 * enable/disable them from the admin console.  For example, Recaptcha is a FormAction.  This allows you as the admin
 * to turn Recaptcha on/off even though it is on the same form/page as other registration validation.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface FormAction extends Provider {
    /**
     * When a FormAuthenticator is rendering the challenge page, even FormAction.buildPage() method will be called
     * This gives the FormAction the opportunity to add additional attributes to the form to be displayed.
     *
     * @param context
     * @param form
     */
    void buildPage(FormContext context, LoginFormsProvider form);
    /**
     * This is the first phase of form processing.  Each FormAction.validate() method is called.  This gives the
     * FormAction a chance to validate and challenge if user input is invalid.
     *
     * @param context
     */
    void validate(ValidationContext context);

    /**
     * Called after all validate() calls of all FormAction providers are successful.
     *
     * @param context
     */
    void success(FormContext context);

    /**
     * Does this FormAction require that a user be set? For registration, this method will always return false.
     *
     * @return
     */
    boolean requiresUser();

    /**
     * Is this FormAction configured for the current user?
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


}
