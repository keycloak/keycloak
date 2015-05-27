package org.keycloak.authentication.authenticators;

import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorContext;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.AuthenticatorModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.LoginActionsService;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.net.URI;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class LoginFormUsernameAuthenticator implements Authenticator {
    protected AuthenticatorModel model;

    public LoginFormUsernameAuthenticator(AuthenticatorModel model) {
        this.model = model;
    }

    @Override
    public void authenticate(AuthenticatorContext context) {
        if (!isActionUrl(context)) {
            MultivaluedMap<String, String> formData = new MultivaluedMapImpl<>();
            String loginHint = context.getClientSession().getNote(OIDCLoginProtocol.LOGIN_HINT_PARAM);
            if (loginHint == null) {
                loginHint = AuthenticationManager.getRememberMeUsername(context.getRealm(), context.getHttpRequest().getHttpHeaders());
                if (loginHint != null) {
                    formData.add("rememberMe", "on");
                }
            }
            if (loginHint != null) formData.add(AuthenticationManager.FORM_USERNAME, loginHint);
            Response challengeResponse = challenge(context, formData);
            context.challenge(challengeResponse);
            return;
        }
        validateUser(context);
    }

    protected boolean isActionUrl(AuthenticatorContext context) {
        URI expected = LoginActionsService.authenticationFormProcessor(context.getUriInfo()).build(context.getRealm().getName());
        String current = context.getUriInfo().getAbsolutePath().getPath();
        String expectedPath = expected.getPath();
        return expectedPath.equals(current);

    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    protected Response challenge(AuthenticatorContext context, MultivaluedMap<String, String> formData) {
        LoginFormsProvider forms = loginForm(context);

        if (formData.size() > 0) forms.setFormData(formData);

        return forms.createLogin();
    }

    protected LoginFormsProvider loginForm(AuthenticatorContext context) {
        ClientSessionCode code = new ClientSessionCode(context.getRealm(), context.getClientSession());
        code.setAction(ClientSessionModel.Action.AUTHENTICATE);
        URI action = LoginActionsService.authenticationFormProcessor(context.getUriInfo())
                .queryParam(OAuth2Constants.CODE, code.getCode())
                .build(context.getRealm().getName());
        return context.getSession().getProvider(LoginFormsProvider.class)
                    .setActionUri(action)
                    .setClientSessionCode(code.getCode());
    }

    protected Response invalidUser(AuthenticatorContext context) {
        return loginForm(context).setError(Messages.INVALID_USER).createLogin();
    }

    protected Response disabledUser(AuthenticatorContext context) {
        return loginForm(context).setError(Messages.ACCOUNT_DISABLED).createLogin();
    }

    protected Response temporarilyDisabledUser(AuthenticatorContext context) {
        return loginForm(context).setError(Messages.ACCOUNT_TEMPORARILY_DISABLED).createLogin();
    }

    public void validateUser(AuthenticatorContext context) {
        MultivaluedMap<String, String> inputData = context.getHttpRequest().getFormParameters();
        String username = inputData.getFirst(AuthenticationManager.FORM_USERNAME);
        if (username == null) {
            Response challengeResponse = invalidUser(context);
            context.failureChallenge(AuthenticationProcessor.Error.INVALID_USER, challengeResponse);
            return;
        }
        UserModel user = KeycloakModelUtils.findUserByNameOrEmail(context.getSession(), context.getRealm(), username);
        if (invalidUser(context, user)) return;
        context.setUser(user);
        context.success();
    }

    public boolean invalidUser(AuthenticatorContext context, UserModel user) {
        if (user == null) {
            Response challengeResponse = invalidUser(context);
            context.failureChallenge(AuthenticationProcessor.Error.INVALID_USER, challengeResponse);
            return true;
        }
        if (!user.isEnabled()) {
            Response challengeResponse = disabledUser(context);
            context.failureChallenge(AuthenticationProcessor.Error.USER_DISABLED, challengeResponse);
            return true;
        }
        if (context.getRealm().isBruteForceProtected()) {
            if (context.getProtector().isTemporarilyDisabled(context.getSession(), context.getRealm(), user.getUsername())) {
                Response challengeResponse = temporarilyDisabledUser(context);
                context.failureChallenge(AuthenticationProcessor.Error.USER_TEMPORARILY_DISABLED, challengeResponse);
                return true;
            }
        }
        return false;
    }

    public Response challenge(AuthenticatorContext context) {
        MultivaluedMap<String, String> formData = new MultivaluedMapImpl<>();
        return challenge(context, formData);
    }

    @Override
    public boolean configuredFor(UserModel user) {
        return true;
    }

    @Override
    public String getRequiredAction() {
        return null;
    }

    @Override
    public void close() {

    }
}
