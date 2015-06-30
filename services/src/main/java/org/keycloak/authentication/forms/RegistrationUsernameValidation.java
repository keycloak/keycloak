package org.keycloak.authentication.forms;

import org.keycloak.Config;
import org.keycloak.authentication.AuthenticatorContext;
import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormActionContext;
import org.keycloak.authentication.FormActionFactory;
import org.keycloak.authentication.FormAuthenticator;
import org.keycloak.events.Errors;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.validation.Validation;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RegistrationUsernameValidation implements FormAction, FormActionFactory {


    public static final String PROVIDER_ID = "username-validation-action";

    @Override
    public void authenticate(FormActionContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        List<FormMessage> errors = new ArrayList<>();

        String email = formData.getFirst(Validation.FIELD_EMAIL);
        String username = formData.getFirst(RegistrationPage.FIELD_USERNAME);

        String usernameField = RegistrationPage.FIELD_USERNAME;
        if (context.getRealm().isRegistrationEmailAsUsername()) {
            username = email;
            usernameField = RegistrationPage.FIELD_EMAIL;
            if (Validation.isBlank(email)) {
                errors.add(new FormMessage(RegistrationPage.FIELD_EMAIL, Messages.MISSING_EMAIL));
            } else if (!Validation.isEmailValid(email)) {
                errors.add(new FormMessage(RegistrationPage.FIELD_EMAIL, Messages.INVALID_EMAIL));
                formData.remove(Validation.FIELD_EMAIL);
            }
            if (errors.size() > 0) {
                context.getEvent().error(Errors.INVALID_REGISTRATION);
                Response challenge = context.getFormAuthenticator().createChallenge(context, formData, errors);
                context.challenge(challenge);
                return;
            }
            if (email != null && context.getSession().users().getUserByEmail(email, context.getRealm()) != null) {
                context.getEvent().error(Errors.EMAIL_IN_USE);
                formData.remove(Validation.FIELD_EMAIL);
                errors.add(new FormMessage(RegistrationPage.FIELD_EMAIL, Messages.EMAIL_EXISTS));
                Response challenge = context.getFormAuthenticator().createChallenge(context, formData, errors);
                context.challenge(challenge);
                return;
            }
        } else {
            if (Validation.isBlank(username)) {
                context.getEvent().error(Errors.INVALID_REGISTRATION);
                errors.add(new FormMessage(RegistrationPage.FIELD_USERNAME, Messages.MISSING_USERNAME));
                Response challenge = context.getFormAuthenticator().createChallenge(context, formData, errors);
                context.challenge(challenge);
                return;
            }

        }
        if (context.getSession().users().getUserByUsername(username, context.getRealm()) != null) {
            context.getEvent().error(Errors.USERNAME_IN_USE);
            errors.add(new FormMessage(usernameField, Messages.USERNAME_EXISTS));
            formData.remove(Validation.FIELD_USERNAME);
            formData.remove(Validation.FIELD_EMAIL);
            Response challenge = context.getFormAuthenticator().createChallenge(context, formData, errors);
            context.challenge(challenge);
            return;

        }
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
    public void close() {

    }

    @Override
    public String getDisplayType() {
        return "Username Validation";
    }

    @Override
    public String getReferenceCategory() {
        return null;
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[0];
    }

    @Override
    public FormAction create(KeycloakSession session) {
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
