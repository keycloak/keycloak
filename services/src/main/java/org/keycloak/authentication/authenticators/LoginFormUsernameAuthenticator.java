package org.keycloak.authentication.authenticators;

import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorContext;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.AuthenticatorModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.managers.AuthenticationManager;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class LoginFormUsernameAuthenticator extends AbstractFormAuthenticator implements Authenticator {
    public static final String FORM_USERNAME = "FORM_USERNAME";
    protected AuthenticatorModel model;

    public LoginFormUsernameAuthenticator(AuthenticatorModel model) {
        this.model = model;
    }

    @Override
    public void authenticate(AuthenticatorContext context) {
        if (isAction(context, REGISTRATION_FORM_ACTION) && context.getUser() != null) {
            context.success();
            return;
        }
        if (!isAction(context, LOGIN_FORM_ACTION)) {
            MultivaluedMap<String, String> formData = new MultivaluedMapImpl<>();
            String loginHint = context.getClientSession().getNote(OIDCLoginProtocol.LOGIN_HINT_PARAM);

            String rememberMeUsername = AuthenticationManager.getRememberMeUsername(context.getRealm(), context.getHttpRequest().getHttpHeaders());

            if (loginHint != null || rememberMeUsername != null) {
                if (loginHint != null) {
                    formData.add(AuthenticationManager.FORM_USERNAME, loginHint);
                } else {
                    formData.add(AuthenticationManager.FORM_USERNAME, rememberMeUsername);
                    formData.add("rememberMe", "on");
                }
            }
            Response challengeResponse = challenge(context, formData);
            context.challenge(challengeResponse);
            return;
        }
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        if (formData.containsKey("cancel")) {
            context.getEvent().error(Errors.REJECTED_BY_USER);
            LoginProtocol protocol = context.getSession().getProvider(LoginProtocol.class, context.getClientSession().getAuthMethod());
            protocol.setRealm(context.getRealm())
                    .setHttpHeaders(context.getHttpRequest().getHttpHeaders())
                    .setUriInfo(context.getUriInfo());
            Response response = protocol.cancelLogin(context.getClientSession());
            context.challenge(response);
            return;
        }

        validateUser(context, formData);
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

    public void validateUser(AuthenticatorContext context, MultivaluedMap<String, String> inputData) {
        String username = inputData.getFirst(AuthenticationManager.FORM_USERNAME);
        if (username == null) {
            context.getEvent().error(Errors.USER_NOT_FOUND);
            Response challengeResponse = invalidUser(context);
            context.failureChallenge(AuthenticationProcessor.Error.INVALID_USER, challengeResponse);
            return;
        }
        context.getEvent().detail(Details.USERNAME, username);
        context.getClientSession().setNote(FORM_USERNAME, username);
        UserModel user = KeycloakModelUtils.findUserByNameOrEmail(context.getSession(), context.getRealm(), username);
        if (invalidUser(context, user)) return;
        String rememberMe = inputData.getFirst("rememberMe");
        boolean remember = rememberMe != null && rememberMe.equalsIgnoreCase("on");
        if (remember) {
            context.getClientSession().setNote(Details.REMEMBER_ME, "true");
            context.getEvent().detail(Details.REMEMBER_ME, "true");
        }
        context.setUser(user);
        context.success();
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
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
