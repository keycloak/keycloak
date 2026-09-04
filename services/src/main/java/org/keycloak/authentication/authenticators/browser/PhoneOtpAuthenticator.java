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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.authenticators.util.PhoneOtpUtils;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.validation.Validation;
import org.keycloak.sessions.AuthenticationSessionModel;

public class PhoneOtpAuthenticator extends AbstractUsernameFormAuthenticator {

    private static final Logger logger = Logger.getLogger(PhoneOtpAuthenticator.class);

    static final String NOTE_PHONE_NUMBER = "PHONE_OTP_PHONE_NUMBER";
    static final String NOTE_OTP_HASH = "PHONE_OTP_CODE_HASH";
    static final String NOTE_OTP_CREATED = "PHONE_OTP_CODE_CREATED";
    static final String NOTE_OTP_ATTEMPTS = "PHONE_OTP_CODE_ATTEMPTS";
    static final String NOTE_OTP_LAST_SENT = "PHONE_OTP_CODE_LAST_SENT";
    static final String NOTE_USER_ID = "PHONE_OTP_USER_ID";

    static final String FORM_PHONE_NUMBER = "phoneNumber";
    static final String FORM_OTP = "otp";

    static final int DEFAULT_CODE_LENGTH = 6;
    static final int DEFAULT_TTL_SECONDS = 300;
    static final int DEFAULT_MAX_ATTEMPTS = 3;
    static final int DEFAULT_RESEND_COOLDOWN_SECONDS = 30;
    static final String DEFAULT_PHONE_ATTRIBUTE = "phoneNumber";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        String phoneNumber = authSession.getAuthNote(NOTE_PHONE_NUMBER);
        String otpHash = authSession.getAuthNote(NOTE_OTP_HASH);

        if (phoneNumber == null || otpHash == null) {
            context.challenge(createPhoneForm(context, phoneNumber, null, null));
            return;
        }

        context.challenge(createCodeForm(context, phoneNumber, null, null));
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> inputData = context.getHttpRequest().getDecodedFormParameters();
        String phoneNumber = inputData.getFirst(FORM_PHONE_NUMBER);
        String otp = inputData.getFirst(FORM_OTP);

        if (inputData.containsKey("resend")) {
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
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }

    private void handlePhoneNumberStep(AuthenticationFlowContext context, String phoneNumber) {
        String trimmedPhone = phoneNumber == null ? null : phoneNumber.trim();
        if (Validation.isBlank(trimmedPhone)) {
            Response challenge = createPhoneForm(context, phoneNumber, Messages.PHONE_OTP_MISSING_PHONE, FORM_PHONE_NUMBER);
            context.challenge(challenge);
            return;
        }

        UserModel user = findUserByPhone(context, trimmedPhone);
        if (user == null) {
            return;
        }

        context.getEvent().user(user).detail(Details.USERNAME, user.getUsername());
        if (!enabledUser(context, user)) {
            return;
        }

        context.setUser(user);
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        authSession.setAuthNote(NOTE_PHONE_NUMBER, trimmedPhone);
        authSession.setAuthNote(NOTE_USER_ID, user.getId());

        generateAndStoreCode(context, authSession, trimmedPhone);

        Response challenge = createCodeForm(context, trimmedPhone, null, null);
        context.challenge(challenge);
    }

    private void handleOtpStep(AuthenticationFlowContext context, String otp) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        String storedHash = authSession.getAuthNote(NOTE_OTP_HASH);
        String phoneNumber = authSession.getAuthNote(NOTE_PHONE_NUMBER);

        if (storedHash == null || phoneNumber == null) {
            Response challenge = createPhoneForm(context, phoneNumber, null, null);
            context.challenge(challenge);
            return;
        }

        UserModel user = resolveUser(context);
        if (user == null) {
            Response challenge = createPhoneForm(context, phoneNumber, Messages.PHONE_OTP_INVALID_PHONE, FORM_PHONE_NUMBER);
            context.failureChallenge(AuthenticationFlowError.INVALID_USER, challenge);
            return;
        }

        context.getEvent().user(user).detail(Details.USERNAME, user.getUsername());
        if (!enabledUser(context, user)) {
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
            context.failureChallenge(AuthenticationFlowError.EXPIRED_CODE, challenge);
            return;
        }

        int attempts = getAttempts(authSession);
        int maxAttempts = getMaxAttempts(context);
        if (attempts >= maxAttempts) {
            clearOtpNotes(authSession);
            Response challenge = createPhoneForm(context, phoneNumber, Messages.PHONE_OTP_TOO_MANY_ATTEMPTS, null);
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challenge);
            return;
        }

        String expectedHash = hashCode(authSession, otp);
        if (!MessageDigest.isEqual(expectedHash.getBytes(StandardCharsets.UTF_8), storedHash.getBytes(StandardCharsets.UTF_8))) {
            updateAttempts(authSession, attempts + 1);
            context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
            Response challenge = createCodeForm(context, phoneNumber, Messages.INVALID_TOTP, Validation.FIELD_OTP_CODE);
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challenge);
            return;
        }

        clearOtpNotes(authSession);
        context.success();
    }

    private void handleResend(AuthenticationFlowContext context) {
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

    private UserModel resolveUser(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        if (user != null) {
            return user;
        }

        String userId = context.getAuthenticationSession().getAuthNote(NOTE_USER_ID);
        if (userId == null) {
            return null;
        }

        UserModel resolved = context.getSession().users().getUserById(context.getRealm(), userId);
        if (resolved != null) {
            context.setUser(resolved);
        }
        return resolved;
    }

    private UserModel findUserByPhone(AuthenticationFlowContext context, String phoneNumber) {
        String attributeName = getPhoneAttribute(context);
        Stream<UserModel> usersStream = context.getSession().users()
                .searchForUserByUserAttributeStream(context.getRealm(), attributeName, phoneNumber);
        List<UserModel> users = usersStream.limit(2).collect(Collectors.toList());

        if (users.isEmpty()) {
            context.getEvent().error(Errors.USER_NOT_FOUND);
            Response challenge = createPhoneForm(context, phoneNumber, Messages.PHONE_OTP_INVALID_PHONE, FORM_PHONE_NUMBER);
            context.failureChallenge(AuthenticationFlowError.INVALID_USER, challenge);
            return null;
        }

        if (users.size() > 1) {
            context.getEvent().error(Errors.INVALID_USER);
            Response challenge = createPhoneForm(context, phoneNumber, Messages.PHONE_OTP_DUPLICATE_PHONE, FORM_PHONE_NUMBER);
            context.failureChallenge(AuthenticationFlowError.USER_CONFLICT, challenge);
            return null;
        }

        return users.get(0);
    }

    private void generateAndStoreCode(AuthenticationFlowContext context, AuthenticationSessionModel authSession, String phoneNumber) {
        int codeLength = getCodeLength(context);
        String code = generateCode(codeLength);
        authSession.setAuthNote(NOTE_OTP_HASH, hashCode(authSession, code));
        authSession.setAuthNote(NOTE_OTP_CREATED, Integer.toString(Time.currentTime()));
        authSession.setAuthNote(NOTE_OTP_ATTEMPTS, "0");
        authSession.setAuthNote(NOTE_OTP_LAST_SENT, Integer.toString(Time.currentTime()));

        logger.infof("Phone OTP code for %s is %s", phoneNumber, code);
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
        authSession.removeAuthNote(NOTE_USER_ID);
    }

    private String getPhoneAttribute(AuthenticationFlowContext context) {
        String value = getConfigValue(context, PhoneOtpAuthenticatorFactory.CONF_PHONE_ATTRIBUTE);
        if (Validation.isBlank(value)) {
            return DEFAULT_PHONE_ATTRIBUTE;
        }
        return value;
    }

    private int getCodeLength(AuthenticationFlowContext context) {
        int value = getConfigInt(context, PhoneOtpAuthenticatorFactory.CONF_CODE_LENGTH, DEFAULT_CODE_LENGTH);
        if (value < 4 || value > 9) {
            return DEFAULT_CODE_LENGTH;
        }
        return value;
    }

    private int getCodeTtlSeconds(AuthenticationFlowContext context) {
        int value = getConfigInt(context, PhoneOtpAuthenticatorFactory.CONF_CODE_TTL, DEFAULT_TTL_SECONDS);
        if (value <= 0) {
            return DEFAULT_TTL_SECONDS;
        }
        return value;
    }

    private int getMaxAttempts(AuthenticationFlowContext context) {
        int value = getConfigInt(context, PhoneOtpAuthenticatorFactory.CONF_MAX_ATTEMPTS, DEFAULT_MAX_ATTEMPTS);
        if (value <= 0) {
            return DEFAULT_MAX_ATTEMPTS;
        }
        return value;
    }

    private int getResendCooldownSeconds(AuthenticationFlowContext context) {
        int value = getConfigInt(context, PhoneOtpAuthenticatorFactory.CONF_RESEND_COOLDOWN, DEFAULT_RESEND_COOLDOWN_SECONDS);
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

    private int getConfigInt(AuthenticationFlowContext context, String key, int defaultValue) {
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

    private String getConfigValue(AuthenticationFlowContext context, String key) {
        AuthenticatorConfigModel configModel = context.getAuthenticatorConfig();
        if (configModel == null) {
            return null;
        }
        Map<String, String> config = configModel.getConfig();
        if (config == null) {
            return null;
        }
        return config.get(key);
    }

    private Response createPhoneForm(AuthenticationFlowContext context, String phoneNumber, String error, String field, Object... params) {
        LoginFormsProvider form = context.form().setExecution(context.getExecution().getId());
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
        return form.createForm("phone-otp-login.ftl");
    }

    private Response createCodeForm(AuthenticationFlowContext context, String phoneNumber, String error, String field, Object... params) {
        LoginFormsProvider form = context.form().setExecution(context.getExecution().getId());
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
        return form.createForm("phone-otp-code.ftl");
    }

    private Response createCodeFormInfo(AuthenticationFlowContext context, String phoneNumber, String infoMessage, Object... params) {
        LoginFormsProvider form = context.form().setExecution(context.getExecution().getId());
        if (phoneNumber != null) {
            form.setAttribute(FORM_PHONE_NUMBER, phoneNumber);
        }
        if (infoMessage != null) {
            form.setInfo(infoMessage, params);
        }
        return form.createForm("phone-otp-code.ftl");
    }
}
