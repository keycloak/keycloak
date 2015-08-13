package org.keycloak.authentication.authenticators.browser;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AbstractFormAuthenticator;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractUsernameFormAuthenticator extends AbstractFormAuthenticator {

    private static final Logger logger = Logger.getLogger(AbstractUsernameFormAuthenticator.class);

    public static final String REGISTRATION_FORM_ACTION = "registration_form";
    public static final String ATTEMPTED_USERNAME = "ATTEMPTED_USERNAME";

    @Override
    public void action(AuthenticationFlowContext context) {

    }

    protected Response invalidUser(AuthenticationFlowContext context) {
        return context.form()
                .setError(Messages.INVALID_USER)
                .createLogin();
    }

    protected Response disabledUser(AuthenticationFlowContext context) {
        return context.form()
                .setError(Messages.ACCOUNT_DISABLED).createLogin();
    }

    protected Response temporarilyDisabledUser(AuthenticationFlowContext context) {
        return context.form()
                .setError(Messages.ACCOUNT_TEMPORARILY_DISABLED).createLogin();
    }

    protected Response invalidCredentials(AuthenticationFlowContext context) {
        return context.form()
                .setError(Messages.INVALID_USER).createLogin();
    }

    protected Response setDuplicateUserChallenge(AuthenticationFlowContext context, String eventError, String loginFormError, AuthenticationFlowError authenticatorError) {
        context.getEvent().error(eventError);
        Response challengeResponse = context.form()
                .setError(loginFormError).createLogin();
        context.failureChallenge(authenticatorError, challengeResponse);
        return challengeResponse;
    }

    public boolean invalidUser(AuthenticationFlowContext context, UserModel user) {
        if (user == null) {
            context.getEvent().error(Errors.USER_NOT_FOUND);
            Response challengeResponse = invalidUser(context);
            context.failureChallenge(AuthenticationFlowError.INVALID_USER, challengeResponse);
            return true;
        }
        if (!user.isEnabled()) {
            context.getEvent().user(user);
            context.getEvent().error(Errors.USER_DISABLED);
            Response challengeResponse = disabledUser(context);
            context.failureChallenge(AuthenticationFlowError.USER_DISABLED, challengeResponse);
            return true;
        }
        if (context.getRealm().isBruteForceProtected()) {
            if (context.getProtector().isTemporarilyDisabled(context.getSession(), context.getRealm(), user.getUsername())) {
                context.getEvent().user(user);
                context.getEvent().error(Errors.USER_TEMPORARILY_DISABLED);
                Response challengeResponse = temporarilyDisabledUser(context);
                context.failureChallenge(AuthenticationFlowError.USER_TEMPORARILY_DISABLED, challengeResponse);
                return true;
            }
        }
        return false;
    }

    public boolean validateUser(AuthenticationFlowContext context, MultivaluedMap<String, String> inputData) {
        String username = inputData.getFirst(AuthenticationManager.FORM_USERNAME);
        if (username == null) {
            context.getEvent().error(Errors.USER_NOT_FOUND);
            Response challengeResponse = invalidUser(context);
            context.failureChallenge(AuthenticationFlowError.INVALID_USER, challengeResponse);
            return false;
        }
        context.getEvent().detail(Details.USERNAME, username);
        context.getClientSession().setNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME, username);

        UserModel user = null;
        try {
            user = KeycloakModelUtils.findUserByNameOrEmail(context.getSession(), context.getRealm(), username);
        } catch (ModelDuplicateException mde) {
            logger.error(mde.getMessage(), mde);

            // Could happen during federation import
            if (mde.getDuplicateFieldName() != null && mde.getDuplicateFieldName().equals(UserModel.EMAIL)) {
                setDuplicateUserChallenge(context, Errors.EMAIL_IN_USE, Messages.EMAIL_EXISTS, AuthenticationFlowError.INVALID_USER);
            } else {
                setDuplicateUserChallenge(context, Errors.USERNAME_IN_USE, Messages.USERNAME_EXISTS, AuthenticationFlowError.INVALID_USER);
            }

            return false;
        }

        if (invalidUser(context, user)) return false;
        String rememberMe = inputData.getFirst("rememberMe");
        boolean remember = rememberMe != null && rememberMe.equalsIgnoreCase("on");
        if (remember) {
            context.getClientSession().setNote(Details.REMEMBER_ME, "true");
            context.getEvent().detail(Details.REMEMBER_ME, "true");
        } else {
            context.getClientSession().removeNote(Details.REMEMBER_ME);
        }
        context.setUser(user);
        return true;
    }

    public boolean validatePassword(AuthenticationFlowContext context, MultivaluedMap<String, String> inputData) {
        List<UserCredentialModel> credentials = new LinkedList<>();
        String password = inputData.getFirst(CredentialRepresentation.PASSWORD);
        if (password == null || password.isEmpty()) {
            if (context.getUser() != null) {
                context.getEvent().user(context.getUser());
            }
            context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
            Response challengeResponse = invalidCredentials(context);
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
            return false;
        }
        credentials.add(UserCredentialModel.password(password));
        boolean valid = context.getSession().users().validCredentials(context.getRealm(), context.getUser(), credentials);
        if (!valid) {
            context.getEvent().user(context.getUser());
            context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
            Response challengeResponse = invalidCredentials(context);
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
            return false;
        }
        return true;
    }
}
