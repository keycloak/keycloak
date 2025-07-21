/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.authentication.requiredactions;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.AuthenticatorUtil;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.actiontoken.updateemail.UpdateEmailActionToken;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsPages;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.forms.login.freemarker.Templates;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionConfigModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.services.Urls;
import org.keycloak.services.validation.Validation;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.userprofile.EventAuditingAttributeChangeListener;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.userprofile.ValidationException;

public class UpdateEmail implements RequiredActionProvider, RequiredActionFactory, EnvironmentDependentProviderFactory {

    private static final Logger logger = Logger.getLogger(UpdateEmail.class);

    public static final String CONFIG_VERIFY_EMAIL = "verifyEmail";
    private static final String FORCE_EMAIL_VERIFICATION = "forceEmailVerification";

    public static boolean isEnabled(RealmModel realm) {
        if (!Profile.isFeatureEnabled(Profile.Feature.UPDATE_EMAIL)) {
            return false;
        }

        RequiredActionProviderModel model = realm.getRequiredActionProviderByAlias(RequiredAction.UPDATE_EMAIL.name());

        return model != null && model.isEnabled();
    }

    public static boolean isVerifyEmailEnabled(RealmModel realm) {
        if (!isEnabled(realm)) {
            return false;
        }

        RequiredActionConfigModel config = realm.getRequiredActionConfigByAlias(RequiredAction.UPDATE_EMAIL.name());

        if (config == null) {
            return false;
        }

        return isVerifyEmailEnabled(realm, config);
    }

    public static void forceEmailVerification(KeycloakSession session) {
        session.setAttribute(FORCE_EMAIL_VERIFICATION, true);
    }

    private static boolean isVerifyEmailEnabled(RealmModel realm, RequiredActionConfigModel config) {
        boolean forceVerifyEmail = Boolean.parseBoolean(config.getConfigValue(CONFIG_VERIFY_EMAIL, Boolean.FALSE.toString()));
        return forceVerifyEmail || realm.isVerifyEmail();
    }

    @Override
    public InitiatedActionSupport initiatedActionSupport() {
        return InitiatedActionSupport.SUPPORTED;
    }

    @Override
    public String getDisplayText() {
        return "Update Email";
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {

    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        if (isEnabled(context.getRealm())) {
            KeycloakSession session = context.getSession();

            if (session.getAttributeOrDefault(FORCE_EMAIL_VERIFICATION, Boolean.FALSE)) {
                sendEmailUpdateConfirmation(context, false);
                return;
            }

            context.challenge(context.form().createResponse(UserModel.RequiredAction.UPDATE_EMAIL));
        }
    }

    @Override
    public void processAction(RequiredActionContext context) {
        if (!isEnabled(context.getRealm())) {
            return;
        }
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String newEmail = formData.getFirst(UserModel.EMAIL);

        RealmModel realm = context.getRealm();
        UserModel user = context.getUser();
        UserProfile emailUpdateValidationResult;
        try {
            emailUpdateValidationResult = validateEmailUpdate(context.getSession(), user, newEmail);
        } catch (ValidationException pve) {
            List<FormMessage> errors = Validation.getFormErrorsFromValidation(pve.getErrors());
            context.challenge(context.form().setErrors(errors).setFormData(formData)
                    .createResponse(UserModel.RequiredAction.UPDATE_EMAIL));
            return;
        }

        final boolean logoutSessions = "on".equals(formData.getFirst("logout-sessions"));
        if (!isVerifyEmailEnabled(realm, context.getConfig()) || Validation.isBlank(newEmail)
                || Objects.equals(user.getEmail(), newEmail) && user.isEmailVerified()) {
            if (logoutSessions) {
                AuthenticatorUtil.logoutOtherSessions(context);
            }
            updateEmailWithoutConfirmation(context, emailUpdateValidationResult);
            return;
        }

        sendEmailUpdateConfirmation(context, logoutSessions);
    }

    private void sendEmailUpdateConfirmation(RequiredActionContext context, boolean logoutSessions) {
        UserModel user = context.getUser();
        String oldEmail = user.getEmail();
        String newEmail = context.getHttpRequest().getDecodedFormParameters().getFirst(UserModel.EMAIL);

        RealmModel realm = context.getRealm();
        int validityInSecs = realm.getActionTokenGeneratedByUserLifespan(UpdateEmailActionToken.TOKEN_TYPE);

        UriInfo uriInfo = context.getUriInfo();
        KeycloakSession session = context.getSession();
        AuthenticationSessionModel authenticationSession = context.getAuthenticationSession();

        UpdateEmailActionToken actionToken = new UpdateEmailActionToken(user.getId(), Time.currentTime() + validityInSecs,
                oldEmail, newEmail, authenticationSession.getClient().getClientId(), logoutSessions, authenticationSession.getRedirectUri());

        String link = Urls
                .actionTokenBuilder(uriInfo.getBaseUri(), actionToken.serialize(session, realm, uriInfo),
                        authenticationSession.getClient().getClientId(), authenticationSession.getTabId(), AuthenticationProcessor.getClientData(session, authenticationSession))

                .build(realm.getName()).toString();

        context.getEvent().event(EventType.SEND_VERIFY_EMAIL).detail(Details.EMAIL, newEmail);
        try {
            session.getProvider(EmailTemplateProvider.class).setAuthenticationSession(authenticationSession).setRealm(realm)
                    .setUser(user).sendEmailUpdateConfirmation(link, TimeUnit.SECONDS.toMinutes(validityInSecs), newEmail);
        } catch (EmailException e) {
            logger.error("Failed to send email for email update", e);
            context.getEvent().error(Errors.EMAIL_SEND_FAILED);
            return;
        }
        context.getEvent().success();

        LoginFormsProvider forms = context.form();
        context.challenge(forms.setAttribute("messageHeader", forms.getMessage("emailUpdateConfirmationSentTitle"))
                .setInfo("emailUpdateConfirmationSent", newEmail).createForm(Templates.getTemplate(LoginFormsPages.INFO)));
    }

    private void updateEmailWithoutConfirmation(RequiredActionContext context,
                                                UserProfile emailUpdateValidationResult) {

        updateEmailNow(context.getEvent(), context.getUser(), emailUpdateValidationResult);
        context.success();
    }

    public static UserProfile validateEmailUpdate(KeycloakSession session, UserModel user, String newEmail) {
        MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
        formData.putSingle(UserModel.USERNAME, user.getUsername());
        formData.putSingle(UserModel.EMAIL, newEmail);
        UserProfileProvider profileProvider = session.getProvider(UserProfileProvider.class);
        UserProfile profile = profileProvider.create(UserProfileContext.UPDATE_EMAIL, formData, user);
        profile.validate();
        return profile;
    }

    public static void updateEmailNow(EventBuilder event, UserModel user, UserProfile emailUpdateValidationResult) {

        String oldEmail = user.getEmail();
        String newEmail = emailUpdateValidationResult.getAttributes().getFirst(UserModel.EMAIL);
        event.event(EventType.UPDATE_EMAIL).detail(Details.PREVIOUS_EMAIL, oldEmail).detail(Details.UPDATED_EMAIL, newEmail);
        emailUpdateValidationResult.update(false, new EventAuditingAttributeChangeListener(emailUpdateValidationResult, event));
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return UserModel.RequiredAction.UPDATE_EMAIL.name();
    }

    @Override
    public int getMaxAuthAge(KeycloakSession session) {
        // always require re-authentication
        return 0;
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name("verifyEmail")
                .label("Force Email Verification")
                .helpText("If enabled, the user will be forced to verify the email regardless if email verification is enabled at the realm level or not. Otherwise, verification will be based on the realm level setting.")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue(Boolean.FALSE)
                .add().build();
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.UPDATE_EMAIL);
    }
}
