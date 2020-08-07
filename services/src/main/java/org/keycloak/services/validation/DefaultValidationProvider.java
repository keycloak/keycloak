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

        // TODO add additional validators
        registry.register(createUsernameValidation(), ValidationKey.User.USERNAME,
                ValidationContextKey.User.PROFILE_UPDATE, ValidationContextKey.User.REGISTRATION);

        registry.register(createEmailValidation(), ValidationKey.User.EMAIL,
                ValidationContextKey.User.PROFILE_UPDATE, ValidationContextKey.User.REGISTRATION);

        // TODO firstname / lastname validation could be merged?
        registry.register(createFirstnameValidation(), ValidationKey.User.FIRSTNAME,
                ValidationContextKey.User.PROFILE_UPDATE);

        registry.register(createLastnameValidation(),
                ValidationKey.User.LASTNAME,
                ValidationContextKey.User.PROFILE_UPDATE);
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

            if (!context.getRealm().isRegistrationEmailAsUsername()
                    && context.getAttributeAsBoolean("userNameRequired")
                    && org.keycloak.services.validation.Validation.isBlank(input)) {
                context.addError(key, Messages.MISSING_USERNAME);
                return false;
            }
            return true;
        };
    }

//    protected Validation uniqueEmailValidation() {
//        return (key, value, context) -> {
//
//            if (context.getRealm().isDuplicateEmailsAllowed()) {
//                return true;
//            }
//
//            String input = value instanceof String ? (String) value : null;
//            if (input == null) {
//                context.addError(key, Messages.MISSING_EMAIL);
//            }
//
//            UserModel userByEmail = context.getSession().users().getUserByEmail(input, context.getRealm());
//            return userByEmail == null;
//        };
//    }

//
//    protected Validation createProfileValidation() {
//        return (key, value, context) -> {
//
//            UserModel input = value instanceof UserModel ? (UserModel) value : null;
//
//            if (input == null) {
//                context.addError(key, Messages.INVALID_USER);
//                return false;
//            }
//
//            if (!"content".equals(input.getFirstAttribute("FAKE_FIELD"))) {
//                context.addError(key, "FAKE_FIELD_ERRORKEY");
//                return false;
//            }
//
//            boolean emailValid = context.validateNested(ValidationKey.User.EMAIL, input.getEmail());
//            if (!emailValid) {
//                context.addError(key, Messages.INVALID_EMAIL);
//            }
//
//            return emailValid;
//        };
//    }
}
