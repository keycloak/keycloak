package org.keycloak.authentication.forms;

import org.keycloak.Config;
import org.keycloak.authentication.AuthenticatorContext;
import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormActionContext;
import org.keycloak.authentication.FormActionFactory;
import org.keycloak.authentication.FormAuthenticator;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.resources.AttributeFormDataProcessor;
import org.keycloak.services.validation.Validation;

import javax.ws.rs.core.MultivaluedMap;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RegistrationUserCreation implements FormAction, FormActionFactory {

    public static final String PROVIDER_ID = "registration-user-creation";

    @Override
    public void authenticate(FormActionContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String email = formData.getFirst(Validation.FIELD_EMAIL);
        String username = formData.getFirst(RegistrationPage.FIELD_USERNAME);
        if (context.getRealm().isRegistrationEmailAsUsername()) {
            username = formData.getFirst(RegistrationPage.FIELD_EMAIL);
        }
        UserModel user = context.getSession().users().addUser(context.getRealm(), username);
        user.setEnabled(true);
        user.setFirstName(formData.getFirst("firstName"));
        user.setLastName(formData.getFirst("lastName"));

        user.setEmail(email);
        context.getClientSession().setNote(OIDCLoginProtocol.LOGIN_HINT_PARAM, username);
        AttributeFormDataProcessor.process(formData, context.getRealm(), user);
        context.setUser(user);
        AuthenticationExecutionModel.Requirement categoryRequirement = context.getCategoryRequirementFromCurrentFlow(UserCredentialModel.PASSWORD);
        boolean passwordRequired = categoryRequirement != null && categoryRequirement != AuthenticationExecutionModel.Requirement.DISABLED;
        if (passwordRequired) {
            String password = formData.getFirst(RegistrationPage.FIELD_PASSWORD);
            UserCredentialModel credentials = new UserCredentialModel();
            credentials.setType(CredentialRepresentation.PASSWORD);
            credentials.setValue(password);

            try {
                context.getSession().users().updateCredential(context.getRealm(), user, UserCredentialModel.password(formData.getFirst("password")));
            } catch (Exception me) {
                user.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
            }
        }
        context.getEvent().user(user);
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
