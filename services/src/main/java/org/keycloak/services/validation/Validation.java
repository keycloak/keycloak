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

package org.keycloak.services.validation;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.keycloak.authentication.requiredactions.util.UpdateProfileContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.policy.PasswordPolicyManagerProvider;
import org.keycloak.policy.PolicyError;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.messages.Messages;

import javax.ws.rs.core.MultivaluedMap;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class Validation {

    public static final String FIELD_PASSWORD_CONFIRM = "password-confirm";
    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_LAST_NAME = "lastName";
    public static final String FIELD_FIRST_NAME = "firstName";
    public static final String FIELD_PASSWORD = "password";
    public static final String FIELD_USERNAME = "username";

    // Actually allow same emails like angular. See ValidationTest.testEmailValidation()
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9!#$%&'*+/=?^_`{|}~.-]+@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*");

    // Provides a fast lookup for custom E-Mail validation patterns
    private static final LoadingCache<String, Pattern> CUSTOM_EMAIL_VALIDATION_PATTERN_CACHE = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .build(CacheLoader.<String,Pattern>from(Pattern::compile));

    public static List<FormMessage> validateRegistrationForm(KeycloakSession session, RealmModel realm, MultivaluedMap<String, String> formData, List<String> requiredCredentialTypes, PasswordPolicy policy) {
        List<FormMessage> errors = new ArrayList<>();

        if (!realm.isRegistrationEmailAsUsername() && isBlank(formData.getFirst(FIELD_USERNAME))) {
            addError(errors, FIELD_USERNAME, Messages.MISSING_USERNAME);
        }

        if (isBlank(formData.getFirst(FIELD_FIRST_NAME))) {
            addError(errors, FIELD_FIRST_NAME, Messages.MISSING_FIRST_NAME);
        }

        if (isBlank(formData.getFirst(FIELD_LAST_NAME))) {
            addError(errors, FIELD_LAST_NAME, Messages.MISSING_LAST_NAME);
        }

        if (isBlank(formData.getFirst(FIELD_EMAIL))) {
            addError(errors, FIELD_EMAIL, Messages.MISSING_EMAIL);
        } else if (!isEmailValid(formData.getFirst(FIELD_EMAIL))) {
            addError(errors, FIELD_EMAIL, Messages.INVALID_EMAIL);
        }

        if (requiredCredentialTypes.contains(CredentialRepresentation.PASSWORD)) {
            if (isBlank(formData.getFirst(FIELD_PASSWORD))) {
                addError(errors, FIELD_PASSWORD, Messages.MISSING_PASSWORD);
            } else if (!formData.getFirst(FIELD_PASSWORD).equals(formData.getFirst(FIELD_PASSWORD_CONFIRM))) {
                addError(errors, FIELD_PASSWORD_CONFIRM, Messages.INVALID_PASSWORD_CONFIRM);
            }
        }

        if (formData.getFirst(FIELD_PASSWORD) != null) {
            PolicyError err = session.getProvider(PasswordPolicyManagerProvider.class).validate(realm.isRegistrationEmailAsUsername() ? formData.getFirst(FIELD_EMAIL) : formData.getFirst(FIELD_USERNAME), formData.getFirst(FIELD_PASSWORD));
            if (err != null)
                errors.add(new FormMessage(FIELD_PASSWORD, err.getMessage(), err.getParameters()));
        }

        return errors;
    }

    private static void addError(List<FormMessage> errors, String field, String message) {
        errors.add(new FormMessage(field, message));
    }

    public static List<FormMessage> validateUpdateProfileForm(RealmModel realm, MultivaluedMap<String, String> formData) {
        return validateUpdateProfileForm(realm, formData, realm.isEditUsernameAllowed());
    }

    public static List<FormMessage> validateUpdateProfileForm(RealmModel realm, MultivaluedMap<String, String> formData, boolean userNameRequired) {
        List<FormMessage> errors = new ArrayList<>();

        if (!realm.isRegistrationEmailAsUsername() && userNameRequired && isBlank(formData.getFirst(FIELD_USERNAME))) {
            addError(errors, FIELD_USERNAME, Messages.MISSING_USERNAME);
        }

        if (isBlank(formData.getFirst(FIELD_FIRST_NAME))) {
            addError(errors, FIELD_FIRST_NAME, Messages.MISSING_FIRST_NAME);
        }

        if (isBlank(formData.getFirst(FIELD_LAST_NAME))) {
            addError(errors, FIELD_LAST_NAME, Messages.MISSING_LAST_NAME);
        }

        String email = formData.getFirst(FIELD_EMAIL);
        if (isBlank(email)) {
            addError(errors, FIELD_EMAIL, Messages.MISSING_EMAIL);
        } else if (!isEmailValid(email)) {
            addError(errors, FIELD_EMAIL, Messages.INVALID_EMAIL);
        }

        String allowedEmailPattern = realm.getAllowedEmailPattern();
        if (allowedEmailPattern != null) {
            if (!isValidEmailCustom(allowedEmailPattern, email)) {
                addError(errors, FIELD_EMAIL, Messages.INVALID_EMAIL_NOT_ALLOWED);
            }
        }

        return errors;
    }

    /**
     * Validate if user object contains all mandatory fields.
     *
     * @param realm user is for
     * @param user  to validate
     * @return true if user object contains all mandatory values, false if some mandatory value is missing
     */
    public static boolean validateUserMandatoryFields(RealmModel realm, UpdateProfileContext user) {
        return !(isBlank(user.getFirstName()) || isBlank(user.getLastName()) || isBlank(user.getEmail()));
    }

    /**
     * Check if string is empty (null or lenght is 0)
     *
     * @param s to check
     * @return true if string is empty
     */
    public static boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }

    /**
     * Check if string is blank (null or lenght is 0 or contains only white characters)
     *
     * @param s to check
     * @return true if string is blank
     */
    public static boolean isBlank(String s) {
        return s == null || s.trim().length() == 0;
    }

    /**
     * Checks if the given {@code email} String is a valid E-Mail address.
     *
     * @param email
     * @return
     */
    public static boolean isEmailValid(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Checks if the given {@code email} String is a valid E-Mail address according to the given {@code validEmailPattern}.
     *
     * @param validEmailPattern
     * @param email
     * @return
     */
    public static boolean isValidEmailCustom(String validEmailPattern, String email) {

        if (isBlank(validEmailPattern)) {
            return true;
        }

        Pattern pattern = CUSTOM_EMAIL_VALIDATION_PATTERN_CACHE.getUnchecked(validEmailPattern.trim());
        return pattern.matcher(email).matches();
    }
}
