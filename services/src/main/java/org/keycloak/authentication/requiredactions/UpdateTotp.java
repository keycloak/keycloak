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

import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.*;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.OTPCredentialProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.CredentialValidation;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.validation.Validation;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UpdateTotp implements RequiredActionProvider, RequiredActionFactory, DisplayTypeRequiredActionFactory, CredentialRegistrator {
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
        event.event(EventType.UPDATE_TOTP);
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String challengeResponse = formData.getFirst("totp");
        String totpSecret = formData.getFirst("totpSecret");
        String mode = formData.getFirst("mode");
        String userLabel = formData.getFirst("userLabel");

        OTPPolicy policy = context.getRealm().getOTPPolicy();
        OTPCredentialModel credentialModel = OTPCredentialModel.createFromPolicy(context.getRealm(), totpSecret, userLabel);
        if (Validation.isBlank(challengeResponse)) {
            Response challenge = context.form()
                    .setAttribute("mode", mode)
                    .setError(Messages.MISSING_TOTP)
                    .createResponse(UserModel.RequiredAction.CONFIGURE_TOTP);
            context.challenge(challenge);
            return;
        } else if (!validateOTPCredential(context, challengeResponse, credentialModel, policy)) {
            Response challenge = context.form()
                    .setAttribute("mode", mode)
                    .setError(Messages.INVALID_TOTP)
                    .createResponse(UserModel.RequiredAction.CONFIGURE_TOTP);
            context.challenge(challenge);
            return;
        }
        OTPCredentialProvider otpCredentialProvider = (OTPCredentialProvider) context.getSession().getProvider(CredentialProvider.class, "keycloak-otp");
        final List<CredentialModel> otpCredentials = (otpCredentialProvider.isConfiguredFor(context.getRealm(), context.getUser()))
            ? context.getSession().userCredentialManager().getStoredCredentialsByType(context.getRealm(), context.getUser(), OTPCredentialModel.TYPE)
            : Collections.EMPTY_LIST;
        if (otpCredentials.size() >= 1 && Validation.isBlank(userLabel)) {
            Response challenge = context.form()
                    .setAttribute("mode", mode)
                    .setError(Messages.MISSING_TOTP_DEVICE_NAME)
                    .createResponse(UserModel.RequiredAction.CONFIGURE_TOTP);
            context.challenge(challenge);
            return;
        }
        CredentialModel createdCredential = otpCredentialProvider.createCredential(context.getRealm(), context.getUser(), credentialModel);
        UserCredentialModel credential = new UserCredentialModel(createdCredential.getId(), otpCredentialProvider.getType(), challengeResponse);
        //If the type is HOTP, call verify once to consume the OTP used for registration and increase the counter.
        if (OTPCredentialModel.HOTP.equals(credentialModel.getOTPCredentialData().getSubType()) && !context.getSession().userCredentialManager().isValid(context.getRealm(), context.getUser(), credential)) {
            Response challenge = context.form()
                    .setAttribute("mode", mode)
                    .setError(Messages.INVALID_TOTP)
                    .createResponse(UserModel.RequiredAction.CONFIGURE_TOTP);
            context.challenge(challenge);
            return;
        }
        context.success();
    }


    // Use separate method, so it's possible to override in the custom provider
    protected boolean validateOTPCredential(RequiredActionContext context, String token, OTPCredentialModel credentialModel, OTPPolicy policy) {
        return CredentialValidation.validOTP(token, credentialModel, policy.getLookAheadWindow());
    }


    @Override
    public void close() {

    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return this;
    }


    @Override
    public RequiredActionProvider createDisplay(KeycloakSession session, String displayType) {
        if (displayType == null) return this;
        if (!OAuth2Constants.DISPLAY_CONSOLE.equalsIgnoreCase(displayType)) return null;
        return ConsoleUpdateTotp.SINGLETON;
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
    public boolean isOneTimeAction() {
        return true;
    }
}
