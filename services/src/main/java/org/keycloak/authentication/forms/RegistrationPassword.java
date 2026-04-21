/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.authentication.forms;

import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.core.MultivaluedMap;

import org.keycloak.Config;
import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormActionFactory;
import org.keycloak.authentication.FormContext;
import org.keycloak.authentication.ValidationContext;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.policy.PasswordPolicyManagerProvider;
import org.keycloak.policy.PolicyError;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.validation.Validation;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RegistrationPassword implements FormAction, FormActionFactory {
    public static final String PROVIDER_ID = "registration-password-action";

    // Configuration option
    public static final String ALWAYS_SET_PASSWORD_ON_REGISTER_FORM = "always_set_password_on_register_form";

    // Authentication note to signal that password fields should not be rendered on the registration page, but rather "update password" required action should be added
    // to the user account
    private static final String UPDATE_PASSWORD_AFTER_EMAIL_VERIFICATION_NOTE = "update_password_after_email_verification_note";

    @Override
    public String getHelpText() {
        return "Validates that password matches password confirmation field.  It also will store password in user's credential store.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(ALWAYS_SET_PASSWORD_ON_REGISTER_FORM)
                .label("Always set password on register form")
                .helpText("When this option is false and 'Verify Email' is enabled for the realm, then the password will not be set by the user on the registration form, but rather in the later stage once "
                        + " user's email address is successfully verified. This is recommended for security reasons. When true, the password fields will be available directly on the registration form and can be set "
                        + " by the user before his email is verified. This option is deprecated and might be removed in the future.")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .add()
                .build();
    }

    @Override
    public void validate(ValidationContext context) {
        if (isVerifyEmail(context)) {
            context.success();
            return;
        }

        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        List<FormMessage> errors = new ArrayList<>();
        context.getEvent().detail(Details.REGISTER_METHOD, "form");
        if (Validation.isBlank(formData.getFirst(RegistrationPage.FIELD_PASSWORD))) {
            errors.add(new FormMessage(RegistrationPage.FIELD_PASSWORD, Messages.MISSING_PASSWORD));
        } else if (!formData.getFirst(RegistrationPage.FIELD_PASSWORD).equals(formData.getFirst(RegistrationPage.FIELD_PASSWORD_CONFIRM))) {
            errors.add(new FormMessage(RegistrationPage.FIELD_PASSWORD_CONFIRM, Messages.INVALID_PASSWORD_CONFIRM));
        }
        if (formData.getFirst(RegistrationPage.FIELD_PASSWORD) != null) {
            PolicyError err = context.getSession().getProvider(PasswordPolicyManagerProvider.class).validate(context.getRealm().isRegistrationEmailAsUsername() ? formData.getFirst(RegistrationPage.FIELD_EMAIL) : formData.getFirst(RegistrationPage.FIELD_USERNAME), formData.getFirst(RegistrationPage.FIELD_PASSWORD));
            if (err != null)
                errors.add(new FormMessage(RegistrationPage.FIELD_PASSWORD, err.getMessage(), err.getParameters()));
        }

        if (errors.size() > 0) {
            context.error(Errors.INVALID_REGISTRATION);
            formData.remove(RegistrationPage.FIELD_PASSWORD);
            formData.remove(RegistrationPage.FIELD_PASSWORD_CONFIRM);
            context.validationError(formData, errors);
            return;
        } else {
            context.success();
        }
    }

    @Override
    public void success(FormContext context) {
        UserModel user = context.getUser();

        if ("true".equals(context.getAuthenticationSession().getAuthNote(UPDATE_PASSWORD_AFTER_EMAIL_VERIFICATION_NOTE))) {
            user.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
            return;
        }

        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        try {
            user.credentialManager().updateCredential(UserCredentialModel.password(formData.getFirst(RegistrationPage.FIELD_PASSWORD), false));
        } catch (Exception me) {
            user.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
        }

    }

    @Override
    public void buildPage(FormContext context, LoginFormsProvider form) {
        if (isVerifyEmail(context)) {
            context.getAuthenticationSession().setAuthNote(UPDATE_PASSWORD_AFTER_EMAIL_VERIFICATION_NOTE, "true");
        } else {
            form.setAttribute("passwordRequired", true);
        }
    }

    private boolean isVerifyEmail(FormContext context) {
        String alwaysSetPasswordCfg = context.getAuthenticatorConfig() == null ? null : context.getAuthenticatorConfig().getConfig().get(ALWAYS_SET_PASSWORD_ON_REGISTER_FORM);
        if ("true".equals(alwaysSetPasswordCfg)) return false;

        if (context.getRealm().isVerifyEmail()) return true;

        // Check if verifyEmail is set as default required action. In that case, newly registered users are also required to verify their emails
        RequiredActionProviderModel verifyEmailAction = context.getRealm().getRequiredActionProviderByAlias(UserModel.RequiredAction.VERIFY_EMAIL.name());
        return verifyEmailAction != null && verifyEmailAction.isDefaultAction();
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
        return "Password Validation";
    }

    @Override
    public String getReferenceCategory() {
        return PasswordCredentialModel.TYPE;
    }

    @Override
    public boolean isConfigurable() {
        return true;
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
