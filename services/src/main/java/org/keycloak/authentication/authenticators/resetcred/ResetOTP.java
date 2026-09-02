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

package org.keycloak.authentication.authenticators.resetcred;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.CredentialValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.OTPCredentialProvider;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.services.messages.Messages;

import static java.util.Arrays.asList;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResetOTP extends AbstractSetRequiredActionAuthenticator implements CredentialValidator<OTPCredentialProvider> {

    public static final String PROVIDER_ID = "reset-otp";

    private static final String ACTION_ON_OTP_RESET_FLAG = "action_on_otp_reset_flag";
    private static final String REMOVE_NONE = "Remove none";
    private static final String REMOVE_ONE = "Remove one";
    private static final String REMOVE_ALL = "Remove all";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        AuthenticatorConfigModel authenticatorConfigModel = context.getAuthenticatorConfig();
        Map<String, String> authenticatorConfig = null;

        if (authenticatorConfigModel != null) {
            authenticatorConfig = authenticatorConfigModel.getConfig();
        }

        if (authenticatorConfig != null) {
            String selectedOption = authenticatorConfig.get(ACTION_ON_OTP_RESET_FLAG);

            List<CredentialModel> otpCredentialModelList = context.getUser().credentialManager()
                    .getStoredCredentialsByTypeStream(OTPCredentialModel.TYPE).collect(Collectors.toList());

            if (REMOVE_ALL.equals(selectedOption)) {
                otpCredentialModelList.forEach(otpCredentialModel -> context.getUser().credentialManager()
                        .removeStoredCredentialById(otpCredentialModel.getId()));
            }
            else if (REMOVE_ONE.equals(selectedOption) && !otpCredentialModelList.isEmpty()) {
                Response challengeResponse = context.form()
                        .setAttribute("configuredOtpCredentials", otpCredentialModelList)
                        .createOtpReset();

                context.challenge(challengeResponse);
                return;
            }
        }

        // To ensure backwards compatability, the required action has to be set even if no configuration is available.
        context.getAuthenticationSession().addRequiredAction(UserModel.RequiredAction.CONFIGURE_TOTP);
        context.success();
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> inputData = context.getHttpRequest().getDecodedFormParameters();

        String credentialId = inputData.getFirst("selectedCredentialId");

        // This case should never occur. If you there are no OTP credentials available the form will never be displayed in the first place.
        // If the form is displayed the first OTP credential is selected by default, and it's not possible to unselect radio buttons.
        if (credentialId == null || credentialId.isEmpty()) {
            List<CredentialModel> otpCredentialModelList = context.getUser().credentialManager()
                    .getStoredCredentialsByTypeStream(OTPCredentialModel.TYPE).collect(Collectors.toList());

            Response challengeResponse = context.form()
                    .setAttribute("configuredOtpCredentials", otpCredentialModelList)
                    .setError(Messages.RESET_OTP_MISSING_ID_ERROR)
                    .createOtpReset();

            context.challenge(challengeResponse);

            return;
        }

        context.getUser().credentialManager().removeStoredCredentialById(credentialId);

        context.getAuthenticationSession().addRequiredAction(UserModel.RequiredAction.CONFIGURE_TOTP);
        context.success();
    }

    @Override public boolean isConfigurable() {
        return true;
    }

    @Override public List<ProviderConfigProperty> getConfigProperties() {
        ProviderConfigurationBuilder builder = ProviderConfigurationBuilder.create();

        builder.property(ACTION_ON_OTP_RESET_FLAG,
                "Action on OTP reset.",
                " If 'Remove none' is chosen, the user will keep all existing OTP configurations (legacy behavior)." +
                        " If 'Remove one' is chosen, the user will be prompted to choose one OTP configuration which will then be removed." +
                        " If 'Remove all' is chosen, all existing OTP configurations of the user will be removed." +
                        " The user will always be prompted to configure a new OTP no matter which option is selected.",
                ProviderConfigProperty.LIST_TYPE,
                REMOVE_NONE,
                asList(REMOVE_NONE, REMOVE_ONE, REMOVE_ALL));

        return builder.build();
    }

    @Override
    public OTPCredentialProvider getCredentialProvider(KeycloakSession session) {
        return (OTPCredentialProvider)session.getProvider(CredentialProvider.class, "keycloak-otp");
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return getCredentialProvider(session).isConfiguredFor(realm, user);
    }

    @Override
    public String getDisplayType() {
        return "Reset OTP";
    }

    @Override
    public String getHelpText() {
        return "Removes existing OTP configurations (if chosen) and sets the 'Configure OTP' required action.";
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

}
