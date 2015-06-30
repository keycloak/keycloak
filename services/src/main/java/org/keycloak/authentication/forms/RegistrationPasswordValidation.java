package org.keycloak.authentication.forms;

import org.keycloak.Config;
import org.keycloak.authentication.AuthenticatorContext;
import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormActionContext;
import org.keycloak.authentication.FormActionFactory;
import org.keycloak.authentication.FormAuthenticator;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
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
public class RegistrationPasswordValidation implements FormAction, FormActionFactory {
    public static final String PROVIDER_ID = "password-validation-action";

    @Override
    public void authenticate(FormActionContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        List<FormMessage> errors = new ArrayList<>();
        context.getEvent().detail(Details.REGISTER_METHOD, "form");
        if (Validation.isBlank(formData.getFirst(RegistrationPage.FIELD_PASSWORD))) {
            errors.add(new FormMessage(RegistrationPage.FIELD_PASSWORD, Messages.MISSING_PASSWORD));
        } else if (!formData.getFirst(RegistrationPage.FIELD_PASSWORD).equals(formData.getFirst(RegistrationPage.FIELD_PASSWORD_CONFIRM))) {
            errors.add(new FormMessage(RegistrationPage.FIELD_PASSWORD_CONFIRM, Messages.INVALID_PASSWORD_CONFIRM));
        }
        if (formData.getFirst(RegistrationPage.FIELD_PASSWORD) != null) {
            PasswordPolicy.Error err = context.getRealm().getPasswordPolicy().validate(context.getRealm().isRegistrationEmailAsUsername() ? formData.getFirst(RegistrationPage.FIELD_EMAIL) : formData.getFirst(RegistrationPage.FIELD_USERNAME), formData.getFirst(RegistrationPage.FIELD_PASSWORD));
            if (err != null)
                errors.add(new FormMessage(RegistrationPage.FIELD_PASSWORD, err.getMessage(), err.getParameters()));
        }

        if (errors.size() > 0) {
            context.getEvent().error(Errors.INVALID_REGISTRATION);
            formData.remove(RegistrationPage.FIELD_PASSWORD);
            formData.remove(RegistrationPage.FIELD_PASSWORD_CONFIRM);
            Response challenge = context.getFormAuthenticator().createChallenge(context, formData, errors);
            context.challenge(challenge);
            return;
        } else {
            context.success();
        }
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
        return "Password Validation";
    }

    @Override
    public String getReferenceCategory() {
        return UserCredentialModel.PASSWORD;
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
