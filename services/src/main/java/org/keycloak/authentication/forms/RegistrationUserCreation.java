package org.keycloak.authentication.forms;

import org.keycloak.Config;
import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormActionFactory;
import org.keycloak.authentication.FormContext;
import org.keycloak.authentication.ValidationContext;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.AttributeFormDataProcessor;
import org.keycloak.services.validation.Validation;

import javax.ws.rs.core.MultivaluedMap;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RegistrationUserCreation implements FormAction, FormActionFactory {

    public static final String PROVIDER_ID = "registration-user-creation";

    @Override
    public String getHelpText() {
        return "This action must always be first! Validates the username of the user in validation phase.  In success phase, this will create the user in the database.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return null;
    }

    @Override
    public void validate(ValidationContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        List<FormMessage> errors = new ArrayList<>();
        context.getEvent().detail(Details.REGISTER_METHOD, "form");

        String email = formData.getFirst(Validation.FIELD_EMAIL);
        String username = formData.getFirst(RegistrationPage.FIELD_USERNAME);
        context.getEvent().detail(Details.USERNAME, username);
        context.getEvent().detail(Details.EMAIL, email);

        String usernameField = RegistrationPage.FIELD_USERNAME;
        if (context.getRealm().isRegistrationEmailAsUsername()) {
            username = email;
            context.getEvent().detail(Details.USERNAME, username);
            usernameField = RegistrationPage.FIELD_EMAIL;
            if (Validation.isBlank(email)) {
                errors.add(new FormMessage(RegistrationPage.FIELD_EMAIL, Messages.MISSING_EMAIL));
            } else if (!Validation.isEmailValid(email)) {
                errors.add(new FormMessage(RegistrationPage.FIELD_EMAIL, Messages.INVALID_EMAIL));
                formData.remove(Validation.FIELD_EMAIL);
            }
            if (errors.size() > 0) {
                context.getEvent().error(Errors.INVALID_REGISTRATION);
                context.validationError(formData, errors);
                return;
            }
            if (email != null && context.getSession().users().getUserByEmail(email, context.getRealm()) != null) {
                context.getEvent().error(Errors.USERNAME_IN_USE);
                formData.remove(Validation.FIELD_EMAIL);
                errors.add(new FormMessage(RegistrationPage.FIELD_EMAIL, Messages.USERNAME_EXISTS));
                context.validationError(formData, errors);
                return;
            }
        } else {
            if (Validation.isBlank(username)) {
                context.getEvent().error(Errors.INVALID_REGISTRATION);
                errors.add(new FormMessage(RegistrationPage.FIELD_USERNAME, Messages.MISSING_USERNAME));
                context.validationError(formData, errors);
                return;
            }

        }
        if (context.getSession().users().getUserByUsername(username, context.getRealm()) != null) {
            context.getEvent().error(Errors.USERNAME_IN_USE);
            errors.add(new FormMessage(usernameField, Messages.USERNAME_EXISTS));
            formData.remove(Validation.FIELD_USERNAME);
            formData.remove(Validation.FIELD_EMAIL);
            context.validationError(formData, errors);
            return;

        }
        context.success();
    }

    @Override
    public void buildPage(FormContext context, LoginFormsProvider form) {

    }

    @Override
    public void success(FormContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String email = formData.getFirst(Validation.FIELD_EMAIL);
        String username = formData.getFirst(RegistrationPage.FIELD_USERNAME);
        if (context.getRealm().isRegistrationEmailAsUsername()) {
            username = formData.getFirst(RegistrationPage.FIELD_EMAIL);
        }
        context.getEvent().detail(Details.USERNAME, username)
                .detail(Details.REGISTER_METHOD, "form")
                .detail(Details.EMAIL, email)
        ;
        UserModel user = context.getSession().users().addUser(context.getRealm(), username);
        user.setEnabled(true);

        user.setEmail(email);
        context.getClientSession().setNote(OIDCLoginProtocol.LOGIN_HINT_PARAM, username);
        AttributeFormDataProcessor.process(formData, context.getRealm(), user);
        context.setUser(user);
        context.getEvent().user(user);
        context.getEvent().success();
        context.newEvent().event(EventType.LOGIN);
        context.getEvent().client(context.getClientSession().getClient().getClientId())
                .detail(Details.REDIRECT_URI, context.getClientSession().getRedirectUri())
                .detail(Details.AUTH_METHOD, context.getClientSession().getAuthMethod());
        String authType = context.getClientSession().getNote(Details.AUTH_TYPE);
        if (authType != null) {
            context.getEvent().detail(Details.AUTH_TYPE, authType);
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
    public boolean isUserSetupAllowed() {
        return false;
    }


    @Override
    public void close() {

    }

    @Override
    public String getDisplayType() {
        return "Registration User Creation";
    }

    @Override
    public String getReferenceCategory() {
        return null;
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    private static AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.DISABLED
    };
    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
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
