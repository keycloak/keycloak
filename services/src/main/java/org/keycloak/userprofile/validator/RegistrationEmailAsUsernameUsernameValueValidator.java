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

import org.keycloak.models.RealmModel;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.validation.Validation;
import org.keycloak.validate.SimpleValidator;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.ValidatorConfig;

/**
 * Validator to check User Profile username attribute value during Registration when "RegistrationEmailAsUsername()" is
 * enabled. Expects List of Strings as input.
 * 
 * @author Vlastimil Elias <velias@redhat.com>
 *
 */
public class RegistrationEmailAsUsernameUsernameValueValidator implements SimpleValidator {

    public static final String ID = "up-registration-email-as-username-username-value";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ValidationContext validate(Object input, String inputHint, ValidationContext context, ValidatorConfig config) {

        RealmModel realm = context.getSession().getContext().getRealm();

        if (!realm.isRegistrationEmailAsUsername()) {
            return context;
        }

        @SuppressWarnings("unchecked")
        List<String> values = (List<String>) input;

        if (values == null || values.isEmpty()) {
            return context;
        }

        String value = values.get(0);

        if (value != null && Validation.isBlank(value)) {
            context.addError(new ValidationError(ID, inputHint, Messages.MISSING_USERNAME));
        }

        return context;
    }

}
