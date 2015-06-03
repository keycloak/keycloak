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
    protected boolean isActionUrl(AuthenticatorContext context) {
        URI expected = LoginActionsService.authenticationFormProcessor(context.getUriInfo()).build(context.getRealm().getName());
        String current = context.getUriInfo().getAbsolutePath().getPath();
        String expectedPath = expected.getPath();
        return expectedPath.equals(current);

    }

    protected LoginFormsProvider loginForm(AuthenticatorContext context) {
        ClientSessionCode code = new ClientSessionCode(context.getRealm(), context.getClientSession());
        code.setAction(ClientSessionModel.Action.AUTHENTICATE);
        URI action = getActionUrl(context, code);
        return context.getSession().getProvider(LoginFormsProvider.class)
                    .setActionUri(action)
                    .setClientSessionCode(code.getCode());
    }

    public static URI getActionUrl(AuthenticatorContext context, ClientSessionCode code) {
        return LoginActionsService.authenticationFormProcessor(context.getUriInfo())
                    .queryParam(OAuth2Constants.CODE, code.getCode())
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
