/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.userprofile.validator;

import java.util.List;
import java.util.Objects;

import jakarta.ws.rs.core.Response;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.validation.Validation;
import org.keycloak.userprofile.UserProfileAttributeValidationContext;
import org.keycloak.validate.SimpleValidator;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.ValidatorConfig;

/**
 * Validator to check User Profile email duplication conditions based on realm settings like isDuplicateEmailsAllowed.
 * Expects List of Strings as input.
 * 
 * @author Vlastimil Elias <velias@redhat.com>
 *
 */
public class DuplicateEmailValidator implements SimpleValidator {

    public static final String ID = "up-duplicate-email";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ValidationContext validate(Object input, String inputHint, ValidationContext context, ValidatorConfig config) {
        @SuppressWarnings("unchecked")
        List<String> values = (List<String>) input;

        if (values == null || values.isEmpty()) {
            return context;
        }

        String value = values.get(0);

        if (Validation.isBlank(value))
            return context;

        KeycloakSession session = context.getSession();
        RealmModel realm = session.getContext().getRealm();
        UserModel user = UserProfileAttributeValidationContext.from(context).getAttributeContext().getUser();

        // Only check if duplicate email addresses are not allowed, and the user is either new or changed their email address
        if (!realm.isDuplicateEmailsAllowed() && (user == null || !Objects.equals(user.getFirstAttribute(inputHint), value))) {
            UserModel userByEmail = session.users().getUserByEmail(realm, value);
            // check for duplicated email
            if (userByEmail != null && (user == null || !userByEmail.getId().equals(user.getId()))) {
                context.addError(new ValidationError(ID, inputHint, Messages.EMAIL_EXISTS)
                    .setStatusCode(Response.Status.CONFLICT));
            } else if (realm.isLoginWithEmailAllowed()) {
                // check for duplicated username
                userByEmail = session.users().getUserByUsername(realm, value);
                if (userByEmail != null && (user == null || !userByEmail.getId().equals(user.getId()))) {
                    context.addError(new ValidationError(ID, inputHint, Messages.EMAIL_EXISTS)
                            .setStatusCode(Response.Status.CONFLICT));
                }
            }
        }

        return context;
    }

}
