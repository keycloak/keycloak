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

package org.keycloak.authentication.requiredactions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.AuthenticatorUtil;
import org.keycloak.authentication.CredentialRegistrator;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.OTPCredentialProvider;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.RecoveryAuthnCodesCredentialModel;
import org.keycloak.models.utils.CredentialValidation;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.validation.Validation;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.utils.CredentialHelper;

import org.jboss.logging.Logger;

import static org.keycloak.models.AuthenticationExecutionModel.Requirement.DISABLED;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UpdateTotp implements RequiredActionProvider, RequiredActionFactory, CredentialRegistrator {

    private static final Logger log = Logger.getLogger(KeycloakModelUtils.class);
    public static final String ADD_RECOVERY_CODES = "add-recovery-codes";

    List<ProviderConfigProperty> ADD_RECOVERY_CODES_CONFIG_PROPERTIES = addRecoveryCodesConfig();

    static List<ProviderConfigProperty> addRecoveryCodesConfig() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(ADD_RECOVERY_CODES)
                .label("Add Recovery Codes")
                .helpText("""
                        If this option is enabled, the user will be required to configure recovery codes following the OTP configuration.
                        If the user already has recovery codes configured, Keycloak will not ask for setting them up.
                        As a prerequisite, enable the recovery codes required action and enable recovery codes in your authentication flow.""")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue(false)
                .add()
                .build();
    }

    @Override
    public InitiatedActionSupport initiatedActionSupport() {
        return InitiatedActionSupport.SUPPORTED;
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        Response challenge = context.form()
                .setAttribute("mode", context.getUriInfo().getQueryParameters().getFirst("mode"))
                .createResponse(UserModel.RequiredAction.CONFIGURE_TOTP);
        context.challenge(challenge);
    }

    @Override
    public void processAction(RequiredActionContext context) {
        EventBuilder event = context.getEvent();
        event.event(EventType.UPDATE_CREDENTIAL);
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String challengeResponse = formData.getFirst("totp");
        String totpSecret = formData.getFirst("totpSecret");
        String mode = formData.getFirst("mode");
        String userLabel = formData.getFirst("userLabel");

        OTPPolicy policy = context.getRealm().getOTPPolicy();
        OTPCredentialModel credentialModel = OTPCredentialModel.createFromPolicy(context.getRealm(), totpSecret, userLabel);
        event.detail(Details.CREDENTIAL_TYPE, credentialModel.getType());

        EventBuilder deprecatedEvent = event.clone().event(EventType.UPDATE_TOTP);
        if (Validation.isBlank(challengeResponse)) {
            Response challenge = context.form()
                    .setAttribute("mode", mode)
                    .addError(new FormMessage(Validation.FIELD_OTP_CODE, Messages.MISSING_TOTP))
                    .createResponse(UserModel.RequiredAction.CONFIGURE_TOTP);
            context.challenge(challenge);
            return;
        } else if (!validateOTPCredential(context, challengeResponse, credentialModel, policy)) {
            Response challenge = context.form()
                    .setAttribute("mode", mode)
                    .addError(new FormMessage(Validation.FIELD_OTP_CODE, Messages.INVALID_TOTP))
                    .createResponse(UserModel.RequiredAction.CONFIGURE_TOTP);
            context.challenge(challenge);
            return;
        }
        OTPCredentialProvider otpCredentialProvider = (OTPCredentialProvider) context.getSession().getProvider(CredentialProvider.class, "keycloak-otp");
        final Stream<CredentialModel> otpCredentials  = (otpCredentialProvider.isConfiguredFor(context.getRealm(), context.getUser()))
            ? context.getUser().credentialManager().getStoredCredentialsByTypeStream(OTPCredentialModel.TYPE)
            : Stream.empty();
        if (otpCredentials.count() >= 1 && Validation.isBlank(userLabel)) {
            Response challenge = context.form()
                    .setAttribute("mode", mode)
                    .addError(new FormMessage(Validation.FIELD_OTP_LABEL, Messages.MISSING_TOTP_DEVICE_NAME))
                    .createResponse(UserModel.RequiredAction.CONFIGURE_TOTP);
            context.challenge(challenge);
            return;
        }

        if ("on".equals(formData.getFirst("logout-sessions"))) {
            AuthenticatorUtil.logoutOtherSessions(context);
        }

        try {
            if (!CredentialHelper.createOTPCredential(context.getSession(), context.getRealm(), context.getUser(), challengeResponse, credentialModel)) {
                Response challenge = context.form()
                        .setAttribute("mode", mode)
                        .addError(new FormMessage(Validation.FIELD_OTP_CODE, Messages.INVALID_TOTP))
                        .createResponse(UserModel.RequiredAction.CONFIGURE_TOTP);
                context.challenge(challenge);
                return;
            }
        } catch (ModelDuplicateException e) {
            String field = switch (e.getDuplicateFieldName()) {
                case CredentialModel.USER_LABEL ->  Validation.FIELD_OTP_LABEL;
                default -> null;
            };
            Response challenge = context.form()
                    .setAttribute("mode", mode)
                    .addError(new FormMessage(field, e.getMessage()))
                    .createResponse(UserModel.RequiredAction.CONFIGURE_TOTP);
            context.challenge(challenge);
            return;
        }

        if (context.getConfig() != null &&
                Boolean.parseBoolean(context.getConfig().getConfigValue(ADD_RECOVERY_CODES, "false"))) {
            if (!isRecoveryCodesEnabledInAuthenticationFlow(context.getRealm(), context.getSession())) {
                log.info("OTP configured to set up recovery codes, but recovery codes are not enabled in the authentication flows. Skipping the setup of recovery codes.");
            } else if (!context.getRealm().getRequiredActionProviderByAlias(UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES.name()).isEnabled()) {
                log.info("OTP configured to set up recovery codes, but recovery codes required action is not enabled. Skipping the setup of recovery codes.");
            } else if (context.getUser().getRequiredActionsStream().noneMatch(s -> s.equals(UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES.name()))) {
                if (!context.getUser().credentialManager().isConfiguredFor(RecoveryAuthnCodesCredentialModel.TYPE)) {
                    context.getUser().addRequiredAction(UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES);
                }
            }
        }

        context.getAuthenticationSession().removeAuthNote(Constants.TOTP_SECRET_KEY);
        context.success();
        deprecatedEvent.success();
    }

    /**
     * Check if recovery codes are enabled in the authentication flow.
     * This is the same logic that is applied in the account console to show if recovery codes can be set up.
     */
    private boolean isRecoveryCodesEnabledInAuthenticationFlow(RealmModel realm, KeycloakSession session) {
        return realm.getAuthenticationFlowsStream()
                .filter(s -> !isFlowEffectivelyDisabled(realm, s))
                .flatMap(flow ->
                        realm.getAuthenticationExecutionsStream(flow.getId())
                                .filter(exe -> Objects.nonNull(exe.getAuthenticator()) && exe.getRequirement() != DISABLED)
                                .map(exe -> (AuthenticatorFactory) session.getKeycloakSessionFactory()
                                        .getProviderFactory(Authenticator.class, exe.getAuthenticator()))
                                .filter(Objects::nonNull)
                                .flatMap(authFact -> Stream.concat(Stream.of(authFact.getReferenceCategory()), authFact.getOptionalReferenceCategories(session).stream()))
                                .filter(Objects::nonNull)
                ).anyMatch(s -> s.equals(RecoveryAuthnCodesCredentialModel.TYPE));
    }

    // Returns true if flow is effectively disabled - either it's execution or some parent execution is disabled
    private boolean isFlowEffectivelyDisabled(RealmModel realm, AuthenticationFlowModel flow) {
        while (!flow.isTopLevel()) {
            AuthenticationExecutionModel flowExecution = realm.getAuthenticationExecutionByFlowId(flow.getId());
            if (flowExecution == null) return false; // Can happen under some corner cases
            if (DISABLED == flowExecution.getRequirement()) return true;
            if (flowExecution.getParentFlow() == null) return false;

            // Check parent flow
            flow = realm.getAuthenticationFlowById(flowExecution.getParentFlow());
            if (flow == null) return false;
        }

        return false;
    }

    // Use separate method, so it's possible to override in the custom provider
    protected boolean validateOTPCredential(RequiredActionContext context, String token, OTPCredentialModel credentialModel, OTPPolicy policy) {
        return CredentialValidation.validOTP(token, credentialModel, policy.getLookAheadWindow());
    }


    @Override
    public void close() {

    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        List<ProviderConfigProperty> configs = new ArrayList<>(List.copyOf(MAX_AUTH_AGE_CONFIG_PROPERTIES));
        configs.addAll(List.copyOf(ADD_RECOVERY_CODES_CONFIG_PROPERTIES));
        return configs;
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
    public String getDisplayText() {
        return "Configure OTP";
    }


    @Override
    public String getId() {
        return UserModel.RequiredAction.CONFIGURE_TOTP.name();
    }

    @Override
    public String getCredentialType(KeycloakSession session, AuthenticationSessionModel authenticationSession) {
        return OTPCredentialModel.TYPE;
    }

    @Override
    public boolean isOneTimeAction() {
        return true;
    }
}
