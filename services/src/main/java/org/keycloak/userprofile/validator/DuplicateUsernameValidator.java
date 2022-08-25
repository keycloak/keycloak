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

import javax.ws.rs.core.Response;
import java.util.List;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.validation.Validation;
import org.keycloak.userprofile.UserProfileAttributeValidationContext;
import org.keycloak.validate.SimpleValidator;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.ValidatorConfig;

/**
 * Validator to check that User Profile username already exists in database for another user in case of it's change, and
 * fail in this case. Expects List of Strings as input.
 * 
 * @author Vlastimil Elias <velias@redhat.com>
 *
 */
public class DuplicateUsernameValidator implements SimpleValidator {

    public static final String ID = "up-duplicate-username";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ValidationContext validate(Object input, String inputHint, ValidationContext context, ValidatorConfig config) {
        @SuppressWarnings("unchecked")
        List<String> values = (List<String>) input;

        if (values.isEmpty()) {
            return context;
        }

        String value = values.get(0);

        if (Validation.isBlank(value))
            return context;

        KeycloakSession session = context.getSession();
        UserModel existing = session.users().getUserByUsername(session.getContext().getRealm(), value);
        UserModel user = UserProfileAttributeValidationContext.from(context).getAttributeContext().getUser();

        if (user != null && !value.equals(user.getFirstAttribute(UserModel.USERNAME)) && (existing != null && !existing.getId().equals(user.getId()))) {
            context.addError(new ValidationError(ID, inputHint, Messages.USERNAME_EXISTS)
                .setStatusCode(Response.Status.CONFLICT));
        }

        return context;
    }

}
