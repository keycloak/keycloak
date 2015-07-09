package org.keycloak.authentication.forms;

import org.keycloak.Config;
import org.keycloak.authentication.FormAuthenticator;
import org.keycloak.authentication.FormAuthenticatorFactory;
import org.keycloak.authentication.FormContext;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import javax.ws.rs.core.Response;
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
    public Response render(FormContext context, LoginFormsProvider form) {
        return form.createRegistration();
    }

    @Override
    public void close() {

    }

    @Override
    public String getDisplayType() {
        return "Registration Page";
    }

    @Override
    public String getHelpText() {
        return null;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return null;
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
