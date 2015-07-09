package org.keycloak.authentication.authenticators;

import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorContext;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.LoginActionsService;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractFormAuthenticator implements Authenticator {

    private static final Logger logger = Logger.getLogger(AbstractFormAuthenticator.class);

    public static final String REGISTRATION_FORM_ACTION = "registration_form";
    public static final String EXECUTION = "execution";
    public static final String ATTEMPTED_USERNAME = "ATTEMPTED_USERNAME";

    @Override
    public void action(AuthenticatorContext context) {

    }

    @Override
    public void close() {

    }

    protected LoginFormsProvider loginForm(AuthenticatorContext context) {
        String accessCode = context.generateAccessCode();
        URI action = getActionUrl(context, accessCode);
        LoginFormsProvider provider = context.getSession().getProvider(LoginFormsProvider.class)
                    .setUser(context.getUser())
                    .setActionUri(action)
                    .setClientSessionCode(accessCode);
        if (context.getForwardedErrorMessage() != null) {
            provider.setError(context.getForwardedErrorMessage());
        }
        return provider;
    }

    public URI getActionUrl(AuthenticatorContext context, String code) {
        return LoginActionsService.authenticationFormProcessor(context.getUriInfo())
                .queryParam(OAuth2Constants.CODE, code)
                .queryParam(EXECUTION, context.getExecution().getId())
                    .build(context.getRealm().getName());
    }

    protected Response invalidUser(AuthenticatorContext context) {
        return loginForm(context)
                .setError(Messages.INVALID_USER)
                .createLogin();
    }

    protected Response disabledUser(AuthenticatorContext context) {
        return loginForm(context)
                .setError(Messages.ACCOUNT_DISABLED).createLogin();
    }

    protected Response temporarilyDisabledUser(AuthenticatorContext context) {
        return loginForm(context)
                .setError(Messages.ACCOUNT_TEMPORARILY_DISABLED).createLogin();
    }

    protected Response invalidCredentials(AuthenticatorContext context) {
        return loginForm(context)
                .setError(Messages.INVALID_USER).createLogin();
    }

    protected Response setDuplicateUserChallenge(AuthenticatorContext context, String eventError, String loginFormError, AuthenticationProcessor.Error authenticatorError) {
        context.getEvent().error(eventError);
        Response challengeResponse = loginForm(context)
                .setError(loginFormError).createLogin();
        context.failureChallenge(authenticatorError, challengeResponse);
        return challengeResponse;
    }

    public boolean invalidUser(AuthenticatorContext context, UserModel user) {
        if (user == null) {
            context.getEvent().error(Errors.USER_NOT_FOUND);
            Response challengeResponse = invalidUser(context);
            context.failureChallenge(AuthenticationProcessor.Error.INVALID_USER, challengeResponse);
            return true;
        }
        if (!user.isEnabled()) {
            context.getEvent().user(user);
            context.getEvent().error(Errors.USER_DISABLED);
            Response challengeResponse = disabledUser(context);
            context.failureChallenge(AuthenticationProcessor.Error.USER_DISABLED, challengeResponse);
            return true;
        }
        if (context.getRealm().isBruteForceProtected()) {
            if (context.getProtector().isTemporarilyDisabled(context.getSession(), context.getRealm(), user.getUsername())) {
                context.getEvent().user(user);
                context.getEvent().error(Errors.USER_TEMPORARILY_DISABLED);
                Response challengeResponse = temporarilyDisabledUser(context);
                context.failureChallenge(AuthenticationProcessor.Error.USER_TEMPORARILY_DISABLED, challengeResponse);
                return true;
            }
        }
        return false;
    }

    public boolean validateUser(AuthenticatorContext context, MultivaluedMap<String, String> inputData) {
        String username = inputData.getFirst(AuthenticationManager.FORM_USERNAME);
        if (username == null) {
            context.getEvent().error(Errors.USER_NOT_FOUND);
            Response challengeResponse = invalidUser(context);
            context.failureChallenge(AuthenticationProcessor.Error.INVALID_USER, challengeResponse);
            return false;
        }
        context.getEvent().detail(Details.USERNAME, username);
        context.getClientSession().setNote(AbstractFormAuthenticator.ATTEMPTED_USERNAME, username);

        UserModel user = null;
        try {
            user = KeycloakModelUtils.findUserByNameOrEmail(context.getSession(), context.getRealm(), username);
        } catch (ModelDuplicateException mde) {
            logger.error(mde.getMessage(), mde);

            // Could happen during federation import
            if (mde.getDuplicateFieldName() != null && mde.getDuplicateFieldName().equals(UserModel.EMAIL)) {
                setDuplicateUserChallenge(context, Errors.EMAIL_IN_USE, Messages.EMAIL_EXISTS, AuthenticationProcessor.Error.INVALID_USER);
            } else {
                setDuplicateUserChallenge(context, Errors.USERNAME_IN_USE, Messages.USERNAME_EXISTS, AuthenticationProcessor.Error.INVALID_USER);
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

    public boolean validatePassword(AuthenticatorContext context, MultivaluedMap<String, String> inputData) {
        List<UserCredentialModel> credentials = new LinkedList<>();
        String password = inputData.getFirst(CredentialRepresentation.PASSWORD);
        if (password == null) {
            if (context.getUser() != null) {
                context.getEvent().user(context.getUser());
            }
            context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
            Response challengeResponse = invalidCredentials(context);
            context.failureChallenge(AuthenticationProcessor.Error.INVALID_CREDENTIALS, challengeResponse);
            return false;
        }
        credentials.add(UserCredentialModel.password(password));
        boolean valid = context.getSession().users().validCredentials(context.getRealm(), context.getUser(), credentials);
        if (!valid) {
            context.getEvent().user(context.getUser());
            context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
            Response challengeResponse = invalidCredentials(context);
            context.failureChallenge(AuthenticationProcessor.Error.INVALID_CREDENTIALS, challengeResponse);
            return false;
        }
        return true;
    }
}
