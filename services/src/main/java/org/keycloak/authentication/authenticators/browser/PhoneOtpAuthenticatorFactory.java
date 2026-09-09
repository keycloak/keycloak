/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.authentication.authenticators.browser;

import java.util.Arrays;
import java.util.List;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public class PhoneOtpAuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "auth-phone-otp-form";
    static final PhoneOtpAuthenticator SINGLETON = new PhoneOtpAuthenticator();

    public static final String CONF_PHONE_ATTRIBUTE = "phone_attribute";
    public static final String CONF_CODE_LENGTH = "code_length";
    public static final String CONF_CODE_TTL = "code_ttl";
    public static final String CONF_MAX_ATTEMPTS = "max_attempts";
    public static final String CONF_RESEND_COOLDOWN = "resend_cooldown";

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    @Override
    public void init(Config.Scope config) {
        // no-op
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Phone OTP";
    }

    @Override
    public String getHelpText() {
        return "Authenticate users with a one-time code sent to a phone number attribute.";
    }

    @Override
    public String getReferenceCategory() {
        return null;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        ProviderConfigProperty phoneAttribute = new ProviderConfigProperty();
        phoneAttribute.setType(ProviderConfigProperty.STRING_TYPE);
        phoneAttribute.setName(CONF_PHONE_ATTRIBUTE);
        phoneAttribute.setLabel("Phone attribute");
        phoneAttribute.setHelpText("User attribute to search for the phone number (defaults to phoneNumber).");
        phoneAttribute.setDefaultValue(PhoneOtpAuthenticator.DEFAULT_PHONE_ATTRIBUTE);

        ProviderConfigProperty codeLength = new ProviderConfigProperty();
        codeLength.setType(ProviderConfigProperty.STRING_TYPE);
        codeLength.setName(CONF_CODE_LENGTH);
        codeLength.setLabel("Code length");
        codeLength.setHelpText("Number of digits in the OTP code (defaults to 6).");
        codeLength.setDefaultValue(Integer.toString(PhoneOtpAuthenticator.DEFAULT_CODE_LENGTH));

        ProviderConfigProperty codeTtl = new ProviderConfigProperty();
        codeTtl.setType(ProviderConfigProperty.STRING_TYPE);
        codeTtl.setName(CONF_CODE_TTL);
        codeTtl.setLabel("Code time-to-live (seconds)");
        codeTtl.setHelpText("How long the OTP code is valid (defaults to 300 seconds).");
        codeTtl.setDefaultValue(Integer.toString(PhoneOtpAuthenticator.DEFAULT_TTL_SECONDS));

        ProviderConfigProperty maxAttempts = new ProviderConfigProperty();
        maxAttempts.setType(ProviderConfigProperty.STRING_TYPE);
        maxAttempts.setName(CONF_MAX_ATTEMPTS);
        maxAttempts.setLabel("Max attempts");
        maxAttempts.setHelpText("Maximum number of failed OTP attempts before restart (defaults to 3).");
        maxAttempts.setDefaultValue(Integer.toString(PhoneOtpAuthenticator.DEFAULT_MAX_ATTEMPTS));

        ProviderConfigProperty resendCooldown = new ProviderConfigProperty();
        resendCooldown.setType(ProviderConfigProperty.STRING_TYPE);
        resendCooldown.setName(CONF_RESEND_COOLDOWN);
        resendCooldown.setLabel("Resend cooldown (seconds)");
        resendCooldown.setHelpText("Minimum delay in seconds before another OTP can be sent (defaults to 30).");
        resendCooldown.setDefaultValue(Integer.toString(PhoneOtpAuthenticator.DEFAULT_RESEND_COOLDOWN_SECONDS));

        return Arrays.asList(phoneAttribute, codeLength, codeTtl, maxAttempts, resendCooldown);
    }
}
