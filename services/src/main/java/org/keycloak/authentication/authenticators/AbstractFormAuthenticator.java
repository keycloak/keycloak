package org.keycloak.authentication.authenticators;

import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.AuthenticatorContext;
import org.keycloak.events.Errors;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.LoginActionsService;

import javax.ws.rs.core.Response;
import java.net.URI;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AbstractFormAuthenticator {

    public static final String LOGIN_FORM_ACTION = "login_form";
    public static final String REGISTRATION_FORM_ACTION = "registration_form";
    public static final String ACTION = "action";

    protected boolean isAction(AuthenticatorContext context, String action) {
        return action.equals(context.getAction());
    }

    protected LoginFormsProvider loginForm(AuthenticatorContext context) {
        ClientSessionCode code = new ClientSessionCode(context.getRealm(), context.getClientSession());
        code.setAction(ClientSessionModel.Action.AUTHENTICATE.name());
        URI action = getActionUrl(context, code, LOGIN_FORM_ACTION);
        LoginFormsProvider provider = context.getSession().getProvider(LoginFormsProvider.class)
                    .setUser(context.getUser())
                    .setActionUri(action)
                    .setClientSessionCode(code.getCode());
        if (context.getForwardedErrorMessage() != null) {
            provider.setError(context.getForwardedErrorMessage());
        }
        return provider;
    }

    public static URI getActionUrl(AuthenticatorContext context, ClientSessionCode code, String action) {
        return LoginActionsService.authenticationFormProcessor(context.getUriInfo())
                .queryParam(OAuth2Constants.CODE, code.getCode())
                .queryParam(ACTION, action)
                    .build(context.getRealm().getName());
    }

    protected Response invalidUser(AuthenticatorContext context) {
        return loginForm(context)
                .setError(Messages.INVALID_USER)
                .setClientSessionCode(new ClientSessionCode(context.getRealm(), context.getClientSession()).getCode())
                .createLogin();
    }

    protected Response disabledUser(AuthenticatorContext context) {
        return loginForm(context)
                .setClientSessionCode(new ClientSessionCode(context.getRealm(), context.getClientSession()).getCode())
                .setError(Messages.ACCOUNT_DISABLED).createLogin();
    }

    protected Response temporarilyDisabledUser(AuthenticatorContext context) {
        return loginForm(context)
                .setClientSessionCode(new ClientSessionCode(context.getRealm(), context.getClientSession()).getCode())
                .setError(Messages.ACCOUNT_TEMPORARILY_DISABLED).createLogin();
    }

    protected Response invalidCredentials(AuthenticatorContext context) {
        return loginForm(context)
                .setClientSessionCode(new ClientSessionCode(context.getRealm(), context.getClientSession()).getCode())
                .setError(Messages.INVALID_USER).createLogin();
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
}
