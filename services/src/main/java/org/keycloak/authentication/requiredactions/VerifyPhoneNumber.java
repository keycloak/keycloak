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

package org.keycloak.authentication.requiredactions;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.keycloak.Config;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.authenticators.util.PhoneOtpUtils;
import org.keycloak.common.util.Time;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RequiredActionConfigModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.validation.Validation;
import org.keycloak.sessions.AuthenticationSessionModel;

import org.jboss.logging.Logger;

public class VerifyPhoneNumber implements RequiredActionProvider, RequiredActionFactory {

    private static final Logger logger = Logger.getLogger(VerifyPhoneNumber.class);

    public static final String PROVIDER_ID = "VERIFY_PHONE_NUMBER";

    static final String NOTE_PHONE_NUMBER = "VERIFY_PHONE_NUMBER_VALUE";
    static final String NOTE_OTP_HASH = "VERIFY_PHONE_OTP_HASH";
    static final String NOTE_OTP_CREATED = "VERIFY_PHONE_OTP_CREATED";
    static final String NOTE_OTP_ATTEMPTS = "VERIFY_PHONE_OTP_ATTEMPTS";
    static final String NOTE_OTP_LAST_SENT = "VERIFY_PHONE_OTP_LAST_SENT";

    static final String FORM_PHONE_NUMBER = "phoneNumber";
    static final String FORM_OTP = "otp";

    static final int DEFAULT_CODE_LENGTH = 6;
    static final int DEFAULT_TTL_SECONDS = 300;
    static final int DEFAULT_MAX_ATTEMPTS = 3;
    static final int DEFAULT_RESEND_COOLDOWN_SECONDS = 30;
    static final String DEFAULT_PHONE_ATTRIBUTE = "phoneNumber";
    static final String DEFAULT_PHONE_VERIFIED_ATTRIBUTE = "phoneNumberVerified";

    public static final String CONF_PHONE_ATTRIBUTE = "phone_attribute";
    public static final String CONF_PHONE_VERIFIED_ATTRIBUTE = "phone_verified_attribute";
    public static final String CONF_CODE_LENGTH = "code_length";
    public static final String CONF_CODE_TTL = "code_ttl";
    public static final String CONF_MAX_ATTEMPTS = "max_attempts";
    public static final String CONF_RESEND_COOLDOWN = "resend_cooldown";

    @Override
    public InitiatedActionSupport initiatedActionSupport() {
        return InitiatedActionSupport.SUPPORTED;
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        String phoneAttribute = getPhoneAttribute(context);
        String verifiedAttribute = getPhoneVerifiedAttribute(context);
        String phoneNumber = context.getUser().getFirstAttribute(phoneAttribute);
        String verified = context.getUser().getFirstAttribute(verifiedAttribute);

        if (!Validation.isBlank(phoneNumber) && !Boolean.parseBoolean(verified)) {
            context.getUser().addRequiredAction(PROVIDER_ID);
        }
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        String phoneNumber = authSession.getAuthNote(NOTE_PHONE_NUMBER);
        String otpHash = authSession.getAuthNote(NOTE_OTP_HASH);

        if (phoneNumber == null) {
            phoneNumber = context.getUser().getFirstAttribute(getPhoneAttribute(context));
        }

        if (phoneNumber == null || otpHash == null) {
            Response challenge = createPhoneForm(context, phoneNumber, null, null);
            context.challenge(challenge);
            return;
        }

        Response challenge = createCodeForm(context, phoneNumber, null, null);
        context.challenge(challenge);
    }

    @Override
    public void processAction(RequiredActionContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String phoneNumber = formData.getFirst(FORM_PHONE_NUMBER);
        String otp = formData.getFirst(FORM_OTP);

        if (formData.containsKey("resend")) {
            handleResend(context);
            return;
        }

        if (!Validation.isBlank(phoneNumber) && Validation.isBlank(otp)) {
            handlePhoneNumberStep(context, phoneNumber);
            return;
        }

        if (!Validation.isBlank(otp)) {
            handleOtpStep(context, otp);
            return;
        }

        Response challenge = createPhoneForm(context, phoneNumber, Messages.PHONE_OTP_MISSING_PHONE, FORM_PHONE_NUMBER);
        context.challenge(challenge);
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return this;
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
    public String getDisplayText() {
        return "Verify phone number";
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        List<ProviderConfigProperty> config = new ArrayList<>();

        ProviderConfigProperty phoneAttribute = new ProviderConfigProperty();
        phoneAttribute.setType(ProviderConfigProperty.STRING_TYPE);
        phoneAttribute.setName(CONF_PHONE_ATTRIBUTE);
        phoneAttribute.setLabel("Phone attribute");
        phoneAttribute.setHelpText("User attribute storing the phone number (defaults to phoneNumber).");
        phoneAttribute.setDefaultValue(DEFAULT_PHONE_ATTRIBUTE);

        ProviderConfigProperty verifiedAttribute = new ProviderConfigProperty();
        verifiedAttribute.setType(ProviderConfigProperty.STRING_TYPE);
        verifiedAttribute.setName(CONF_PHONE_VERIFIED_ATTRIBUTE);
        verifiedAttribute.setLabel("Phone verified attribute");
        verifiedAttribute.setHelpText("User attribute storing verification status (defaults to phoneNumberVerified).");
        verifiedAttribute.setDefaultValue(DEFAULT_PHONE_VERIFIED_ATTRIBUTE);

        ProviderConfigProperty codeLength = new ProviderConfigProperty();
        codeLength.setType(ProviderConfigProperty.STRING_TYPE);
        codeLength.setName(CONF_CODE_LENGTH);
        codeLength.setLabel("Code length");
        codeLength.setHelpText("Number of digits in the OTP code (defaults to 6).");
        codeLength.setDefaultValue(Integer.toString(DEFAULT_CODE_LENGTH));

        ProviderConfigProperty codeTtl = new ProviderConfigProperty();
        codeTtl.setType(ProviderConfigProperty.STRING_TYPE);
        codeTtl.setName(CONF_CODE_TTL);
        codeTtl.setLabel("Code time-to-live (seconds)");
        codeTtl.setHelpText("How long the OTP code is valid (defaults to 300 seconds).");
        codeTtl.setDefaultValue(Integer.toString(DEFAULT_TTL_SECONDS));

        ProviderConfigProperty maxAttempts = new ProviderConfigProperty();
        maxAttempts.setType(ProviderConfigProperty.STRING_TYPE);
        maxAttempts.setName(CONF_MAX_ATTEMPTS);
        maxAttempts.setLabel("Max attempts");
        maxAttempts.setHelpText("Maximum number of failed OTP attempts before restart (defaults to 3).");
        maxAttempts.setDefaultValue(Integer.toString(DEFAULT_MAX_ATTEMPTS));

        ProviderConfigProperty resendCooldown = new ProviderConfigProperty();
        resendCooldown.setType(ProviderConfigProperty.STRING_TYPE);
        resendCooldown.setName(CONF_RESEND_COOLDOWN);
        resendCooldown.setLabel("Resend cooldown (seconds)");
        resendCooldown.setHelpText("Minimum delay in seconds before another OTP can be sent (defaults to 30).");
        resendCooldown.setDefaultValue(Integer.toString(DEFAULT_RESEND_COOLDOWN_SECONDS));

        config.add(phoneAttribute);
        config.add(verifiedAttribute);
        config.add(codeLength);
        config.add(codeTtl);
        config.add(maxAttempts);
        config.add(resendCooldown);
        return config;
    }

    private void handlePhoneNumberStep(RequiredActionContext context, String phoneNumber) {
        String trimmedPhone = phoneNumber == null ? null : phoneNumber.trim();
        if (Validation.isBlank(trimmedPhone)) {
            Response challenge = createPhoneForm(context, phoneNumber, Messages.PHONE_OTP_MISSING_PHONE, FORM_PHONE_NUMBER);
            context.challenge(challenge);
            return;
        }

        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        authSession.setAuthNote(NOTE_PHONE_NUMBER, trimmedPhone);
        generateAndStoreCode(context, authSession, trimmedPhone);

        Response challenge = createCodeForm(context, trimmedPhone, null, null);
        context.challenge(challenge);
    }

    private void handleOtpStep(RequiredActionContext context, String otp) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        String storedHash = authSession.getAuthNote(NOTE_OTP_HASH);
        String phoneNumber = authSession.getAuthNote(NOTE_PHONE_NUMBER);

        if (storedHash == null || phoneNumber == null) {
            Response challenge = createPhoneForm(context, phoneNumber, null, null);
            context.challenge(challenge);
            return;
        }

        if (Validation.isBlank(otp)) {
            Response challenge = createCodeForm(context, phoneNumber, Messages.MISSING_TOTP, Validation.FIELD_OTP_CODE);
            context.challenge(challenge);
            return;
        }

        if (isExpired(authSession, getCodeTtlSeconds(context))) {
            clearOtpNotes(authSession);
            Response challenge = createPhoneForm(context, phoneNumber, Messages.PHONE_OTP_EXPIRED, null);
            context.challenge(challenge);
            return;
        }

        int attempts = getAttempts(authSession);
        int maxAttempts = getMaxAttempts(context);
        if (attempts >= maxAttempts) {
            clearOtpNotes(authSession);
            Response challenge = createPhoneForm(context, phoneNumber, Messages.PHONE_OTP_TOO_MANY_ATTEMPTS, null);
            context.challenge(challenge);
            return;
        }

        String expectedHash = hashCode(authSession, otp);
        if (!MessageDigest.isEqual(expectedHash.getBytes(StandardCharsets.UTF_8), storedHash.getBytes(StandardCharsets.UTF_8))) {
            updateAttempts(authSession, attempts + 1);
            Response challenge = createCodeForm(context, phoneNumber, Messages.INVALID_TOTP, Validation.FIELD_OTP_CODE);
            context.challenge(challenge);
            return;
        }

        String phoneAttribute = getPhoneAttribute(context);
        String verifiedAttribute = getPhoneVerifiedAttribute(context);
        context.getUser().setSingleAttribute(phoneAttribute, phoneNumber);
        context.getUser().setSingleAttribute(verifiedAttribute, "true");
        context.getUser().removeRequiredAction(PROVIDER_ID);
        clearOtpNotes(authSession);
        context.success();
    }

    private void handleResend(RequiredActionContext context) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        String phoneNumber = authSession.getAuthNote(NOTE_PHONE_NUMBER);

        if (phoneNumber == null) {
            Response challenge = createPhoneForm(context, null, null, null);
            context.challenge(challenge);
            return;
        }

        Long remaining = getResendCooldownRemaining(authSession, getResendCooldownSeconds(context));
        if (remaining != null) {
            Response challenge = createCodeForm(context, phoneNumber, Messages.PHONE_OTP_RESEND_COOLDOWN, null, remaining);
            context.challenge(challenge);
            return;
        }

        generateAndStoreCode(context, authSession, phoneNumber);
        Response challenge = createCodeFormInfo(context, phoneNumber, Messages.PHONE_OTP_CODE_RESENT);
        context.challenge(challenge);
    }

    private void generateAndStoreCode(RequiredActionContext context, AuthenticationSessionModel authSession, String phoneNumber) {
        int codeLength = getCodeLength(context);
        String code = generateCode(codeLength);
        authSession.setAuthNote(NOTE_OTP_HASH, hashCode(authSession, code));
        authSession.setAuthNote(NOTE_OTP_CREATED, Integer.toString(Time.currentTime()));
        authSession.setAuthNote(NOTE_OTP_ATTEMPTS, "0");
        authSession.setAuthNote(NOTE_OTP_LAST_SENT, Integer.toString(Time.currentTime()));

        logger.infof("Phone verification code for %s is %s", phoneNumber, code);

    }

    private String generateCode(int length) {
        int safeLength = length <= 0 ? DEFAULT_CODE_LENGTH : length;
        return PhoneOtpUtils.generateCode(safeLength);
    }

    private String hashCode(AuthenticationSessionModel authSession, String code) {
        String salt = authSession.getParentSession().getId() + ":" + authSession.getTabId();
        return PhoneOtpUtils.hashCode(code, salt);
    }

    private boolean isExpired(AuthenticationSessionModel authSession, int ttlSeconds) {
        String created = authSession.getAuthNote(NOTE_OTP_CREATED);
        if (created == null) {
            return true;
        }
        int createdAt;
        try {
            createdAt = Integer.parseInt(created);
        } catch (NumberFormatException ex) {
            return true;
        }
        return PhoneOtpUtils.isExpired(createdAt, ttlSeconds, Time.currentTime());
    }

    private int getAttempts(AuthenticationSessionModel authSession) {
        String attempts = authSession.getAuthNote(NOTE_OTP_ATTEMPTS);
        if (attempts == null) {
            return 0;
        }
        try {
            return Integer.parseInt(attempts);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void updateAttempts(AuthenticationSessionModel authSession, int attempts) {
        authSession.setAuthNote(NOTE_OTP_ATTEMPTS, Integer.toString(attempts));
    }

    private void clearOtpNotes(AuthenticationSessionModel authSession) {
        authSession.removeAuthNote(NOTE_OTP_HASH);
        authSession.removeAuthNote(NOTE_OTP_CREATED);
        authSession.removeAuthNote(NOTE_OTP_ATTEMPTS);
        authSession.removeAuthNote(NOTE_OTP_LAST_SENT);
        authSession.removeAuthNote(NOTE_PHONE_NUMBER);
    }

    private String getPhoneAttribute(RequiredActionContext context) {
        String value = getConfigValue(context, CONF_PHONE_ATTRIBUTE);
        if (Validation.isBlank(value)) {
            return DEFAULT_PHONE_ATTRIBUTE;
        }
        return value;
    }

    private String getPhoneVerifiedAttribute(RequiredActionContext context) {
        String value = getConfigValue(context, CONF_PHONE_VERIFIED_ATTRIBUTE);
        if (Validation.isBlank(value)) {
            return DEFAULT_PHONE_VERIFIED_ATTRIBUTE;
        }
        return value;
    }

    private int getCodeLength(RequiredActionContext context) {
        int value = getConfigInt(context, CONF_CODE_LENGTH, DEFAULT_CODE_LENGTH);
        if (value < 4 || value > 9) {
            return DEFAULT_CODE_LENGTH;
        }
        return value;
    }

    private int getCodeTtlSeconds(RequiredActionContext context) {
        int value = getConfigInt(context, CONF_CODE_TTL, DEFAULT_TTL_SECONDS);
        if (value <= 0) {
            return DEFAULT_TTL_SECONDS;
        }
        return value;
    }

    private int getMaxAttempts(RequiredActionContext context) {
        int value = getConfigInt(context, CONF_MAX_ATTEMPTS, DEFAULT_MAX_ATTEMPTS);
        if (value <= 0) {
            return DEFAULT_MAX_ATTEMPTS;
        }
        return value;
    }

    private int getResendCooldownSeconds(RequiredActionContext context) {
        int value = getConfigInt(context, CONF_RESEND_COOLDOWN, DEFAULT_RESEND_COOLDOWN_SECONDS);
        if (value < 0) {
            return DEFAULT_RESEND_COOLDOWN_SECONDS;
        }
        return value;
    }

    private Long getResendCooldownRemaining(AuthenticationSessionModel authSession, int cooldownSeconds) {
        if (cooldownSeconds <= 0) {
            return null;
        }
        String lastSent = authSession.getAuthNote(NOTE_OTP_LAST_SENT);
        if (lastSent == null) {
            return null;
        }
        int lastSentAt;
        try {
            lastSentAt = Integer.parseInt(lastSent);
        } catch (NumberFormatException ex) {
            return null;
        }
        return PhoneOtpUtils.cooldownRemaining(lastSentAt, cooldownSeconds, Time.currentTime());
    }

    private int getConfigInt(RequiredActionContext context, String key, int defaultValue) {
        String raw = getConfigValue(context, key);
        if (Validation.isBlank(raw)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private String getConfigValue(RequiredActionContext context, String key) {
        RequiredActionConfigModel configModel = context.getConfig();
        if (configModel == null) {
            return null;
        }
        Map<String, String> config = configModel.getConfig();
        if (config == null) {
            return null;
        }
        return config.get(key);
    }

    private Response createPhoneForm(RequiredActionContext context, String phoneNumber, String error, String field, Object... params) {
        LoginFormsProvider form = context.form();
        if (phoneNumber != null) {
            form.setAttribute(FORM_PHONE_NUMBER, phoneNumber);
        }
        if (error != null) {
            if (field != null) {
                form.addError(new FormMessage(field, error, params));
            } else {
                form.setError(error, params);
            }
        }
        return form.createForm("verify-phone-number.ftl");
    }

    private Response createCodeForm(RequiredActionContext context, String phoneNumber, String error, String field, Object... params) {
        LoginFormsProvider form = context.form();
        if (phoneNumber != null) {
            form.setAttribute(FORM_PHONE_NUMBER, phoneNumber);
        }
        if (error != null) {
            if (field != null) {
                form.addError(new FormMessage(field, error, params));
            } else {
                form.setError(error, params);
            }
        }
        return form.createForm("verify-phone-number-code.ftl");
    }

    private Response createCodeFormInfo(RequiredActionContext context, String phoneNumber, String infoMessage, Object... params) {
        LoginFormsProvider form = context.form();
        if (phoneNumber != null) {
            form.setAttribute(FORM_PHONE_NUMBER, phoneNumber);
        }
        if (infoMessage != null) {
            form.setInfo(infoMessage, params);
        }
        return form.createForm("verify-phone-number-code.ftl");
    }
}
