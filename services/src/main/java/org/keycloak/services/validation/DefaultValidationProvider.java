/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.models.UserModel;
import org.keycloak.services.messages.Messages;
import org.keycloak.validation.NestedValidationContext;
import org.keycloak.validation.ValidationContextKey;
import org.keycloak.validation.ValidationKey;
import org.keycloak.validation.ValidationProvider;
import org.keycloak.validation.ValidationRegistry.MutableValidationRegistry;

public class DefaultValidationProvider implements ValidationProvider {

    @Override
    public void register(MutableValidationRegistry registry) {

        // TODO add additional validations

        UserValidation userValidation = createUserValidation();

        registry.addValidation(ValidationKey.USER_USERNAME, userValidation::validateUsername, "builtin_user_username_validation",
                ValidationContextKey.USER_DEFAULT_CONTEXT_KEY);

        registry.addValidation(ValidationKey.USER_EMAIL, userValidation::validateEmail, "builtin_user_email_validation",
                ValidationContextKey.USER_DEFAULT_CONTEXT_KEY);

        // TODO firstname / lastname validation could be merged?
        registry.addValidation(ValidationKey.USER_FIRSTNAME, userValidation::validateFirstname, "builtin_user_firstname_validation",
                ValidationContextKey.USER_DEFAULT_CONTEXT_KEY);

        registry.addValidation(ValidationKey.USER_LASTNAME, userValidation::validateLastname, "builtin_user_lastname_validation",
                ValidationContextKey.USER_DEFAULT_CONTEXT_KEY);

        registry.addValidation(ValidationKey.USER, userValidation::validateUser, "builtin_user_validation",
                ValidationContextKey.USER_DEFAULT_CONTEXT_KEY);
    }

    protected UserValidation createUserValidation() {
        return new UserValidation();
    }

    public static class UserValidation {

        public boolean validateUsername(ValidationKey key, Object value, NestedValidationContext context) {

            String input = value instanceof String ? (String) value : null;

            boolean usernameRequired = context.getAttributeAsBoolean("userNameRequired");
            if (usernameRequired && Validation.isBlank(input)) {
                context.addError(key, Messages.MISSING_USERNAME);
                return false;
            }
            return true;
        }

        public boolean validateEmail(ValidationKey key, Object value, NestedValidationContext context) {

            String input = value instanceof String ? (String) value : null;

            if (org.keycloak.services.validation.Validation.isBlank(input)) {
                context.addError(key, Messages.MISSING_EMAIL);
                return false;
            }

            if (!org.keycloak.services.validation.Validation.isEmailValid(input)) {
                context.addError(key, Messages.INVALID_EMAIL);
                return false;
            }

            return true;
        }

        public boolean validateUser(ValidationKey key, Object value, NestedValidationContext context) {

            UserModel user = value instanceof UserModel ? (UserModel) value : null;

            if (user == null) {
                return false;
            }

            boolean valid;

            valid = validateEmail(ValidationKey.USER_EMAIL, user.getEmail(), context);
            valid &= validateUsername(ValidationKey.USER_USERNAME, user.getUsername(), context);
            valid &= validateFirstname(ValidationKey.USER_FIRSTNAME, user.getFirstName(), context);
            valid &= validateLastname(ValidationKey.USER_LASTNAME, user.getLastName(), context);

            return valid;
        }

        public boolean validateFirstname(ValidationKey key, Object value, NestedValidationContext context) {

            String input = value instanceof String ? (String) value : null;

            if (org.keycloak.services.validation.Validation.isBlank(input)) {
                context.addError(key, Messages.MISSING_FIRST_NAME);
                return false;
            }
            return true;
        }

        public boolean validateLastname(ValidationKey key, Object value, NestedValidationContext context) {

            String input = value instanceof String ? (String) value : null;

            if (org.keycloak.services.validation.Validation.isBlank(input)) {
                context.addError(key, Messages.MISSING_LAST_NAME);
                return false;
            }
            return true;
        }
    }
}
