package org.keycloak.authentication.forms;

import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticatorContext;
import org.keycloak.authentication.FormActionContext;
import org.keycloak.authentication.FormAuthenticator;
import org.keycloak.authentication.FormAuthenticatorFactory;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.resources.LoginActionsService;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RegistrationPage implements FormAuthenticator, FormAuthenticatorFactory {

    public static final String EXECUTION = "execution";
    public static final String FIELD_PASSWORD_CONFIRM = "password-confirm";
    public static final String FIELD_PASSWORD = "password";
    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_USERNAME = "username";
    public static final String FIELD_LAST_NAME = "lastName";
    public static final String FIELD_FIRST_NAME = "firstName";
    public static final String PROVIDER_ID = "registration-page-form";

    @Override
    public void authenticate(AuthenticatorContext context) {
        LoginFormsProvider registrationPage = createForm(context, context.getExecution().getId());
        context.challenge(registrationPage.createRegistration());

    }
    public URI getActionUrl(AuthenticatorContext context, String executionId, String code) {
        return LoginActionsService.registrationFormProcessor(context.getUriInfo())
                .queryParam(OAuth2Constants.CODE, code)
                .queryParam(EXECUTION, executionId)
                .build(context.getRealm().getName());
    }


    @Override
    public Response createChallenge(FormActionContext context, MultivaluedMap<String, String> formData, List<FormMessage> errorMessages) {
        LoginFormsProvider registrationPage = createForm(context, context.getFormExecution().getId());
        if (formData != null) registrationPage.setFormData(formData);
        if (errorMessages != null) {
            registrationPage.setErrors(errorMessages);
        }
        return registrationPage.createRegistration();
    }

    public LoginFormsProvider createForm(AuthenticatorContext context, String executionId) {
        AuthenticationExecutionModel.Requirement categoryRequirement = context.getCategoryRequirementFromCurrentFlow(UserCredentialModel.PASSWORD);
        boolean passwordRequired = categoryRequirement != null && categoryRequirement != AuthenticationExecutionModel.Requirement.DISABLED;
        String code = context.generateAccessCode();
        URI actionUrl = getActionUrl(context, executionId, code);
        return context.getSession().getProvider(LoginFormsProvider.class)
                .setAttribute("passwordRequired", passwordRequired)
                .setActionUri(actionUrl)
                .setClientSessionCode(code);
    }

    @Override
    public void close() {

    }

    @Override
    public String getDisplayType() {
        return "Registration Page";
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
    public FormAuthenticator create(KeycloakSession session) {
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
