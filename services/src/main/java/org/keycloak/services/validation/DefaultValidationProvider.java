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
import org.keycloak.validation.Validation;
import org.keycloak.validation.ValidationContextKey;
import org.keycloak.validation.ValidationKey;
import org.keycloak.validation.ValidationProvider;
import org.keycloak.validation.ValidationRegistry.MutableValidationRegistry;

public class DefaultValidationProvider implements ValidationProvider {

    @Override
    public void register(MutableValidationRegistry registry) {

        // TODO add additional validations

        registry.register("builtin_user_username_validation",
                createUsernameValidation(), ValidationKey.USER_USERNAME,
                ValidationContextKey.USER_DEFAULT_CONTEXT_KEY);

        registry.register("builtin_user_email_validation",
                createEmailValidation(), ValidationKey.USER_EMAIL,
                ValidationContextKey.USER_DEFAULT_CONTEXT_KEY);

        // TODO firstname / lastname validation could be merged?
        registry.register("builtin_user_firstname_validation",
                createFirstnameValidation(), ValidationKey.USER_FIRSTNAME,
                ValidationContextKey.USER_DEFAULT_CONTEXT_KEY);

        registry.register("builtin_user_lastname_validation",
                createLastnameValidation(), ValidationKey.USER_LASTNAME,
                ValidationContextKey.USER_DEFAULT_CONTEXT_KEY);

        registry.register("builtin_user_validation",
                createUserValidation(), ValidationKey.USER,
                ValidationContextKey.USER_DEFAULT_CONTEXT_KEY);
    }

    protected Validation createUserValidation() {
        return (key, value, context) -> {

            UserModel user = value instanceof UserModel ? (UserModel) value : null;

            if (user == null) {
                return false;
            }

            boolean valid;

            valid = context.validateNested(ValidationKey.USER_EMAIL, user.getEmail());
            valid &= context.validateNested(ValidationKey.USER_USERNAME, user.getUsername());
            valid &= context.validateNested(ValidationKey.USER_FIRSTNAME, user.getFirstName());
            valid &= context.validateNested(ValidationKey.USER_LASTNAME, user.getLastName());

            return valid;
        };
    }

    protected Validation createLastnameValidation() {
        return (key, value, context) -> {

            String input = value instanceof String ? (String) value : null;

            if (org.keycloak.services.validation.Validation.isBlank(input)) {
                context.addError(key, Messages.MISSING_LAST_NAME);
                return false;
            }
            return true;
        };
    }

    protected Validation createFirstnameValidation() {
        return (key, value, context) -> {

            String input = value instanceof String ? (String) value : null;

            if (org.keycloak.services.validation.Validation.isBlank(input)) {
                context.addError(key, Messages.MISSING_FIRST_NAME);
                return false;
            }
            return true;
        };
    }

    protected Validation createEmailValidation() {
        return (key, value, context) -> {

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
        };
    }

    protected Validation createUsernameValidation() {
        return (key, value, context) -> {

            String input = value instanceof String ? (String) value : null;

            boolean usernameRequired = context.getAttributeAsBoolean("userNameRequired");
            if (usernameRequired && org.keycloak.services.validation.Validation.isBlank(input)) {
                context.addError(key, Messages.MISSING_USERNAME);
                return false;
            }
            return true;
        };
    }
}
