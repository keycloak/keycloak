package org.keycloak.authentication.authenticators.resetcred;

import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.Urls;
import org.keycloak.services.messages.Messages;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResetCredentialChooseUser implements Authenticator, AuthenticatorFactory {

    public static final String PROVIDER_ID = "reset-credentials-choose-user";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        Response challenge = context.form().createPasswordReset();
        context.challenge(challenge);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        EventBuilder event = context.getEvent();
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String username = formData.getFirst("username");
        if (username == null || username.isEmpty()) {
            event.error(Errors.USERNAME_MISSING);
            Response challenge = context.form()
                    .setError(Messages.MISSING_USERNAME)
                    .createPasswordReset();
            context.failureChallenge(AuthenticationFlowError.INVALID_USER, challenge);
            return;
        }

        UserModel user = context.getSession().users().getUserByUsername(username, context.getRealm());
        if (user == null && username.contains("@")) {
            user =  context.getSession().users().getUserByEmail(username, context.getRealm());
        }

        if (user == null) {
            event.error(Errors.INVALID_USER_CREDENTIALS);
            Response challenge = context.form()
                    .setError(Messages.INVALID_USER)
                    .createPasswordReset();
            context.failureChallenge(AuthenticationFlowError.INVALID_USER, challenge);
            return;
        }

        if (!user.isEnabled()) {
            event.user(user).error(Errors.USER_DISABLED);
            Response challenge = context.form()
                    .setError(Messages.ACCOUNT_DISABLED)
                    .createPasswordReset();
            context.failureChallenge(AuthenticationFlowError.INVALID_USER, challenge);
            return;
        }

        if (user.getEmail() == null || user.getEmail().trim().length() == 0) {
            event.user(user).error(Errors.INVALID_EMAIL);
            Response challenge = context.form()
                    .setError(Messages.INVALID_EMAIL)
                    .createPasswordReset();
            context.failureChallenge(AuthenticationFlowError.INVALID_USER, challenge);
            return;
        }

        context.setUser(user);
        context.success();
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {

    }

    @Override
    public String getDisplayType() {
        return "Choose User";
    }

    @Override
    public String getReferenceCategory() {
        return null;
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    public static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED
    };

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Choose a user to reset credentials for";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
