package org.keycloak.authentication.authenticators;

import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorContext;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.AuthenticatorModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.ClientSessionCode;
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
        return expected.getPath().equals(context.getUriInfo().getPath());

    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    protected Response challenge(AuthenticatorContext context, MultivaluedMap<String, String> formData) {
        LoginFormsProvider forms = context.getSession().getProvider(LoginFormsProvider.class)
                .setClientSessionCode(new ClientSessionCode(context.getRealm(), context.getClientSession()).getCode());

        if (formData.size() > 0) forms.setFormData(formData);

        return forms.createLogin();
    }

    public void validateUser(AuthenticatorContext context) {
        MultivaluedMap<String, String> inputData = context.getHttpRequest().getFormParameters();
        String username = inputData.getFirst(AuthenticationManager.FORM_USERNAME);
        if (username == null) {
            Response challengeResponse = challenge(context);
            context.failureChallenge(AuthenticationProcessor.Error.INVALID_USER, challengeResponse);
            return;
        }
        UserModel user = KeycloakModelUtils.findUserByNameOrEmail(context.getSession(), context.getRealm(), username);
        if (invalidUser(context, user)) return;
        context.setUser(user);
    }

    public boolean invalidUser(AuthenticatorContext context, UserModel user) {
        if (user == null) {
            Response challengeResponse = challenge(context);
            context.failureChallenge(AuthenticationProcessor.Error.INVALID_USER, challengeResponse);
            return true;
        }
        if (!user.isEnabled()) {
            context.failure(AuthenticationProcessor.Error.USER_DISABLED);
            return true;
        }
        if (context.getRealm().isBruteForceProtected()) {
            if (context.getProtector().isTemporarilyDisabled(context.getSession(), context.getRealm(), user.getUsername())) {
                context.failure(AuthenticationProcessor.Error.USER_TEMPORARILY_DISABLED);
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
