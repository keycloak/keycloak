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
     * Initial call for the authenticator.  This method should check the current HTTP request to determine if the request
     * satifies the Authenticator's requirements.  If it doesn't, it should send back a challenge response by calling
     * the AuthenticationFlowContext.challenge(Response).  If this challenge is a authentication, the action URL
     * of the form must point to
     *
     * /realms/{realm}/login-actions/authenticate?code={session-code}&execution={executionId}
     *
     * or
     *
     * /realms/{realm}/login-actions/registration?code={session-code}&execution={executionId}
     *
     * {session-code} pertains to the code generated from AuthenticationFlowContext.generateAccessCode().  The {executionId}
     * pertains to the AuthenticationExecutionModel.getId() value obtained from AuthenticationFlowContext.getExecution().
     *
     * The action URL will invoke the action() method described below.
     *
     * @param context
     */
    void authenticate(AuthenticationFlowContext context);

    /**
     * Called from a form action invocation.
     *
     * @param context
     */
    void action(AuthenticationFlowContext context);


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



}
